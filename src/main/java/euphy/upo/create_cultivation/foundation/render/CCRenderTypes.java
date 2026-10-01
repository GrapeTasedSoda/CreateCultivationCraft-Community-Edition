package euphy.upo.create_cultivation.foundation.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Additive glow layer that does NOT write depth.
 * <p>
 * Create's own {@code RenderTypes.additive()} keeps the default depth write.
 * That is safe for the display link because its block model is opaque (solid
 * chunk layer, drawn before block entity renderers). Our greenhouse controller
 * uses a translucent block model, and translucent chunk geometry is drawn
 * AFTER block entities: if the halo wrote depth, every translucent machine
 * face behind the halo shell would fail the depth test and vanish, letting
 * the player see straight through the machine (x-ray bug).
 * <p>
 * Same trick as Create's RenderTypes: extend RenderStateShard to reach the
 * protected state constants.
 */
public class CCRenderTypes extends RenderStateShard {

	public static final RenderType ADDITIVE_GLOW = RenderType.create(
			"create_cultivation" + ":" + "additive_glow", DefaultVertexFormat.BLOCK,
			VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
					.setShaderState(RENDERTYPE_SOLID_SHADER)
					.setTextureState(BLOCK_SHEET)
					.setTransparencyState(ADDITIVE_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(true));

	private CCRenderTypes() {
		super(null, null, null);
	}
}
