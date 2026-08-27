package euphy.upo.create_cultivation.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.recipes.CultivatingRecipe;
import euphy.upo.create_cultivation.content.recipes.StackingCultivatingRecipe;
import euphy.upo.create_cultivation.registry.CCBlocks;
import euphy.upo.create_cultivation.registry.CCRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI plugin for Create: Cultivation Craft. Only loaded when JEI is installed.
 * Uses Create's own {@link CreateRecipeCategory} so the layout matches Create.
 */
@JeiPlugin
public class CCJeiPlugin implements IModPlugin {

	private static final ResourceLocation UID = CreateCultivationCraft.asResource("jei_plugin");
	private final List<CreateRecipeCategory<?>> categories = new ArrayList<>();

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		categories.clear();
		categories.add(new CreateRecipeCategory.Builder<CultivatingRecipe>(CultivatingRecipe.class)
			.addTypedRecipes(CCRecipes.CULTIVATING)
			.catalyst(CCBlocks.CULTIVATION_BASE::get)
			.catalyst(CCBlocks.CULTIVATION_TANK::get)
			.itemIcon(CCBlocks.CULTIVATION_BASE.get())
			.emptyBackground(177, 64)
			.build(CreateCultivationCraft.asResource("cultivating"), CultivatingCategory::new));

		categories.add(new CreateRecipeCategory.Builder<StackingCultivatingRecipe>(StackingCultivatingRecipe.class)
			.addTypedRecipes(CCRecipes.STACKING_CULTIVATING)
			.catalyst(CCBlocks.CULTIVATION_BASE::get)
			.catalyst(CCBlocks.CULTIVATION_TANK::get)
			.itemIcon(CCBlocks.CULTIVATION_TANK.get())
			.emptyBackground(177, 64)
			.build(CreateCultivationCraft.asResource("stacking_cultivating"), StackingCultivatingCategory::new));

		registration.addRecipeCategories(categories.toArray(IRecipeCategory[]::new));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		if (Minecraft.getInstance().getConnection() == null) {
			return;
		}
		for (CreateRecipeCategory<?> category : categories) {
			category.registerRecipes(registration);
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		for (CreateRecipeCategory<?> category : categories) {
			category.registerCatalysts(registration);
		}
	}
}
