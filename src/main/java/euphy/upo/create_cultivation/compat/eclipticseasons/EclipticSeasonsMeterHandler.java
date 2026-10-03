package euphy.upo.create_cultivation.compat.eclipticseasons;

import java.util.Locale;

import euphy.upo.create_cultivation.content.climate.ClimateResolver;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Ecliptic Seasons' thermometer and hygrometer re-read through this mod's
 * ambient climate resolver: their right-click readout shows the same values
 * the greenhouse climate system uses (°C / %RH after the per-solar-term
 * adjustments, biome pins and the vanilla fallback).
 *
 * <p>The two items are matched by registry id, so nothing here touches
 * Ecliptic Seasons' classes and the handler is inert while the mod (or just
 * these two items) is absent. Cancelling the event suppresses Ecliptic
 * Seasons' own level-name readout for exactly these two items; the rain
 * gauge keeps its vanilla behavior.
 */
public final class EclipticSeasonsMeterHandler {

    private static final ResourceLocation THERMOMETER =
            ResourceLocation.fromNamespaceAndPath("eclipticseasons", "thermometer");
    private static final ResourceLocation HYGROMETER =
            ResourceLocation.fromNamespaceAndPath("eclipticseasons", "hygrometer");

    private EclipticSeasonsMeterHandler() {}

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        int mode = meterMode(event.getItemStack());
        if (mode == 0)
            return;
        Level level = event.getLevel();
        Player player = event.getEntity();
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        if (level.isClientSide()) {
            if (mode == 1) {
                float tempC = ClimateResolver.resolveTemperature(level, biome);
                player.displayClientMessage(Component.translatable(
                        "create_cultivation.meter.thermometer", String.format(Locale.ROOT, "%.1f", tempC)), true);
            } else {
                float humidity = ClimateResolver.resolveHumidity(level, biome);
                player.displayClientMessage(Component.translatable(
                        "create_cultivation.meter.hygrometer", String.format(Locale.ROOT, "%.0f", humidity)), true);
            }
        }
        event.setCanceled(true);
    }

    private static int meterMode(ItemStack stack) {
        Item item = stack.getItem();
        if (item == BuiltInRegistries.ITEM.getOptional(THERMOMETER).orElse(null))
            return 1;
        if (item == BuiltInRegistries.ITEM.getOptional(HYGROMETER).orElse(null))
            return 2;
        return 0;
    }
}
