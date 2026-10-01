package euphy.upo.create_cultivation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Even with placement blocked, a cogwheel could in theory be cheated into the
 * cell above the greenhouse controller (or survive from before the machine
 * was placed). The engine's small-cog meshing check is purely geometric, so
 * veto it here: no rotation is ever conveyed between a greenhouse controller
 * and a cogwheel sitting directly above it, in either evaluation direction.
 */
@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    @Inject(method = "getRotationSpeedModifier", at = @At("HEAD"), cancellable = true)
    private static void create_cultivation$vetoTopFaceConduction(KineticBlockEntity from, KineticBlockEntity to,
            CallbackInfoReturnable<Float> cir) {
        if (from == null || to == null) {
            return;
        }

        BlockState fromState = from.getBlockState();
        BlockState toState = to.getBlockState();
        boolean fromIsCog = ICogWheel.isSmallCog(fromState) || ICogWheel.isLargeCog(fromState);
        boolean toIsCog = ICogWheel.isSmallCog(toState) || ICogWheel.isLargeCog(toState);

        // cog above controller, either evaluation order
        if (fromIsCog && toState.getBlock() instanceof GreenhouseControllerBlock
                && from.getBlockPos().equals(to.getBlockPos().above())) {
            cir.setReturnValue(0.0f);
        } else if (toIsCog && fromState.getBlock() instanceof GreenhouseControllerBlock
                && to.getBlockPos().equals(from.getBlockPos().above())) {
            cir.setReturnValue(0.0f);
        }
    }
}
