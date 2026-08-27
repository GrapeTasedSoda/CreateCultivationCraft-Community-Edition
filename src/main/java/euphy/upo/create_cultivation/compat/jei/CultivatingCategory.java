package euphy.upo.create_cultivation.compat.jei;

import java.util.List;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import euphy.upo.create_cultivation.content.recipes.CultivatingRecipe;
import net.minecraft.world.level.material.Fluid;

public class CultivatingCategory extends AbstractCultivatingCategory<CultivatingRecipe> {

	public CultivatingCategory(Info<CultivatingRecipe> info) {
		super(info);
	}

	@Override
	protected List<ProcessingOutput> getOutputs(CultivatingRecipe recipe) {
		return recipe.getRollableResults();
	}

	@Override
	protected int getDuration(CultivatingRecipe recipe) {
		return recipe.getProcessingDuration();
	}

	@Override
	protected Fluid getIrrigant(CultivatingRecipe recipe) {
		return recipe.getIrrigant();
	}
}
