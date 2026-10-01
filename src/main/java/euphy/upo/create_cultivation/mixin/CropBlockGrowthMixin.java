package euphy.upo.create_cultivation.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropTracker;

/**
 * Greenhouse climate control for soil-planted vanilla-style crops.
 *
 * <p>Replaces {@link CropBlock#randomTick}: outside any powered greenhouse
 * the original growth path runs unchanged; inside one the vanilla single
 * growth roll is executed {@code growthMultiplier} times (rounding with a
 * probability floor, so 1.5x means a 50% second roll), and a stalled crop
 * (outside both survival ranges) never grows at all.
 *
 * <p>Cultivation tank crops are unaffected - they do not grow through
 * {@code CropBlock.randomTick}.
 */
@Mixin(CropBlock.class)
public abstract class CropBlockGrowthMixin {

    @Shadow
    protected static float getGrowthSpeed(BlockState state, BlockGetter level, BlockPos pos) {
        // static target: shadows for static methods need a dummy body
        throw new AssertionError();
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void create_cultivation$climateGrowth(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random, CallbackInfo ci) {        euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropTracker.Boost boost =
                GreenhouseCropTracker.get(level.dimension(), pos);
        if (boost == null) {
            // not in a greenhouse: optionally apply the ambient (outdoor)
            // climate, otherwise run the vanilla path
            boost = euphy.upo.create_cultivation.content.greenhouse.AmbientCropManager
                    .resolveOutdoor(level, pos, state);
            if (boost == null) {
                return;
            }
        }
        ci.cancel();

        CropBlock self = (CropBlock) (Object) this;
        if (boost.stalled()) {
            return; // outside the survival ranges: no growth at all
        }
        if (!level.isAreaLoaded(pos, 1) || level.getRawBrightness(pos, 0) < 9) {
            return; // same gates as vanilla
        }
        int age = self.getAge(state);
        if (age >= self.getMaxAge()) {
            return;
        }
        float speed = getGrowthSpeed(state, level, pos);
        float mult = Math.max(0.0f, boost.growthMultiplier());
        // whole part: that many vanilla rolls; fractional part: one extra roll
        // with that success chance (1.5x -> 1 roll + 50% second roll)
        int rolls = (int) mult;
        if (random.nextFloat() < mult - rolls) {
            rolls++;
        }
        for (int i = 0; i < rolls; i++) {
            if (net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state,
                    random.nextInt((int) (25.0F / speed) + 1) == 0)) {
                level.setBlock(pos, self.getStateForAge(self.getAge(level.getBlockState(pos)) + 1), 2);
                net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
                if (self.getAge(level.getBlockState(pos)) >= self.getMaxAge()) {
                    break;
                }
            }
        }
    }

    /**
     * Stall fertilizer blocking for the vanilla bone-meal path lives in
     * {@code BoneMealItemMixin} (item-aware, covers player use and
     * dispensers); unlisted items intentionally still work, matching the
     * configurable fertilizer list semantics.
     */
}
