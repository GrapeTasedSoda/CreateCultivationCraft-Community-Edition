package euphy.upo.create_cultivation.compat.display;

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
 * Create Display Link source for the Cultivation Tank. Registered twice on the
 * tank's block entity type (crop mode and time mode) so the display link UI
 * offers both as separately selectable options.
 */
public class CultivationDisplaySource extends DisplaySource {

	/** Crop mode: show the type of crop currently planted. */
	public static final int MODE_CROP = 0;
	/** Time mode: show the remaining growth time. */
	public static final int MODE_TIME = 1;

	private final int mode;

	public CultivationDisplaySource(int mode) {
		this.mode = mode;
	}

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

		if (mode == MODE_CROP) {
			return List.of(cropName);
		}

		// Match the Jade tooltip and the base's orange alarm: when the tank is
		// too short for this crop nothing can grow, so a countdown would lie.
		if (controller.isHeightMismatch()) {
			return List.of(Component.translatable("create_cultivation.jade.height_mismatch"));
		}

		// Same source of truth as the Jade tooltip: tick-accurate remaining
		// growth points (batched + in-flight fraction, stacking-aware) divided
		// by the true per-game-tick rate, so both displays agree.
		float remainingPoints = controller.getSmoothRemainingPoints();
		float pointsPerGameTick = controller.getGrowthPointsPerGameTick();
		if (pointsPerGameTick <= 1.0e-5f) {
			return List.of(Component.translatable("create_cultivation.display_source.stalled"));
		}
		int remainingTicks = Math.round(remainingPoints / pointsPerGameTick);
		return List.of(Component.translatable("create_cultivation.display_source.time", formatDuration(remainingTicks)));
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
