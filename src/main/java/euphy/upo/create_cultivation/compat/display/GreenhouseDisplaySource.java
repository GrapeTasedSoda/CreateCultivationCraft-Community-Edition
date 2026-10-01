package euphy.upo.create_cultivation.compat.display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.climate.CropClimate;
import euphy.upo.create_cultivation.content.climate.CropState;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlockEntity;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseScanner;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create Display Link source for the Greenhouse Controller. Registered three
 * times on the controller's block entity type so the display link UI offers
 * them as separately selectable options:
 * <ul>
 * <li>live climate mode - the controlled interior temperature and humidity;</li>
 * <li>device mode - how many climate devices the enclosure scan found;</li>
 * <li>crop mode - the species (with plant counts) currently sitting inside
 * their optimal climate ranges, i.e. the crops receiving the top bonus.</li>
 * </ul>
 * All readouts share the controller GUI's sources of truth: the live values
 * come from the ticked {@code ClimateState} (falling back to the ambient
 * climate before the first scan), the device count from the last enclosure
 * scan, and the crop classification from the same {@code CropClimate#evaluate}
 * call the growth-boost system uses.
 */
public class GreenhouseDisplaySource extends DisplaySource {

	/** Live climate mode: interior temperature and humidity. */
	public static final int MODE_CLIMATE = 0;
	/** Device mode: number of climate devices connected to the greenhouse. */
	public static final int MODE_DEVICES = 1;
	/** Crop mode: crops currently inside their optimal climate ranges. */
	public static final int MODE_CROPS = 2;

	private final int mode;

	public GreenhouseDisplaySource(int mode) {
		this.mode = mode;
	}

	@Override
	public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof GreenhouseControllerBlockEntity controller)) {
			return DisplaySource.EMPTY;
		}
		if (mode == MODE_CLIMATE) {
			return climateLines(controller);
		}
		if (mode == MODE_DEVICES) {
			return deviceLines(controller);
		}
		return cropLines(controller, stats);
	}

	private List<MutableComponent> climateLines(GreenhouseControllerBlockEntity controller) {
		var state = controller.getClimateState();
		// before the first scan the greenhouse simply sits at the ambient
		// climate - same fallback the controller's simulation uses
		float temp = state.isInitialised() ? state.getCurrentTemp() : controller.getAmbientTemp();
		float humidity = state.isInitialised() ? state.getCurrentHumidity() : controller.getAmbientHumidity();
		return List.of(
			Component.translatable("create_cultivation.display_source.greenhouse_temp",
				String.format(Locale.ROOT, "%.1f", temp)),
			Component.translatable("create_cultivation.display_source.greenhouse_humidity",
				String.format(Locale.ROOT, "%.0f", humidity)));
	}

	private List<MutableComponent> deviceLines(GreenhouseControllerBlockEntity controller) {
		GreenhouseScanner.ScanResult scan = controller.getLastScan();
		if (scan == null) {
			return List.of(Component.translatable("create_cultivation.jade.greenhouse.not_scanned"));
		}
		return List.of(Component.translatable("create_cultivation.display_source.greenhouse_devices",
			scan.devices().size()));
	}

	private List<MutableComponent> cropLines(GreenhouseControllerBlockEntity controller, DisplayTargetStats stats) {
		GreenhouseScanner.ScanResult scan = controller.getLastScan();
		if (scan == null) {
			return List.of(Component.translatable("create_cultivation.jade.greenhouse.not_scanned"));
		}
		var state = controller.getClimateState();
		float temp = state.isInitialised() ? state.getCurrentTemp() : controller.getAmbientTemp();
		float humidity = state.isInitialised() ? state.getCurrentHumidity() : controller.getAmbientHumidity();

		// group the crops in their optimal range by species, first-seen order
		Map<Block, Integer> optimalCounts = new LinkedHashMap<>();
		for (BlockPos cropPos : scan.crops()) {
			BlockState cropState = controller.getLevel().getBlockState(cropPos);
			CropClimate climate = cropState.getBlockHolder().getData(CCDataMaps.CROP_CLIMATE);
			if (climate == null) {
				continue;
			}
			if (climate.evaluate(temp, humidity) != CropState.OPTIMAL) {
				continue;
			}
			optimalCounts.merge(cropState.getBlock(), 1, Integer::sum);
		}

		if (optimalCounts.isEmpty()) {
			return List.of(Component.translatable("create_cultivation.display_source.greenhouse_crops_none"));
		}
		List<MutableComponent> lines = new ArrayList<>();
		for (Map.Entry<Block, Integer> e : optimalCounts.entrySet()) {
			if (lines.size() >= stats.maxRows()) {
				break;
			}
			lines.add(Component.translatable(e.getKey().getDescriptionId()).append(" x" + e.getValue()));
		}
		return lines;
	}

	@Override
	public int getPassiveRefreshTicks() {
		// the live climate visibly drifts, so a 1 s cadence keeps the readout
		// honest (matches the controller GUI's snapshot push); the device and
		// crop lists change rarely - keep Create's default 5 s there.
		return mode == MODE_CLIMATE ? 20 : 100;
	}
}
