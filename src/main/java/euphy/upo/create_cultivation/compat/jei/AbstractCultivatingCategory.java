package euphy.upo.create_cultivation.compat.jei;

import java.util.List;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import euphy.upo.create_cultivation.content.recipes.CultivatingRecipe;
import euphy.upo.create_cultivation.content.recipes.StackingCultivatingRecipe;
import euphy.upo.create_cultivation.registry.CCBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Create-style JEI category for the cultivation recipes: seed input on the left,
 * the machine (power component) in the middle, and the produced crops stacked on
 * the right, with the base growth time shown at the top.
 */
public abstract class AbstractCultivatingCategory<T extends Recipe<RecipeInput>> extends CreateRecipeCategory<T> {

	private static final int INPUT_X = 15;
	private static final int INPUT_Y = 24;
	private static final int FLUID_Y = 44;
	private static final int ARROW_X = 42;
	private static final int ARROW_Y = 26;
	private static final int MACHINE_X = 84;
	private static final int OUTPUT_X = 104;
	private static final int OUTPUT_Y = 24;
	/** Catalyst indicator, right of centre above the arrow. */
	private static final int CATALYST_X = 50;
	private static final int CATALYST_Y = 10;
	/** Width of the empty background used at registration (177 px). */
	private static final int CATEGORY_WIDTH = 177;

	protected AbstractCultivatingCategory(Info<T> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
		if (!recipe.getIngredients().isEmpty()) {
			builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
				.setBackground(getRenderedSlot(), -1, -1)
				.addIngredients(recipe.getIngredients().get(0));
		}

		Fluid irrigant = getIrrigant(recipe);
		if (irrigant == null) {
			irrigant = Fluids.WATER;
		}
		builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, FLUID_Y)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(irrigant, 1000));

		List<ProcessingOutput> outputs = getOutputs(recipe);
		for (int i = 0; i < outputs.size(); i++) {
			ProcessingOutput output = outputs.get(i);
			int xOffset = (i % 4) * 18;
			int yOffset = (i / 4) * 18;
			builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + xOffset, OUTPUT_Y + yOffset)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
		}

		// Catalyst indicator above the arrow: display only, no focus matching.
		builder.addSlot(RecipeIngredientRole.RENDER_ONLY, CATALYST_X, CATALYST_Y)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStack(getCatalystDisplay(recipe));
	}

	@Override
	public void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {
		AllGuiTextures.JEI_ARROW.render(gui, ARROW_X, ARROW_Y);

		// Power component: Cultivation Tank sitting on top of the kinetic Cultivation Base.
		gui.renderItem(new ItemStack(CCBlocks.CULTIVATION_TANK.get()), MACHINE_X, 8);
		gui.renderItem(new ItemStack(CCBlocks.CULTIVATION_BASE.get()), MACHINE_X, 28);

		// Base growth time, right-aligned so it never overlaps the catalyst
		// indicator above the arrow.
		String time = formatDuration(getDuration(recipe));
		Component timeText = Component.translatable("create_cultivation.jei.growth_time", time)
			.withStyle(ChatFormatting.GRAY);
		int timeX = CATEGORY_WIDTH - Minecraft.getInstance().font.width(timeText) - 2;
		gui.drawString(Minecraft.getInstance().font, timeText, timeX, 4, 0xFFFFFF, false);
	}

	/**
	 * The catalyst item this recipe expects in the base's catalyst slot. Falls
	 * back to bone meal when the recipe does not define one, mirroring the
	 * machine's own fallback in {@code CultivationBaseBlockEntity#isCatalyst}.
	 */
	private static ItemStack getCatalystDisplay(Recipe<?> recipe) {
		Ingredient catalyst = getCatalystIngredient(recipe);
		if (catalyst == null || catalyst.isEmpty()) {
			return new ItemStack(Items.BONE_MEAL);
		}
		ItemStack[] items = catalyst.getItems();
		if (items.length == 0 || items[0].isEmpty()) {
			return new ItemStack(Items.BONE_MEAL);
		}
		return items[0].copy();
	}

	private static Ingredient getCatalystIngredient(Recipe<?> recipe) {
		if (recipe instanceof CultivatingRecipe cultivating) {
			return cultivating.getCatalyst();
		}
		if (recipe instanceof StackingCultivatingRecipe stacking) {
			return stacking.getCatalyst();
		}
		return null;
	}

	/** Formats a duration in ticks into something like "1m30s". */
	protected static String formatDuration(int ticks) {
		int totalSeconds = Math.round(ticks / 20.0f);
		if (totalSeconds < 60) {
			return totalSeconds + "s";
		}
		int minutes = totalSeconds / 60;
		int seconds = totalSeconds % 60;
		return seconds == 0 ? minutes + "m" : minutes + "m" + seconds + "s";
	}

	protected abstract List<ProcessingOutput> getOutputs(T recipe);

	protected abstract int getDuration(T recipe);

	protected abstract Fluid getIrrigant(T recipe);
}
