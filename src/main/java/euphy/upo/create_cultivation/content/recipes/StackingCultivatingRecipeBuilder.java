package euphy.upo.create_cultivation.content.recipes;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class StackingCultivatingRecipeBuilder extends ProcessingRecipeBuilder<StackingCultivatingRecipeParams, StackingCultivatingRecipe, StackingCultivatingRecipeBuilder> {

    public StackingCultivatingRecipeBuilder(ResourceLocation recipeId) {
        super(StackingCultivatingRecipe::new, recipeId);
    }


    public StackingCultivatingRecipeBuilder result(ProcessingOutput output) {
        this.params.result = output;
        return this;
    }

    public StackingCultivatingRecipeBuilder maxHeight(int height) {
        this.params.maxHeight = height;
        return this;
    }

    public StackingCultivatingRecipeBuilder minHeight(int height) {
        this.params.minHeight = height;
        return this;
    }

    public StackingCultivatingRecipeBuilder blockToRender(Block block) {
        this.params.blockToRender = block;
        return this;
    }

    public StackingCultivatingRecipeBuilder topRender(Block block) {
        this.params.topRender = block;
        return this;
    }

    public StackingCultivatingRecipeBuilder stageByProgress(boolean stageByProgress) {
        this.params.stageByProgress = stageByProgress;
        return this;
    }

    public StackingCultivatingRecipeBuilder catalyst(Ingredient catalyst) {
        this.params.catalyst = catalyst;
        return this;
    }

    public StackingCultivatingRecipeBuilder catalystUse(int ticks) {
        this.params.catalystUse = ticks;
        return this;
    }


    @Override
    protected StackingCultivatingRecipeParams createParams() {
        return new StackingCultivatingRecipeParams();
    }

    @Override
    public StackingCultivatingRecipeBuilder self() {
        return this;
    }
}
