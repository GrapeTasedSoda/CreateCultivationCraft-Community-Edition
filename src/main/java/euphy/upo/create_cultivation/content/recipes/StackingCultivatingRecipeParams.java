package euphy.upo.create_cultivation.content.recipes;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

public class StackingCultivatingRecipeParams extends ProcessingRecipeParams {

    public ProcessingOutput result;
    public int maxHeight;

    public Block blockToRender;
    /** The fluid that waters this crop. {@code null} means the default (water). */
    public Fluid irrigant;
    /** The catalyst item accepted by the base's catalyst slot. {@code null} means the default (bone meal). */
    public Ingredient catalyst;
    /** How many boosted ticks one catalyst item lasts. */
    public int catalystUse;

    public StackingCultivatingRecipeParams() {
        super();
        this.blockToRender = Blocks.AIR;
        this.irrigant = null;
        this.catalyst = null;
        this.catalystUse = 600;
    }


    public static final MapCodec<StackingCultivatingRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(p -> p.ingredients.get(0)),
            ProcessingOutput.CODEC.fieldOf("result").forGetter(p -> ((StackingCultivatingRecipeParams)p).result),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block_to_render").forGetter(p -> ((StackingCultivatingRecipeParams)p).blockToRender),
            Codec.INT.optionalFieldOf("processingDuration", 200).forGetter(p -> p.processingDuration),
            Codec.INT.optionalFieldOf("maxHeight", 3).forGetter(p -> ((StackingCultivatingRecipeParams)p).maxHeight),
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("irrigant").forGetter(p -> Optional.ofNullable(p.irrigant)),
            Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(p -> Optional.ofNullable(((StackingCultivatingRecipeParams)p).catalyst)),
            Codec.INT.optionalFieldOf("catalyst_use", 600).forGetter(p -> ((StackingCultivatingRecipeParams)p).catalystUse)
    ).apply(instance, (ingredient, result, block, duration, maxHeight, irrigant, catalyst, catalystUse) -> {
        StackingCultivatingRecipeParams params = new StackingCultivatingRecipeParams();
        params.ingredients.add(ingredient);
        params.result = result;
        params.blockToRender = block;
        params.processingDuration = duration;
        params.maxHeight = maxHeight;
        params.irrigant = irrigant.orElse(null);
        params.catalyst = catalyst.orElse(null);
        params.catalystUse = catalystUse;
        return params;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, StackingCultivatingRecipeParams> STREAM_CODEC =
            ProcessingRecipeParams.streamCodec(StackingCultivatingRecipeParams::new);


    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        ProcessingOutput.STREAM_CODEC.encode(buffer, this.result);
        buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(this.blockToRender));
        buffer.writeInt(this.maxHeight);
        buffer.writeBoolean(this.irrigant != null);
        if (this.irrigant != null) {
            ByteBufCodecs.registry(Registries.FLUID).encode(buffer, this.irrigant);
        }
        buffer.writeBoolean(this.catalyst != null);
        if (this.catalyst != null) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, this.catalyst);
        }
        buffer.writeInt(this.catalystUse);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        this.result = ProcessingOutput.STREAM_CODEC.decode(buffer);
        this.blockToRender = BuiltInRegistries.BLOCK.get(buffer.readResourceLocation());
        this.maxHeight = buffer.readInt();
        this.irrigant = buffer.readBoolean() ? ByteBufCodecs.registry(Registries.FLUID).decode(buffer) : null;
        this.catalyst = buffer.readBoolean() ? Ingredient.CONTENTS_STREAM_CODEC.decode(buffer) : null;
        this.catalystUse = buffer.readInt();
    }
}
