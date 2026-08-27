package euphy.upo.create_cultivation.compat.jei;

import java.util.List;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import euphy.upo.create_cultivation.content.recipes.StackingCultivatingRecipe;
import net.minecraft.world.level.material.Fluid;

public class StackingCultivatingCategory extends AbstractCultivatingCategory<StackingCultivatingRecipe> {

	public StackingCultivatingCategory(Info<StackingCultivatingRecipe> info) {
		super(info);
	}

	@Override
	protected List<ProcessingOutput> getOutputs(StackingCultivatingRecipe recipe) {
		return List.of(recipe.getResult());
	}

	@Override
	protected int getDuration(StackingCultivatingRecipe recipe) {
		return recipe.getProcessingDuration();
	}

	@Override
	protected Fluid getIrrigant(StackingCultivatingRecipe recipe) {
		return recipe.getIrrigant();
	}
}
