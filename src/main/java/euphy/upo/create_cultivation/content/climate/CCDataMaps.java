package euphy.upo.create_cultivation.content.climate;

import java.util.function.BiFunction;

import com.mojang.serialization.Codec;
import euphy.upo.create_cultivation.CreateCultivationCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Data map types powering the greenhouse climate system.
 *
 * <p>Data files live under {@code data/<ns>/data_maps/...}: biomes in
 * {@code worldgen/biome/biome_climate.json}, crops in
 * {@code block/crop_climate.json}. Other mods and data packs can freely append
 * entries (or {@code "replace": true} whole files), which is the compat story
 * for modded biomes and modded crops.
 */
public final class CCDataMaps {

    /** Per-biome temperature (°C) / humidity (%RH) overrides. */
    public static final DataMapType<Biome, BiomeClimate> BIOME_CLIMATE = DataMapType.builder(
            CreateCultivationCraft.asResource("biome_climate"),
            Registries.BIOME,
            BiomeClimate.CODEC
    ).build();

    /** Per-block crop climate ranges (°C / %RH intervals). */
    public static final DataMapType<Block, CropClimate> CROP_CLIMATE = DataMapType.builder(
            CreateCultivationCraft.asResource("crop_climate"),
            Registries.BLOCK,
            CropClimate.CODEC
    ).build();

    /**
     * Display item for crop blocks that have no item form of their own (rice,
     * grapes, trellises and other multi-structure crops whose planting item is
     * a separate seed/fruit): the GUI crop list renders this instead of an
     * empty slot. Blocks WITH an item form never consult this map.
     */
    public static final DataMapType<Block, CropDisplay> CROP_DISPLAY_ICON = DataMapType.builder(
            CreateCultivationCraft.asResource("display_icon"),
            Registries.BLOCK,
            CropDisplay.CODEC
    ).build();

    /** The item shown for an itemless crop block. */
    public record CropDisplay(Item item) {
        public static final Codec<CropDisplay> CODEC = BuiltInRegistries.ITEM.byNameCodec()
                .fieldOf("item").xmap(CropDisplay::new, CropDisplay::item).codec();
    }

    private CCDataMaps() {}

    /**
     * Extension points for climate mods (e.g. Serene Seasons). When set, the
     * resolver consults them after the {@code biome_climate} data map (an
     * explicit per-biome pin always wins over the dynamic season). The hook
     * receives the {@link LevelReader} because seasons advance per dimension;
     * returning null defers to the next resolution stage.
     */
    private static BiFunction<LevelReader, Holder<Biome>, Float> temperatureResolver;
    private static BiFunction<LevelReader, Holder<Biome>, Float> humidityResolver;

    public static void setTemperatureResolver(BiFunction<LevelReader, Holder<Biome>, Float> resolver) {
        temperatureResolver = resolver;
    }

    public static BiFunction<LevelReader, Holder<Biome>, Float> getTemperatureResolver() {
        return temperatureResolver;
    }

    public static void setHumidityResolver(BiFunction<LevelReader, Holder<Biome>, Float> resolver) {
        humidityResolver = resolver;
    }

    public static BiFunction<LevelReader, Holder<Biome>, Float> getHumidityResolver() {
        return humidityResolver;
    }

    public static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(BIOME_CLIMATE);
        event.register(CROP_CLIMATE);
        event.register(CROP_DISPLAY_ICON);
    }
}
