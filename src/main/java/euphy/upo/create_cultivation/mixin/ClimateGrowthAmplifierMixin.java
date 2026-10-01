package euphy.upo.create_cultivation.mixin;

import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.greenhouse.AmbientCropManager;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Climate growth amplifier for modded crops that are NOT vanilla-style
 * {@link CropBlock}s (Kaleidoscope Tavern grape trellises and their hanging
 * grape bunches, Farmer's Delight mushroom colonies, ...). These grow through
 * their own {@code randomTick} implementations, so the CropBlock multiplier
 * mixin cannot reach them; instead this handler cancels the vanilla dispatch
 * and re-invokes it once per growth roll, giving data-mapped randomTick crops
 * the same climate multiplier as greenhouse-vanilla ones.
 *
 * <p>Serene Seasons compatibility: the re-invocations re-run the full handler
 * chain, so Serene Seasons' seasonal veto (and the greenhouse-glass tag
 * exemption from our datapack) applies to every amplified attempt exactly as
 * to a normal tick.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ClimateGrowthAmplifierMixin {

    /**
     * Re-entrancy guard for the re-invoked dispatch: the amplified run must
     * reach the vanilla body (and other mods' handlers) without being
     * amplified again.
     */
    private static final ThreadLocal<Boolean> CREATE_CULTIVATION$AMPLIFYING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void create_cultivation$amplifyRandomTick(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (CREATE_CULTIVATION$AMPLIFYING.get()) {
            return; // amplified re-invocation: let the vanilla body run
        }
        BlockState state = (BlockState) (Object) this;
        if (state.getBlock() instanceof CropBlock) {
            return; // vanilla-style crops are handled by CropBlockGrowthMixin
        }
        if (state.getBlockHolder().getData(CCDataMaps.CROP_CLIMATE) == null) {
            return; // fast path: not a climate crop
        }
        GreenhouseCropTracker.Boost boost = GreenhouseCropTracker.get(level.dimension(), pos);
        if (boost == null) {
            boost = AmbientCropManager.resolveOutdoor(level, pos, state);
        }
        if (boost == null) {
            return;
        }
        if (boost.stalled()) {
            // outside both survival ranges: the crop's own growth randomTick
            // is suppressed entirely, matching vanilla-crop stall semantics
            ci.cancel();
            return;
        }
        float mult = Math.max(0.0f, boost.growthMultiplier());
        int runs = (int) mult;
        if (random.nextFloat() < mult - runs) {
            runs++;
        }
        if (runs <= 1) {
            return; // no amplification needed this tick
        }
        ci.cancel();
        CREATE_CULTIVATION$AMPLIFYING.set(Boolean.TRUE);
        try {
            for (int i = 0; i < runs; i++) {
                state.randomTick(level, pos, random);
                if (level.getBlockState(pos).getBlock() != state.getBlock()) {
                    break; // the plant outgrew this position (e.g. vines moving down)
                }
            }
        } finally {
            CREATE_CULTIVATION$AMPLIFYING.set(Boolean.FALSE);
        }
    }
}
