package euphy.upo.create_cultivation.compat.display;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseBlockEntity;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Locale;

/**
 * Create Display Link source for the Cultivation Base. Registered twice on the
 * base's block entity type (output mode and catalyst mode) so the display link
 * UI offers both as separately selectable options.
 */
public class BaseDisplaySource extends DisplaySource {

	/** Output mode: list the harvest items stored in the base's 8 output slots. */
	public static final int MODE_OUTPUT = 0;
	/** Catalyst mode: show how many catalyst items sit in the catalyst slot. */
	public static final int MODE_CATALYST = 1;
	/** Multiplier mode: show the current yield and growth rate multipliers. */
	public static final int MODE_MULTIPLIERS = 2;

	private final int mode;

	public BaseDisplaySource(int mode) {
		this.mode = mode;
	}

	@Override
	public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof CultivationBaseBlockEntity baseBE)) {
			return DisplaySource.EMPTY;
		}

		ItemStackHandler handler = baseBE.getItemHandler();

		if (mode == MODE_CATALYST) {
			ItemStack catalyst = handler.getStackInSlot(CultivationBaseBlockEntity.CATALYST_SLOT);
			if (catalyst.isEmpty()) {
				return List.of(Component.translatable("create_cultivation.display_source.base_catalyst_none"));
			}
			return List.of(Component.translatable(
				"create_cultivation.display_source.base_catalyst_item", catalyst.getHoverName(), catalyst.getCount()));
		}

		if (mode == MODE_MULTIPLIERS) {
			// Must mirror the harvest logic exactly: config yield x fertilizer
			// yield, x watering bonus when watered, x synergy when watered AND
			// fertilized.
			double yieldMultiplier = CCConfig.CROP_YIELD.get() * baseBE.getCatalystYieldMultiplier();
			double growthMultiplier = 0.0;

			BlockEntity beAbove = baseBE.getLevel().getBlockEntity(baseBE.getBlockPos().above());
			if (beAbove instanceof CultivationTankBlockEntity tankBE) {
				CultivationTankBlockEntity controllerBE = tankBE.getControllerBE();
				if (controllerBE != null && controllerBE.isHeightMismatch()) {
					// Match the orange alarm and the display link time readout:
					// nothing can grow while the tank is too short, so showing
					// live multipliers would be misleading.
					return List.of(
						Component.translatable("create_cultivation.display_source.base_yield_multiplier",
							String.format(Locale.ROOT, "%.2f", 0.0)),
						Component.translatable("create_cultivation.display_source.base_growth_rate",
							String.format(Locale.ROOT, "%.2f", 0.0)));
				}
				growthMultiplier = tankBE.getSpeedMultiplier() * CCConfig.GROWTH_RATE.get()
					* baseBE.getCatalystGrowthMultiplier();
				if (tankBE.isWatered()) {
					growthMultiplier *= CCConfig.WATERING_GROWTH_BONUS.get();
					yieldMultiplier *= CCConfig.WATERING_YIELD_BONUS.get();
					if (baseBE.isCatalystBoostActive()) {
						yieldMultiplier *= CCConfig.WATER_CATALYST_SYNERGY_BONUS.get();
						growthMultiplier *= CCConfig.WATER_CATALYST_SYNERGY_BONUS.get();
					}
				}
			}

			return List.of(
				Component.translatable("create_cultivation.display_source.base_yield_multiplier",
					String.format(Locale.ROOT, "%.2f", yieldMultiplier)),
				Component.translatable("create_cultivation.display_source.base_growth_rate",
					String.format(Locale.ROOT, "%.2f", growthMultiplier)));
		}

		List<MutableComponent> lines = new ArrayList<>();
		for (int slot = 0; slot < CultivationBaseBlockEntity.SLOT_COUNT && lines.size() < stats.maxRows(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (!stack.isEmpty()) {
				lines.add(stack.getHoverName().copy().append(" x" + stack.getCount()));
			}
		}
		if (lines.isEmpty()) {
			return List.of(Component.translatable("create_cultivation.display_source.base_empty"));
		}
		return lines;
	}
}
