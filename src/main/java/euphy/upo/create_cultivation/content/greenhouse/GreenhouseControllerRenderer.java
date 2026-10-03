package euphy.upo.create_cultivation.content.greenhouse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import euphy.upo.create_cultivation.foundation.render.CCRenderTypes;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer for the greenhouse controller.
 * <p>
 * The static blockstate model is authored with the front (lamps + gear case)
 * facing north; the blockstate rotates it per {@code HORIZONTAL_FACING}. The
 * block entity renderer draws only the dynamic parts in block-local space
 * (which does not rotate), so everything here must compensate for the facing:
 * <ul>
 *   <li>Rotating cogwheel: {@code AllPartialModels.SHAFTLESS_COGWHEEL} stood
 *       up from its native Y axis into the vertical plane, spun around the
 *       facing axis (Z for north/south facings, X for east/west, matching
 *       {@link GreenhouseControllerBlock#getRotationAxis}).</li>
 *   <li>Lamps: display-link style additive halos ({@code DISPLAY_LINK_GLOW})
 *       over both 3 x 3 x 3 bulb housings, with the halo positions rotated
 *       around the block centre by the facing's model rotation. Brightness
 *       follows the display link bulb exactly: the eased glow value scales
 *       the RGB channels (color = 200 * curve) at constant alpha 255.</li>
 * </ul>
 */
public class GreenhouseControllerRenderer extends KineticBlockEntityRenderer<GreenhouseControllerBlockEntity> {

	private static final int FULLBRIGHT = 0xF000F0;

	/** Display link halo curve replication ({@code LinkBulbRenderer}). */
	private static final float GLOW_RADIUS_THRESHOLD = 0.125f;

	/** Left bulb housing tints yellow (lemon yellow), right one green - matching the texture. */
	private static final int YELLOW_R = 0xFF, YELLOW_G = 0xE8, YELLOW_B = 0x28;
	private static final int GREEN_R = 0x54, GREEN_G = 0xFF, GREEN_B = 0x54;

	/**
	 * The vanilla glow shell is a 6 px cube; scaled to 3.5 px it fully shrouds
	 * the 3 x 3 x 3 px bulb housing while staying below 4 px so it never
	 * reaches the surrounding cabinet geometry (topside at z 2..4, core top at
	 * y 19, sides at x 0..2 / 14..16 - all verified clear of the shell).
	 */
	private static final float SHELL_SCALE = 3.5f / 6.0f;

	public GreenhouseControllerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(GreenhouseControllerBlockEntity be, float partialTicks, PoseStack ms,
			MultiBufferSource buffer, int light, int overlay) {
		renderCogwheel(be, ms, buffer, light, overlay);
		renderBulbs(be, partialTicks, ms, buffer);
	}

	private void renderCogwheel(GreenhouseControllerBlockEntity be, PoseStack ms, MultiBufferSource buffer,
			int light, int overlay) {
		Direction.Axis axis = be.getBlockState()
			.getValue(GreenhouseControllerBlock.HORIZONTAL_FACING)
			.getAxis();

		SuperByteBuffer cog = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, be.getBlockState());

		// Verbatim transform sequence from MechanicalCrafterRenderer#renderFast -
		// Create's own renderer for exactly our geometry: a Y-native cogwheel
		// on a HorizontalKineticBlock whose rotation axis is the facing axis.
		// (standardKineticRotationTransform spins the wheel around the facing
		// axis; the UP quarter turn only matters for east/west facings where
		// the spin axis must land on X; the EAST quarter turn stands the disc
		// up into the vertical plane.)
		KineticBlockEntityRenderer.standardKineticRotationTransform(cog, be, light);
		cog.rotateCentered(axis != Direction.Axis.X ? 0.0f : (float) (Math.PI / 2), Direction.UP);
		cog.rotateCentered((float) (Math.PI / 2), Direction.EAST);

		ms.pushPose();
		cog.overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.solid()));
		ms.popPose();
	}

	private void renderBulbs(GreenhouseControllerBlockEntity be, float partialTicks, PoseStack ms,
			MultiBufferSource buffer) {
		// independent lamp signalling: green blinks once per 5 s while the
		// controller is working, yellow burns steady while the connected
		// devices cannot reach the setpoints; both dark by default
		Direction facing = be.getBlockState().getValue(GreenhouseControllerBlock.HORIZONTAL_FACING);
		renderLamp(ms, buffer, rotateFacingOffset(be.getYellowBulbOffset(), facing),
				be.getYellowGlow(partialTicks), YELLOW_R, YELLOW_G, YELLOW_B);
		renderLamp(ms, buffer, rotateFacingOffset(be.getGreenBulbOffset(), facing),
				be.getGreenGlow(partialTicks), GREEN_R, GREEN_G, GREEN_B);
	}

	/** One lamp halo at {@code glow} strength (0 = dark, 1 = fully lit). */
	private void renderLamp(PoseStack ms, MultiBufferSource buffer, Vec3 offset, float glow,
			int r, int g, int b) {
		if (glow < GLOW_RADIUS_THRESHOLD) {
			return;
		}

		// Display link halo curve: 1 - (glow - 0.75)^2 * 2, clamped to [-1, 1].
		// Like LinkBulbRenderer, the eased value scales the RGB channels at
		// constant alpha 255 - brightness modulation happens in color, not alpha.
		float haloCurve = Mth.clamp(1.0f - (float) Math.pow(glow - 0.75f, 2) * 2.0f, -1.0f, 1.0f);
		int color = (int) (GreenhouseControllerBlockEntity.MAX_ALPHA * haloCurve);
		if (color <= 0) {
			return;
		}
		renderHalo(ms, buffer, offset, color, r, g, b);
	}

	/**
	 * The blockstate model is authored facing north and rotated by the
	 * {@code y} blockstate rotation ({@code north->0, east->90, south->180,
	 * west->270} degrees, clockwise from above - vanilla furnace mapping). BER
	 * local space does not rotate, so lamp offsets authored for north must be
	 * rotated around the block centre by the same amount.
	 */
	private static Vec3 rotateFacingOffset(Vec3 local, Direction facing) {
		double dx = local.x - 0.5;
		double dz = local.z - 0.5;
		int quarterTurns = (facing.get2DDataValue() + 2) % 4; // N:0, E:1, S:2, W:3
		for (int i = 0; i < quarterTurns; i++) {
			double temp = dx;
			dx = -dz;
			dz = temp;
		}
		return new Vec3(0.5 + dx, local.y, 0.5 + dz);
	}

	private void renderHalo(PoseStack ms, MultiBufferSource buffer, Vec3 offset, int brightness, int r, int g, int b) {
		SuperByteBuffer halo = CachedBuffers.partial(AllPartialModels.DISPLAY_LINK_GLOW,
				euphy.upo.create_cultivation.registry.CCBlocks.GREENHOUSE_CONTROLLER.getDefaultState());

		// The vanilla glow shell spans -3..3 px around (0, 2.5, 0), so with
		// uniform scale s it covers (x, 2.5s-3s .. 2.5s+3s). Scale it to 3.5 px
		// and shift the shell centre onto the bulb housing centre by -2.5s px
		// on Y (the display link glow covers its bulb the same way).
		ms.pushPose();
		ms.translate(offset.x, offset.y, offset.z);
		ms.translate(0.0f, -SHELL_SCALE * 2.5f / 16.0f, 0.0f);
		ms.scale(SHELL_SCALE, SHELL_SCALE, SHELL_SCALE);
		halo.light(FULLBRIGHT)
				.color(r * brightness / 255, g * brightness / 255, b * brightness / 255, 255)
				.disableDiffuse()
				.renderInto(ms, buffer.getBuffer(CCRenderTypes.ADDITIVE_GLOW));
		ms.popPose();
	}
}
