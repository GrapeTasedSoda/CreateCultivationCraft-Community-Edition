package euphy.upo.create_cultivation.registry;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class CCBlockTags {

    /**
     * Blocks accepted as greenhouse walls / ceiling. The controller's flood
     * fill treats these as the enclosure boundary; the floor may be any full
     * block. Extendable via datapacks (other mods can append their glass).
     */
    public static final TagKey<Block> GREENHOUSE_BOUNDARY =
            TagKey.create(Registries.BLOCK, CreateCultivationCraft.asResource("greenhouse_boundary"));

    /**
     * Blocks the scanner reports as greenhouse devices (sprinklers,
     * dehumidifiers, climate controllers - once they exist). Populated by the
     * device blocks themselves via datagen/tag file later.
     */
    public static final TagKey<Block> GREENHOUSE_DEVICE =
            TagKey.create(Registries.BLOCK, CreateCultivationCraft.asResource("greenhouse_device"));
}
