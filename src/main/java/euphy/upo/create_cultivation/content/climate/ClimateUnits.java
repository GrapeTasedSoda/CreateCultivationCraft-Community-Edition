package euphy.upo.create_cultivation.content.climate;

import net.minecraft.util.Mth;

/**
 * Unit conversions for the greenhouse climate system.
 *
 * <p>Temperature uses a dedicated Celsius scale [-25, 120] °C. The default
 * mapping from the vanilla biome temperature is a piecewise-linear anchor
 * curve (see {@link #TEMP_ANCHORS}) that reproduces plausible real-world
 * climates for the overworld; the nether/end are pinned by the built-in
 * {@code create_cultivation:biome_climate} data map instead (their native
 * temperature is indistinguishable from the desert's). All of it stays
 * overridable: climate mods via the runtime resolver hook, datapacks via the
 * data map. Humidity uses a dedicated percent scale: the vanilla downfall
 * value [0, 1] maps onto [0, 100] %RH (biomes without precipitation are
 * pinned to 0 %RH).
 */
public final class ClimateUnits {

    /** Lowest vanilla biome temperature (frozen peaks). */
    public static final float NATIVE_TEMP_MIN = -0.7f;
    /** Highest vanilla biome temperature (desert, badlands, nether). */
    public static final float NATIVE_TEMP_MAX = 2.0f;

    /** Greenhouse temperature scale, in degrees Celsius. */
    public static final float TEMP_C_MIN = -25.0f;
    public static final float TEMP_C_MAX = 120.0f;

    /** Greenhouse humidity scale, in %RH. */
    public static final float HUMIDITY_MIN = 0.0f;
    public static final float HUMIDITY_MAX = 100.0f;

    /**
     * Anchor curve for the default biome temperature mapping, native temp →
     * °C, sorted ascending. Chosen so the vanilla overworld reads like real
     * climates instead of a raw linear stretch: the nether and the end are
     * deliberately NOT covered here (their native temperature is identical to
     * the desert's) - they are pinned by the built-in
     * {@code create_cultivation:biome_climate} data map instead.
     */
    private static final float[][] TEMP_ANCHORS = {
            {-0.70f, -25f},  // frozen peaks, ice spikes
            { 0.00f,  -6f},  // snowy plains
            { 0.25f,   4f},  // taiga
            { 0.50f,  14f},  // meadow, cherry grove, warm ocean
            { 0.80f,  20f},  // plains, forest, swamp
            { 0.95f,  28f},  // jungle
            { 1.20f,  33f},  // savanna
            { 1.50f,  40f},  // (hot modded biomes interpolate here)
            { 2.00f,  50f},  // desert, badlands
    };

    private ClimateUnits() {}

    private static float anchorSlope(float[] a, float[] b) {
        return (b[1] - a[1]) / (b[0] - a[0]);
    }

    /**
     * Maps a native biome temperature onto the Celsius greenhouse scale using
     * the piecewise-linear anchor curve. Native values below/above the table
     * extend the first/last segment's slope, clamped to the scale.
     */
    public static float nativeTempToCelsius(float nativeTemp) {
        float[] lo = TEMP_ANCHORS[0];
        float[] hi = TEMP_ANCHORS[TEMP_ANCHORS.length - 1];
        if (nativeTemp <= lo[0])
            return Mth.clamp(lo[1] + (nativeTemp - lo[0]) * anchorSlope(lo, TEMP_ANCHORS[1]), TEMP_C_MIN, TEMP_C_MAX);
        if (nativeTemp >= hi[0])
            return Mth.clamp(hi[1] + (nativeTemp - hi[0]) * anchorSlope(TEMP_ANCHORS[TEMP_ANCHORS.length - 2], hi), TEMP_C_MIN, TEMP_C_MAX);
        for (int i = 1; i < TEMP_ANCHORS.length; i++) {
            if (nativeTemp <= TEMP_ANCHORS[i][0])
                return Mth.clamp(TEMP_ANCHORS[i - 1][1]
                        + (nativeTemp - TEMP_ANCHORS[i - 1][0]) * anchorSlope(TEMP_ANCHORS[i - 1], TEMP_ANCHORS[i]),
                        TEMP_C_MIN, TEMP_C_MAX);
        }
        return Mth.clamp(hi[1], TEMP_C_MIN, TEMP_C_MAX);
    }

    /** Maps a native biome downfall value to the percent humidity scale. */
    public static float downfallToHumidity(float downfall, boolean hasPrecipitation) {
        if (!hasPrecipitation)
            return HUMIDITY_MIN;
        return Mth.clamp(downfall, HUMIDITY_MIN, HUMIDITY_MAX) * 100.0f;
    }

    public static float clampTempC(float tempC) {
        return Mth.clamp(tempC, TEMP_C_MIN, TEMP_C_MAX);
    }

    public static float clampHumidity(float humidity) {
        return Mth.clamp(humidity, HUMIDITY_MIN, HUMIDITY_MAX);
    }
}
