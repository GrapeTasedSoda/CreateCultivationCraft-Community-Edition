package euphy.upo.create_cultivation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;

import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;

/**
 * The greenhouse controller's 22 px tall model reaches into the cell above it.
 * Cogwheels may therefore neither be placed there nor offered as a placement
 * hint: veto the dedicated cogwheel's own validity check whenever the target
 * cell sits directly above a greenhouse controller.
 * <p>
 * Create calls {@code isValidCogwheelPosition} from {@link CogWheelBlock}
 * {@code canSurvive} (manual placement) and from every cogwheel placement
 * helper (ghost hints + helper-assisted placement), so cancelling it once
 * kills all top-face placement paths.
 */
@Mixin(value = CogWheelBlock.class, remap = false)
public abstract class CogWheelBlockMixin {

    @Inject(method = "isValidCogwheelPosition", at = @At("HEAD"), cancellable = true)
    private static void create_cultivation$vetoCogsAboveController(boolean large, LevelReader world, BlockPos pos,
            Axis cogAxis, CallbackInfoReturnable<Boolean> cir) {
        BlockPos below = pos.below();
        if (world.getBlockState(below).getBlock() instanceof GreenhouseControllerBlock) {
            // any cogwheel axis directly on top is rejected, regardless of size
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
