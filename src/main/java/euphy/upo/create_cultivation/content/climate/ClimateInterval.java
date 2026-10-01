package euphy.upo.create_cultivation.content.climate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A closed numeric interval used for crop climate ranges. JSON shape:
 * {@code {"min": 16, "max": 30}}.
 */
public record ClimateInterval(float min, float max) {

    public static final Codec<ClimateInterval> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("min").forGetter(ClimateInterval::min),
            Codec.FLOAT.fieldOf("max").forGetter(ClimateInterval::max)
    ).apply(i, ClimateInterval::new));

    public boolean contains(float value) {
        return value >= min && value <= max;
    }
}
