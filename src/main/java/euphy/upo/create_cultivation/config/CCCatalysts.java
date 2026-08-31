package euphy.upo.create_cultivation.config;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config-driven catalyst registry. Each {@code catalysts} entry in the common
 * config defines one accepted catalyst item and its effects:
 *
 * <pre>modid:item_id;durationTicks;growthMultiplier;yieldMultiplier</pre>
 *
 * <ul>
 *   <li>{@code durationTicks} — boosted ticks one item lasts (20 ticks = 1s).</li>
 *   <li>{@code growthMultiplier} — growth speed multiplier while active
 *       (values below 1.0 slow crops down, which is allowed).</li>
 *   <li>{@code yieldMultiplier} — harvest yield multiplier while active
 *       (values below 1.0 reduce yield, which is allowed).</li>
 * </ul>
 *
 * Recipes can still override the accepted item via their {@code catalyst}
 * ingredient; an item accepted only that way (not present in the table) uses
 * the built-in fallback values below.
 */
public final class CCCatalysts {

    private static final Logger LOGGER = LoggerFactory.getLogger(CCCatalysts.class);

    /** Per-item catalyst definition parsed from the config list. */
    public record CatalystType(int durationTicks, double growthMultiplier, double yieldMultiplier) {}

    /** Used for items accepted only through a recipe's catalyst ingredient. */
    public static final CatalystType FALLBACK = new CatalystType(600, 3.0, 1.0);

    private static final double MIN_MULTIPLIER = 0.05;
    private static final double MAX_MULTIPLIER = 20.0;

    private static volatile List<? extends String> lastRaw = null;
    private static volatile Map<Item, CatalystType> cache = Map.of();

    private CCCatalysts() {
    }

    /** Re-parses the config table when it changed; cheap identity check. */
    private static Map<Item, CatalystType> current() {
        List<? extends String> raw = CCConfig.CATALYSTS.get();
        if (cache.isEmpty() || lastRaw != raw) {
            cache = parse(raw);
            lastRaw = raw;
        }
        return cache;
    }

    private static Map<Item, CatalystType> parse(List<? extends String> raw) {
        Map<Item, CatalystType> map = new HashMap<>();
        for (String entry : raw) {
            String[] parts = entry.split(";");
            if (parts.length != 4) {
                LOGGER.warn("Skipping malformed catalyst entry (expected item;ticks;growth;yield): {}", entry);
                continue;
            }
            try {
                ResourceLocation id = ResourceLocation.parse(parts[0].trim().toLowerCase(Locale.ROOT));
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null || item == Items.AIR) {
                    LOGGER.info("Catalyst entry references unknown item (mod not installed?): {}", parts[0]);
                    continue;
                }
                int duration = Math.max(1, Integer.parseInt(parts[1].trim()));
                double growth = clampMultiplier(parts[2]);
                double yield = Double.parseDouble(parts[3].trim());
                yield = Math.max(0.0, Math.min(MAX_MULTIPLIER, yield));
                map.put(item, new CatalystType(duration, growth, yield));
            } catch (NumberFormatException e) {
                LOGGER.warn("Skipping catalyst entry with invalid numbers: {}", entry);
            }
        }
        return Map.copyOf(map);
    }

    private static double clampMultiplier(String value) {
        double v = Double.parseDouble(value.trim());
        return Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, v));
    }

    /** Whether the item is a registered catalyst. */
    public static boolean isCatalyst(Item item) {
        return current().containsKey(item);
    }

    /** The definition for an item, or {@code null} when it is not registered. */
    public static CatalystType getType(Item item) {
        return current().get(item);
    }

    /** All registered catalyst items (used for pre-filling tooltips/tests). */
    public static boolean hasAny() {
        return !current().isEmpty();
    }
}
