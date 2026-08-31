package euphy.upo.create_cultivation.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import euphy.upo.create_cultivation.CreateCultivationCraft;

public class CCPartialModels {

    public static final PartialModel CULTIVATION_BASE_GLOW = PartialModel.of(
            CreateCultivationCraft.asResource("block/cultivation_base_glow"));

    public static final PartialModel CULTIVATION_BASE_GLOW_FULL = PartialModel.of(
            CreateCultivationCraft.asResource("block/cultivation_base_glow_full"));

    /** Orange "height mismatch" glow (crop needs a taller tank stack). */
    public static final PartialModel CULTIVATION_BASE_GLOW_ERROR = PartialModel.of(
            CreateCultivationCraft.asResource("block/cultivation_base_glow_error"));

    public static void init() {

    }
}