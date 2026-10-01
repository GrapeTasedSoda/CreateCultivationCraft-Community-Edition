package euphy.upo.create_cultivation.content.dehumidifier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renders the condensed water inside the dehumidifier's glass bottle as a
 * real fluid box whose height follows the (smoothed) fill level - the same
 * approach as Create's {@code FluidTankRenderer}.
 *
 * <p>Bottle cavity in authored (north) block units:
 * x 0.078125..0.921875, y 0.140625..0.375, z 0.390625..0.75 (13.5 x 3.75 x
 * 5.75 px). The box is inset half a pixel so it never z-fights with the
 * bottle glass, then remapped to the block's facing with the same rotation
 * the blockstate uses (verified against the humidifier collision table).
 */
public class DehumidifierRenderer extends SafeBlockEntityRenderer<DehumidifierBlockEntity> {

    /** half-pixel inset from the cavity walls (px / 16). */
    private static final double INSET = 0.5 / 16d;

    // authored-north cavity bounds, inset
    private static final double AX0 = 1.25 / 16d + INSET;
    private static final double AX1 = 14.75 / 16d - INSET;
    private static final double AY0 = 2.25 / 16d + INSET;
    private static final double AY1 = 6.0 / 16d - INSET;
    private static final double AZ0 = 6.25 / 16d + INSET;
    private static final double AZ1 = 12.0 / 16d - INSET;

    public DehumidifierRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Maps an authored-north point to world space for the given facing. */
    private static Vec3 mapFacing(double x, double y, double z, Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(x, y, z);
            case SOUTH -> new Vec3(1 - x, y, 1 - z);
            case EAST -> new Vec3(1 - z, y, x);
            case WEST -> new Vec3(z, y, 1 - x);
            default -> new Vec3(x, y, z); // horizontal-only machine
        };
    }

    @Override
    protected void renderSafe(DehumidifierBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay) {
        FluidStack fluid = be.getFluid();
        if (fluid.isEmpty())
            return;

        float level = be.getFluidLevel().getValue(partialTicks);
        if (level < 1 / 512f)
            return;

        Direction facing = be.getBlockState().getValue(DehumidifierBlock.FACING);

        Vec3 lo = mapFacing(AX0, AY0, AZ0, facing);
        Vec3 hi = mapFacing(AX1, AY1, AZ1, facing);
        double xMin = Math.min(lo.x, hi.x);
        double xMax = Math.max(lo.x, hi.x);
        double zMin = Math.min(lo.z, hi.z);
        double zMax = Math.max(lo.z, hi.z);
        double yMin = Math.min(lo.y, hi.y);
        double yMax = Math.max(lo.y, hi.y);

        // water pools from the world-lowest point of the (possibly tilted)
        // bottle cavity upwards
        double clamped = Mth.clamp(level, 0, 1);
        double waterYMax = yMin + (yMax - yMin) * clamped;
        if (waterYMax - yMin < 1 / 1024d)
            return;

        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,
                (float) xMin, (float) yMin, (float) zMin,
                (float) xMax, (float) waterYMax, (float) zMax,
                buffer, ms, light, true, true);
    }
}
