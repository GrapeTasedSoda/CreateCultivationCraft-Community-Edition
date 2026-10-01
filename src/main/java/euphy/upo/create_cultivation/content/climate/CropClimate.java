package euphy.upo.create_cultivation.content.climate;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Climate requirements of a crop, attached through the
 * {@code create_cultivation:crop_climate} block data map. All four ranges are
 * optional in JSON; every crop evaluates against a non-null interval as
 * follows:
 *
 * <ul>
 * <li>survival ranges default to the full temperature/humidity scales (any
 * unconfigured crop keeps growing everywhere, it simply gets no bonus);</li>
 * <li>omitted optimal ranges simply mean "no bonus available".</li>
 * </ul>
 *
 * @param tempOptimal      optimal temperature range in °C (growth + yield bonus)
 * @param tempSurvival     survival temperature range in °C
 * @param humidityOptimal  optimal humidity range in %RH (growth + yield bonus)
 * @param humiditySurvival survival humidity range in %RH
 */
public record CropClimate(
        Optional<ClimateInterval> tempOptimal,
        Optional<ClimateInterval> tempSurvival,
        Optional<ClimateInterval> humidityOptimal,
        Optional<ClimateInterval> humiditySurvival) {

    public static final CropClimate DEFAULT = new CropClimate(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<CropClimate> CODEC = RecordCodecBuilder.create(i -> i.group(
            ClimateInterval.CODEC.optionalFieldOf("temp_optimal").forGetter(CropClimate::tempOptimal),
            ClimateInterval.CODEC.optionalFieldOf("temp_survival").forGetter(CropClimate::tempSurvival),
            ClimateInterval.CODEC.optionalFieldOf("humidity_optimal").forGetter(CropClimate::humidityOptimal),
            ClimateInterval.CODEC.optionalFieldOf("humidity_survival").forGetter(CropClimate::humiditySurvival)
    ).apply(i, CropClimate::new));

    public CropState evaluate(float tempC, float humidityPercent) {
        ClimateInterval tempSurvival = tempSurvival().orElse(fullTemp());
        if (!tempSurvival.contains(tempC))
            return CropState.FAIL;
        ClimateInterval humiditySurvival = humiditySurvival().orElse(fullHumidity());
        if (!humiditySurvival.contains(humidityPercent))
            return CropState.FAIL;

        boolean tempOptimal = tempOptimal().map(r -> r.contains(tempC)).orElse(false);
        boolean humidityOptimal = humidityOptimal().map(r -> r.contains(humidityPercent)).orElse(false);
        if (tempOptimal && humidityOptimal)
            return CropState.OPTIMAL;
        if (tempOptimal || humidityOptimal)
            return CropState.PARTIAL;
        return CropState.SURVIVAL_ONLY;
    }

    private static ClimateInterval fullTemp() {
        return new ClimateInterval(ClimateUnits.TEMP_C_MIN, ClimateUnits.TEMP_C_MAX);
    }

    private static ClimateInterval fullHumidity() {
        return new ClimateInterval(ClimateUnits.HUMIDITY_MIN, ClimateUnits.HUMIDITY_MAX);
    }
}
