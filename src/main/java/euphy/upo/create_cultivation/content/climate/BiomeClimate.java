package euphy.upo.create_cultivation.content.climate;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Climate override for a single biome, attached through the
 * {@code create_cultivation:biome_climate} biome data map (keys may be biome
 * ids or {@code #tags}). Both fields are optional:
 *
 * <ul>
 * <li>{@code temperature} overrides the mapped biome temperature entirely, in
 * °C;</li>
 * <li>{@code humidity} overrides the derived humidity entirely, in %RH.
 * Biomes without an entry fall back to their vanilla downfall value.</li>
 * </ul>
 */
public record BiomeClimate(Optional<Float> temperature, Optional<Float> humidity) {

    public static final Codec<BiomeClimate> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("temperature").forGetter(BiomeClimate::temperature),
            Codec.FLOAT.optionalFieldOf("humidity").forGetter(BiomeClimate::humidity)
    ).apply(i, BiomeClimate::new));
}
