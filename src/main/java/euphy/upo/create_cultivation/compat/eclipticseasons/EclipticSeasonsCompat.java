package euphy.upo.create_cultivation.compat.eclipticseasons;

import com.teamtea.eclipticseasons.api.EclipticSeasonsApi;
import com.teamtea.eclipticseasons.api.constant.solar.SolarTerm;
import com.teamtea.eclipticseasons.api.util.EclipticUtil;
import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.climate.ClimateUnits;
import euphy.upo.create_cultivation.config.CCConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * Ecliptic Seasons (24 solar terms) integration.
 *
 * <p>Both hooks defer (return null) outside season-enabled dimensions or
 * while no save data has arrived yet ({@link SolarTerm#NONE}), so the data
 * map and the vanilla mapping apply as usual.
 *
 * <p>Temperature: Ecliptic Seasons' own season-adjusted biome temperature
 * (native scale, from its per-biome climate settings and per-term changes)
 * mapped through our Celsius anchor curve.
 *
 * <p>Humidity: Ecliptic Seasons' own humidity measure - the term-adjusted
 * downfall blended with the term's rain chance, {@code (downfall*1.5 +
 * rainChance*0.5) / 2} - scaled to our %RH scale. Unlike Serene Seasons,
 * Ecliptic Seasons ships a humidity system itself, so no extra config is
 * needed here.
 */
public final class EclipticSeasonsCompat {

    private EclipticSeasonsCompat() {
    }

    public static void init() {
        CCDataMaps.setTemperatureResolver(EclipticSeasonsCompat::seasonTemperature);
        CCDataMaps.setHumidityResolver(EclipticSeasonsCompat::seasonHumidity);
    }

    @Nullable
    private static Float seasonTemperature(LevelReader level, Holder<Biome> biome) {
        if (!CCConfig.ES_CLIMATE_ENABLED.get())
            return null;
        if (!(level instanceof Level lvl) || !EclipticSeasonsApi.getInstance().isSeasonEnabled(lvl))
            return null;
        SolarTerm solarTerm = EclipticUtil.getNowSolarTerm(lvl);
        if (solarTerm == SolarTerm.NONE)
            return null;
        boolean isServer = !lvl.isClientSide();
        float nativeTemp = EclipticUtil.getTemperatureFloatConstant(solarTerm, biome.value(), isServer);
        return ClimateUnits.nativeTempToCelsius(nativeTemp);
    }

    @Nullable
    private static Float seasonHumidity(LevelReader level, Holder<Biome> biome) {
        if (!CCConfig.ES_CLIMATE_ENABLED.get())
            return null;
        if (!(level instanceof Level lvl) || !EclipticSeasonsApi.getInstance().isSeasonEnabled(lvl))
            return null;
        SolarTerm solarTerm = EclipticUtil.getNowSolarTerm(lvl);
        if (solarTerm == SolarTerm.NONE)
            return null;
        boolean isServer = !lvl.isClientSide();
        float humidity = EclipticUtil.getHumidityConstantFloat(solarTerm, biome, isServer);
        return ClimateUnits.clampHumidity(humidity * 100f);
    }
}
