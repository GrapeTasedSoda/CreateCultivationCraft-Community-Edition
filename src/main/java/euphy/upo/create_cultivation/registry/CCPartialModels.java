package euphy.upo.create_cultivation.registry;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import euphy.upo.create_cultivation.CreateCultivationCraft;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;

public class CCPartialModels {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateCultivationCraft.MODID + "/partial_models");
	/** One-time warn per unbaked partial location; keeps the log clean on packs that never bake them. */
	private static final Set<String> WARNED_UNBAKED = new HashSet<>();

    public static final PartialModel CULTIVATION_BASE_GLOW = PartialModel.of(
            CreateCultivationCraft.asResource("block/cultivation_base_glow"));

    public static final PartialModel CULTIVATION_BASE_GLOW_FULL = PartialModel.of(
            CreateCultivationCraft.asResource("block/cultivation_base_glow_full"));

    /** Orange "height mismatch" glow (crop needs a taller tank stack). */
    public static final PartialModel CULTIVATION_BASE_GLOW_ERROR = PartialModel.of(
            CreateCultivationCraft.asResource("block/cultivation_base_glow_error"));

    public static void init() {

    }

    /**
     * Crash guard around {@link CachedBuffers#partial}: resolves the partial to
     * its cached buffer only when Flywheel actually baked it, otherwise
     * returns {@code null} after warning once.
     *
     * <p>Why: a partial model's {@link PartialModel#get()} is populated from
     * the model bakery (ModelEvent.RegisterAdditional + BakingCompleted) and
     * stays {@code null} when the model was never baked. Catnip's buffering
     * path then invokes methods on that null BakedModel and hard-crashes the
     * client mid-render ("Cannot invoke BakedModel.getModelData ... model is
     * null"). Heavy packs tampering with the bake chain (model culling, CTM,
     * Connector-converted Create addons, ...) can strand standalone partials
     * exactly like that, while Create's own partials stay fine. A decorative
     * layer is not worth a crash - callers skip the layer when this returns
     * {@code null}.
     */
    @Nullable
    public static SuperByteBuffer safePartial(PartialModel partial, BlockState referenceState, String site) {
        if (partial.get() == null) {
            if (WARNED_UNBAKED.add(partial.modelLocation().toString())) {
                LOGGER.warn("Partial model {} was never baked (model baking likely interfered with by another mod in this pack) - skipping {} rendering instead of crashing.",
                        partial.modelLocation(), site);
            }
            return null;
        }
        return CachedBuffers.partial(partial, referenceState);
    }
}