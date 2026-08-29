package euphy.upo.create_cultivation.content.recipes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.codec.CreateCodecs;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CultivatingRecipeParams extends ProcessingRecipeParams {

    public Block cropBlock;
    public int height;
    /** The fluid that waters this crop. {@code null} means the default (water). */
    public Fluid irrigant;
    /** The catalyst item accepted by the base's catalyst slot. {@code null} means the default (bone meal). */
    public Ingredient catalyst;
    /** How many boosted ticks one catalyst item lasts. */
    public int catalystUse;

    public CultivatingRecipeParams() {
        super();
        this.cropBlock = Blocks.AIR;
        this.height = 1;
        this.irrigant = null;
        this.catalyst = null;
        this.catalystUse = 600;
    }

    public static final MapCodec<CultivatingRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(

            Codec.either(CreateCodecs.SIZED_FLUID_INGREDIENT, Ingredient.CODEC).listOf().fieldOf("ingredients").forGetter(p -> {
                List<Either<SizedFluidIngredient, Ingredient>> ingredients = new ArrayList<>();
                p.ingredients.forEach(i -> ingredients.add(Either.right(i)));
                p.fluidIngredients.forEach(i -> ingredients.add(Either.left(i)));
                return ingredients;
            }),
            Codec.either(FluidStack.CODEC, ProcessingOutput.CODEC).listOf().fieldOf("results").forGetter(p -> {
                List<Either<FluidStack, ProcessingOutput>> results = new ArrayList<>();
                p.results.forEach(r -> results.add(Either.right(r)));
                p.fluidResults.forEach(r -> results.add(Either.left(r)));
                return results;
            }),
            Codec.INT.optionalFieldOf("processingDuration", 100).forGetter(p -> p.processingDuration),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("crop_block").forGetter(p -> ((CultivatingRecipeParams)p).cropBlock),
            Codec.INT.optionalFieldOf("height", 1).forGetter(p -> ((CultivatingRecipeParams)p).height),
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("irrigant").forGetter(p -> Optional.ofNullable(p.irrigant)),
            Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(p -> Optional.ofNullable(((CultivatingRecipeParams)p).catalyst)),
            Codec.INT.optionalFieldOf("catalyst_use", 600).forGetter(p -> ((CultivatingRecipeParams)p).catalystUse)
    ).apply(instance, (ingredients, results, duration, cropBlock, height, irrigant, catalyst, catalystUse) -> {
        CultivatingRecipeParams params = new CultivatingRecipeParams();
        ingredients.forEach(either -> either.ifRight(params.ingredients::add).ifLeft(params.fluidIngredients::add));
        results.forEach(either -> either.ifRight(params.results::add).ifLeft(params.fluidResults::add));
        params.processingDuration = duration;
        params.cropBlock = cropBlock;
        params.height = height;
        params.irrigant = irrigant.orElse(null);
        params.catalyst = catalyst.orElse(null);
        params.catalystUse = catalystUse;
        return params;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivatingRecipeParams> STREAM_CODEC =
            ProcessingRecipeParams.streamCodec(CultivatingRecipeParams::new);

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(this.cropBlock));
        buffer.writeInt(this.height);
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
        this.cropBlock = BuiltInRegistries.BLOCK.get(buffer.readResourceLocation());
        this.height = buffer.readInt();
        this.irrigant = buffer.readBoolean() ? ByteBufCodecs.registry(Registries.FLUID).decode(buffer) : null;
        this.catalyst = buffer.readBoolean() ? Ingredient.CONTENTS_STREAM_CODEC.decode(buffer) : null;
        this.catalystUse = buffer.readInt();
    }
}
