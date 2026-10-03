package euphy.upo.create_cultivation.compat.sereneseasons;

import org.jetbrains.annotations.Nullable;

import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.climate.ClimateUnits;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.SeasonsConfig.SeasonProperties;
import sereneseasons.init.ModConfig;
import sereneseasons.init.ModTags;

/**
 * Serene Seasons integration. Installed by the mod constructor behind a
 * {@code ModList.isLoaded("sereneseasons")} check; both climate resolver
 * hooks then return the season-adjusted biome climate:
 *
 * <ul>
 * <li><b>Temperature</b> - mirrors {@code SeasonHooks.getBiomeTemperatureInSeason}:
 * the native biome temperature shifts by the sub-season's
 * {@code biome_temp_adjustment} (SS's own config, e.g. -0.8 in winter), for
 * non-tropical biomes with a base temperature &le; 0.8, clamped to
 * [-0.5, 2.0] native, then converted through our Celsius anchor curve.</li>
 * <li><b>Humidity</b> - Serene Seasons has no humidity mechanism, so the
 * season's offset from our config is applied on top of the vanilla
 * downfall-derived base. Tropical biomes instead run SS's dry/wet seasons:
 * the mid-dry season pins the humidity to 0 %RH (mirroring
 * {@code hasPrecipitationSeasonal}); everything else defers.</li>
 * </ul>
 *
 * <p>Both hooks defer (return null) on non-whitelisted dimensions and
 * blacklisted biomes, so out-of-world behaviour is untouched. Season state
 * comes from {@link SeasonHelper#getSeasonState(Level)}, which works on both
 * the server and the synced client, keeping the Jade readouts consistent.
 */
public final class SereneSeasonsCompat {

    private SereneSeasonsCompat() {}

    public static void init() {
        CCDataMaps.setTemperatureResolver(SereneSeasonsCompat::seasonTemperature);
        CCDataMaps.setHumidityResolver(SereneSeasonsCompat::seasonHumidity);
    }

    /** Season-adjusted temperature in °C, or null to defer (see class docs). */
    @Nullable
    private static Float seasonTemperature(LevelReader level, Holder<Biome> biome) {
        if (!CCConfig.SS_CLIMATE_ENABLED.get())
            return null;
        if (!(level instanceof Level lvl) || !ModConfig.seasons.isDimensionWhitelisted(lvl.dimension())
                || biome.is(ModTags.Biomes.BLACKLISTED_BIOMES))
            return null;
        if (biome.is(ModTags.Biomes.TROPICAL_BIOMES) || biome.value().getBaseTemperature() > 0.8f)
            return null;

        ISeasonState state = SeasonHelper.getSeasonState(lvl);
        SeasonProperties props = ModConfig.seasons.getSeasonProperties(state.getSubSeason());
        float adjustedNative = Mth.clamp(biome.value().getBaseTemperature() + props.biomeTempAdjustment(), -0.5f, 2.0f);
        return ClimateUnits.nativeTempToCelsius(adjustedNative);
    }

    /** Season-adjusted humidity in %RH, or null to defer (see class docs). */
    @Nullable
    private static Float seasonHumidity(LevelReader level, Holder<Biome> biome) {
        if (!CCConfig.SS_CLIMATE_ENABLED.get())
            return null;
        if (!(level instanceof Level lvl) || !ModConfig.seasons.isDimensionWhitelisted(lvl.dimension())
                || biome.is(ModTags.Biomes.BLACKLISTED_BIOMES))
            return null;

        ISeasonState state = SeasonHelper.getSeasonState(lvl);
        if (biome.is(ModTags.Biomes.TROPICAL_BIOMES))
            return state.getTropicalSeason() == Season.TropicalSeason.MID_DRY ? 0.0f : null;

        var climate = biome.value().getModifiedClimateSettings();
        float base = ClimateUnits.downfallToHumidity(climate.downfall(), climate.hasPrecipitation());
        float offset = switch (state.getSeason()) {
            case SPRING -> CCConfig.SS_SPRING_HUMIDITY_OFFSET.get().floatValue();
            case SUMMER -> CCConfig.SS_SUMMER_HUMIDITY_OFFSET.get().floatValue();
            case AUTUMN -> CCConfig.SS_AUTUMN_HUMIDITY_OFFSET.get().floatValue();
            case WINTER -> CCConfig.SS_WINTER_HUMIDITY_OFFSET.get().floatValue();
        };
        return ClimateUnits.clampHumidity(base + offset);
    }
}
