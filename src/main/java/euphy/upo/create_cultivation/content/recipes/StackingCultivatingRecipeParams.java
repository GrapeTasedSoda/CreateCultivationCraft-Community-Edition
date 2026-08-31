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
    /** Minimum tank height (in blocks) required for this crop to grow at all. */
    public int minHeight;

    public Block blockToRender;
    /** Optional separate block rendered for the topmost layer (e.g. rice panicles). Null = same as blockToRender. */
    public Block topRender;
    /** When true, the growing layer's "age" property maps to growth progress instead of always rendering full age. */
    public boolean stageByProgress;
    /** The fluid that waters this crop. {@code null} means the default (water). */
    public Fluid irrigant;
    /** The catalyst item accepted by the base's catalyst slot. {@code null} means the default (bone meal). */
    public Ingredient catalyst;
    /** How many boosted ticks one catalyst item lasts. */
    public int catalystUse;

    public StackingCultivatingRecipeParams() {
        super();
        this.blockToRender = Blocks.AIR;
        this.topRender = null;
        this.stageByProgress = false;
        this.irrigant = null;
        this.catalyst = null;
        this.catalystUse = 600;
        this.minHeight = 1;
    }


    public static final MapCodec<StackingCultivatingRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(p -> p.ingredients.get(0)),
            ProcessingOutput.CODEC.fieldOf("result").forGetter(p -> ((StackingCultivatingRecipeParams)p).result),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block_to_render").forGetter(p -> ((StackingCultivatingRecipeParams)p).blockToRender),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("top_render").forGetter(p -> Optional.ofNullable(((StackingCultivatingRecipeParams)p).topRender)),
            Codec.BOOL.optionalFieldOf("stage_by_progress", false).forGetter(p -> ((StackingCultivatingRecipeParams)p).stageByProgress),
            Codec.INT.optionalFieldOf("processingDuration", 200).forGetter(p -> p.processingDuration),
            Codec.INT.optionalFieldOf("maxHeight", 3).forGetter(p -> ((StackingCultivatingRecipeParams)p).maxHeight),
            Codec.INT.optionalFieldOf("min_height", 1).forGetter(p -> ((StackingCultivatingRecipeParams)p).minHeight),
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("irrigant").forGetter(p -> Optional.ofNullable(p.irrigant)),
            Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(p -> Optional.ofNullable(((StackingCultivatingRecipeParams)p).catalyst)),
            Codec.INT.optionalFieldOf("catalyst_use", 600).forGetter(p -> ((StackingCultivatingRecipeParams)p).catalystUse)
    ).apply(instance, (ingredient, result, block, topBlock, stageByProgress, duration, maxHeight, minHeight, irrigant, catalyst, catalystUse) -> {
        StackingCultivatingRecipeParams params = new StackingCultivatingRecipeParams();
        params.ingredients.add(ingredient);
        params.result = result;
        params.blockToRender = block;
        params.topRender = topBlock.orElse(null);
        params.stageByProgress = stageByProgress;
        params.processingDuration = duration;
        params.maxHeight = maxHeight;
        params.minHeight = minHeight;
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
        buffer.writeBoolean(this.topRender != null);
        if (this.topRender != null) {
            buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(this.topRender));
        }
        buffer.writeBoolean(this.stageByProgress);
        buffer.writeInt(this.maxHeight);
        buffer.writeInt(this.minHeight);
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
        this.topRender = buffer.readBoolean() ? BuiltInRegistries.BLOCK.get(buffer.readResourceLocation()) : null;
        this.stageByProgress = (buffer.readBoolean());
        this.maxHeight = buffer.readInt();
        this.minHeight = buffer.readInt();
        this.irrigant = buffer.readBoolean() ? ByteBufCodecs.registry(Registries.FLUID).decode(buffer) : null;
        this.catalyst = buffer.readBoolean() ? Ingredient.CONTENTS_STREAM_CODEC.decode(buffer) : null;
        this.catalystUse = buffer.readInt();
    }
}
