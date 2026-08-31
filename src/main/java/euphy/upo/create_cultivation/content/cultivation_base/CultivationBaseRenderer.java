package euphy.upo.create_cultivation.content.cultivation_base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import euphy.upo.create_cultivation.registry.CCPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class CultivationBaseRenderer extends KineticBlockEntityRenderer<CultivationBaseBlockEntity> {

    /** Duration of one full red-glow pulse cycle in seconds. */
    private static final double BREATH_PERIOD_SECONDS = 2.0;
    public CultivationBaseRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CultivationBaseBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        renderRotatingCogBER(be, partialTicks, ms, buffer, light, overlay);
    }


    protected void renderRotatingCogBER(CultivationBaseBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        //super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        BlockState blockState = be.getBlockState();
        SuperByteBuffer cogWheelSbb = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);

        Direction.Axis axis = Direction.Axis.Y;
        float angleRadians = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);

        KineticBlockEntityRenderer.kineticRotationTransform(cogWheelSbb, be, axis, angleRadians, light);

        ms.pushPose();

        ms.translate(0, 0, 0);

        cogWheelSbb.overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        ms.popPose();

        if (blockState.getValue(CultivationBaseBlock.WORKING)) {

            boolean outputFull = be.isOutputFull();
            SuperByteBuffer glowLayer = CachedBuffers.partial(
                    outputFull ? CCPartialModels.CULTIVATION_BASE_GLOW_FULL : CCPartialModels.CULTIVATION_BASE_GLOW,
                    blockState);


            int fullbright = 15728880;

            if (outputFull) {
                // "Breathing" alarm: the red glow pulses between 45% and full
                // brightness and floats slightly, driven by wall-clock time so
                // the motion stays smooth regardless of server tick rate.
                double seconds = System.nanoTime() * 1.0e-9;
                float pulse = 0.5f + 0.5f * Mth.sin((float) (seconds * Math.PI * 2.0 / BREATH_PERIOD_SECONDS));
                float lightScale = 0.45f + 0.55f * pulse;
                int level = (int) (15 * lightScale);
                fullbright = (level << 4) | (level << 20);

                ms.pushPose();
                ms.translate(0, (1.0f - lightScale) * 0.5f / 16.0f, 0);
                glowLayer.light(fullbright).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
                ms.popPose();
            } else {
                glowLayer.light(fullbright).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
            }
        }

    }
}
