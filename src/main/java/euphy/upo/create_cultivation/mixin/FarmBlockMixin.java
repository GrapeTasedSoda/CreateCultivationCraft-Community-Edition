package euphy.upo.create_cultivation.mixin;

import euphy.upo.create_cultivation.content.sprinkler.SprinklerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla farmland drying random tick (moisture--) fights the sprinkler
 * over the moisture property: a farmland under an active sprinkler is never
 * 'near water' for vanilla (the machine is not a water block), so vanilla
 * dried it one step per random tick and the sprinkler hydrated it back on the
 * next cycle - the reported visible moisture flip-flop. A farmland inside an
 * active sprinkler's coverage is skipped entirely; the sprinkler owns its
 * moisture.
 */
@Mixin(FarmBlock.class)
public class FarmBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void create_cultivation$sprinklerOwnedMoisture(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (SprinklerBlockEntity.isFarmlandCoveredBySprinkler(level, pos)) {
            ci.cancel();
        }
    }
}
