package euphy.upo.create_cultivation.registry;

import com.tterrag.registrate.util.entry.ItemEntry;
import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.fertilizer.EfficientFertilizerItem;
import net.minecraft.world.item.Item;

/**
 * Mod items. The Cultivation Craft mod ships few standalone items - most
 * content lives on blocks - so this registry currently only holds the
 * efficient fertilizer catalyst.
 */
public class CCItems {

    /**
     * Efficient fertilizer: a stronger catalyst accepted by the cultivation
     * base's fertilizer slot. Effects are configured in the default catalyst
     * table (45 s duration, growth x3, yield x2).
     */
    public static final ItemEntry<EfficientFertilizerItem> EFFICIENT_FERTILIZER = CreateCultivationCraft.REGISTRATE
            .item("efficient_fertilizer", EfficientFertilizerItem::new)
            .properties(p -> p.stacksTo(64))
            .register();

    public static void register() {
    }
}
