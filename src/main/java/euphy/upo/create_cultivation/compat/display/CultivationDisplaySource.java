package euphy.upo.create_cultivation.compat.display;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import euphy.upo.create_cultivation.content.recipes.CultivatingRecipe;
import euphy.upo.create_cultivation.content.recipes.StackingCultivatingRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Create Display Link source for the Cultivation Tank. Shows the current crop
 * type and the remaining base growth time.
 */
public class CultivationDisplaySource extends DisplaySource {

	@Override
	public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof CultivationTankBlockEntity tankBE)) {
			return DisplaySource.EMPTY;
		}
		CultivationTankBlockEntity controller = tankBE.getControllerBE();
		if (controller == null) {
			return DisplaySource.EMPTY;
		}

		RecipeHolder<?> holder = controller.getCurrentRecipe().orElse(null);
		if (holder == null) {
			return List.of(Component.translatable("create_cultivation.display_source.empty"));
		}

		MutableComponent cropName;
		int recipeDuration;
		if (holder.value() instanceof CultivatingRecipe recipe) {
			cropName = Component.translatable(recipe.getCropBlock().getDescriptionId());
			recipeDuration = recipe.getProcessingDuration();
		} else if (holder.value() instanceof StackingCultivatingRecipe recipe) {
			cropName = Component.translatable(recipe.getBlockToRender().getDescriptionId());
			recipeDuration = recipe.getProcessingDuration();
		} else {
			return DisplaySource.EMPTY;
		}

		int tankDuration = controller.getInternalProcessingDuration();
		int progress = controller.getProgress();
		float ratio = tankDuration > 0 ? Math.min(1f, progress / (float) tankDuration) : 0f;
		int remainingTicks = (int) ((1f - ratio) * recipeDuration);

		List<MutableComponent> lines = new ArrayList<>();
		lines.add(cropName);
		lines.add(Component.translatable("create_cultivation.display_source.time", formatDuration(remainingTicks)));
		return lines;
	}

	private static String formatDuration(int ticks) {
		int totalSeconds = Math.round(ticks / 20.0f);
		if (totalSeconds < 60) {
			return totalSeconds + "s";
		}
		int minutes = totalSeconds / 60;
		int seconds = totalSeconds % 60;
		return seconds == 0 ? minutes + "m" : minutes + "m" + seconds + "s";
	}
}
