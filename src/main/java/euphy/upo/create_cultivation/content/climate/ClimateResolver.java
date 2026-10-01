package euphy.upo.create_cultivation.content.climate;

import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/**
 * Resolves the current climate (°C / %RH) for a biome.
 *
 * <p>Resolution order for temperature and humidity (highest priority first):
 * <ol>
 * <li>the {@code create_cultivation:biome_climate} data map entry, if the
 * biome (or a biome tag the biome is in) carries the value - an explicit
 * per-biome pin always wins over the dynamic season;</li>
 * <li>the runtime resolver hooks from {@link CCDataMaps} (climate mods such
 * as Serene Seasons; may return null to defer) - the hooks receive the
 * {@link LevelReader} because seasons advance per dimension;</li>
 * <li>the vanilla mapping: the biome temperature through the Celsius anchor
 * curve, and the downfall value onto the percent scale (0 %RH for biomes
 * without precipitation).</li>
 * </ol>
 */
public final class ClimateResolver {

    private ClimateResolver() {}

    /** Reads the climate of the biome at the given position. */
    public static Climate resolve(LevelReader level, Holder<Biome> biome) {
        return new Climate(resolveTemperature(level, biome), resolveHumidity(level, biome));
    }

    public static float resolveTemperature(LevelReader level, Holder<Biome> biome) {
        BiomeClimate override = biome.getData(CCDataMaps.BIOME_CLIMATE);
        if (override != null && override.temperature().isPresent())
            return ClimateUnits.clampTempC(override.temperature().get());

        var resolver = CCDataMaps.getTemperatureResolver();
        if (resolver != null) {
            Float modded = resolver.apply(level, biome);
            if (modded != null)
                return ClimateUnits.clampTempC(modded);
        }

        return ClimateUnits.nativeTempToCelsius(biome.value().getBaseTemperature());
    }

    public static float resolveHumidity(LevelReader level, Holder<Biome> biome) {
        BiomeClimate override = biome.getData(CCDataMaps.BIOME_CLIMATE);
        if (override != null && override.humidity().isPresent())
            return ClimateUnits.clampHumidity(override.humidity().get());

        var resolver = CCDataMaps.getHumidityResolver();
        if (resolver != null) {
            Float modded = resolver.apply(level, biome);
            if (modded != null)
                return ClimateUnits.clampHumidity(modded);
        }

        var climate = biome.value().getModifiedClimateSettings();
        return ClimateUnits.downfallToHumidity(climate.downfall(), climate.hasPrecipitation());
    }

    /**
     * Immutable climate reading.
     *
     * @param tempC    temperature in °C
     * @param humidity relative humidity in %RH
     */
    public record Climate(float tempC, float humidity) {
    }
}
