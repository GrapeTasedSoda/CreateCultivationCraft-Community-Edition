package euphy.upo.create_cultivation.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * In-game configuration for Create: Cultivation Craft.
 *
 * <p>Registered as a NeoForge COMMON config so it is loaded on both the physical
 * client and the integrated/dedicated server, and so the values can be edited live
 * from the NeoForge config screen ("Mods &gt; create_cultivation &gt; Config") that
 * {@code CreateCultivationCraftClient} already wires up via
 * {@code IConfigScreenFactory/ConfigurationScreen}.</p>
 *
 * <p>Values are grouped into two top-level sections: {@code cultivation_tank}
 * (the base/tank machine: growth, yield, watering, catalysts) and
 * {@code greenhouse} (climate control devices and crop climate bonuses).</p>
 */
public final class CCConfig {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec SPEC;

	// ------------------------------------------------------------------
	// Cultivation tank section
	// ------------------------------------------------------------------

	/** Scales how fast crops mature. 1.0 = vanilla speed of this addon. */
	public static final ModConfigSpec.DoubleValue GROWTH_RATE;

	/** Scales the amount of crops produced per harvest. */
	public static final ModConfigSpec.DoubleValue CROP_YIELD;

	/** Extra yield multiplier applied on top of {@link #CROP_YIELD} while the tank is watered. */
	public static final ModConfigSpec.DoubleValue WATERING_YIELD_BONUS;

	/** Extra growth speed multiplier while the tank is watered. */
	public static final ModConfigSpec.DoubleValue WATERING_GROWTH_BONUS;

	/** How long (in lazy ticks) a "watered" state lasts after a Spout waters the tank. */
	public static final ModConfigSpec.IntValue WATERED_DURATION;

	/** Table-driven catalyst definitions: item;duration;growth;yield per entry. */
	public static final ModConfigSpec.ConfigValue<List<? extends String>> CATALYSTS;

	/** Extra multiplier applied to both growth speed and yield while the tank is watered AND the catalyst is active. */
	public static final ModConfigSpec.DoubleValue WATER_CATALYST_SYNERGY_BONUS;

	/** Chance (0-1) of the Efficient Fertilizer's bonus second application on direct use. */
	public static final ModConfigSpec.DoubleValue FERTILIZER_BONUS_CHANCE;

	// ------------------------------------------------------------------
	// Greenhouse section
	// ------------------------------------------------------------------

	/** Climate work of one air conditioner (degC * blocks). */
	public static final ModConfigSpec.DoubleValue AIRCONDITIONER_CAPACITY;

	/** Climate work of one humidifier (%RH * blocks, raises humidity). */
	public static final ModConfigSpec.DoubleValue HUMIDIFIER_CAPACITY;

	/** Climate work of one dehumidifier (%RH * blocks, lowers humidity). */
	public static final ModConfigSpec.DoubleValue DEHUMIDIFIER_CAPACITY;

	/** Climate change speed factor (dimensionless multiplier). */
	public static final ModConfigSpec.DoubleValue CLIMATE_RATE;

	/** Hard cap on the scanned greenhouse volume (blocks). */
	public static final ModConfigSpec.IntValue MAX_GREENHOUSE_VOLUME;

	/** Ticks between enclosure re-scans while powered. */
	public static final ModConfigSpec.IntValue SCAN_INTERVAL_TICKS;

	/** Horizontal radius of the sprinkler's hydrated farmland area. */
	public static final ModConfigSpec.IntValue SPRINKLER_AREA_RADIUS;

	/** Ticks between outdoor crop climate re-checks. */
	public static final ModConfigSpec.IntValue AMBIENT_RECHECK_INTERVAL_TICKS;

	/** Yield multiplier while temperature AND humidity are inside the optimal ranges. */
	public static final ModConfigSpec.DoubleValue CLIMATE_OPTIMAL_YIELD;

	/** Growth speed multiplier while temperature AND humidity are inside the optimal ranges. */
	public static final ModConfigSpec.DoubleValue CLIMATE_OPTIMAL_GROWTH;

	/** Yield multiplier while exactly one dimension is optimal, the other inside survival. */
	public static final ModConfigSpec.DoubleValue CLIMATE_SURVIVAL_YIELD;

	/** Growth speed multiplier while exactly one dimension is optimal, the other inside survival. */
	public static final ModConfigSpec.DoubleValue CLIMATE_SURVIVAL_GROWTH;

	/** Yield multiplier while both dimensions are inside survival but neither is optimal. */
	public static final ModConfigSpec.DoubleValue CLIMATE_SURVIVAL_ONLY_YIELD;

	/** Growth speed multiplier while both dimensions are inside survival but neither is optimal. */
	public static final ModConfigSpec.DoubleValue CLIMATE_SURVIVAL_ONLY_GROWTH;

	// ------------------------------------------------------------------
	// Ambient (outdoor) climate section
	// ------------------------------------------------------------------

	/** Master switch for the ambient climate crop effects (default off). */
	public static final ModConfigSpec.BooleanValue AMBIENT_CROPS_ENABLED;

	/** Ambient yield bonus while temperature AND humidity are both optimal. */
	public static final ModConfigSpec.DoubleValue AMBIENT_OPTIMAL_YIELD;

	/** Ambient growth bonus while temperature AND humidity are both optimal. */
	public static final ModConfigSpec.DoubleValue AMBIENT_OPTIMAL_GROWTH;

	/** Ambient yield while exactly one dimension is optimal, the other in survival. */
	public static final ModConfigSpec.DoubleValue AMBIENT_PARTIAL_YIELD;

	/** Ambient growth while exactly one dimension is optimal, the other in survival. */
	public static final ModConfigSpec.DoubleValue AMBIENT_PARTIAL_GROWTH;

	/** Ambient yield while both dimensions are merely surviving (no bonus by default). */
	public static final ModConfigSpec.DoubleValue AMBIENT_SURVIVAL_ONLY_YIELD;

	/** Ambient growth while both dimensions are merely surviving (no bonus by default). */
	public static final ModConfigSpec.DoubleValue AMBIENT_SURVIVAL_ONLY_GROWTH;

	/** Master switch: reject fertilizer use on stalled crops. */
	public static final ModConfigSpec.BooleanValue STALL_BLOCK_ENABLED;

	/** Item ids rejected on stalled crops (one registry id per entry). */
	public static final ModConfigSpec.ConfigValue<List<? extends String>> STALL_BLOCKED_FERTILIZERS;

	// ------------------------------------------------------------------
	// Serene Seasons section
	// ------------------------------------------------------------------

	/** Humidity offset (%RH) applied to the resolved biome humidity during Spring. */
	public static final ModConfigSpec.DoubleValue SS_SPRING_HUMIDITY_OFFSET;

	/** Humidity offset (%RH) applied to the resolved biome humidity during Summer. */
	public static final ModConfigSpec.DoubleValue SS_SUMMER_HUMIDITY_OFFSET;

	/** Humidity offset (%RH) applied to the resolved biome humidity during Autumn. */
	public static final ModConfigSpec.DoubleValue SS_AUTUMN_HUMIDITY_OFFSET;

	/** Humidity offset (%RH) applied to the resolved biome humidity during Winter. */
	public static final ModConfigSpec.DoubleValue SS_WINTER_HUMIDITY_OFFSET;

	static {
		BUILDER.push("cultivation_tank")
			.comment("Cultivation tank machine settings: growth speed, harvest yield,")
			.comment("watering bonuses and the catalyst table.");

		BUILDER.push("general")
			.comment("General gameplay settings for the cultivation machine.");

		GROWTH_RATE = BUILDER
			.comment("Multiplier for crop growth speed. 1.0 is the default. Higher = faster.")
			.translation("create_cultivation.config.growthRateMultiplier")
			.defineInRange("growthRateMultiplier", 1.0, 0.05, 20.0);

		CROP_YIELD = BUILDER
			.comment("Multiplier for the amount of crops produced on each harvest. 1.0 is the default.")
			.translation("create_cultivation.config.cropYieldMultiplier")
			.defineInRange("cropYieldMultiplier", 1.0, 0.1, 64.0);

		BUILDER.pop().push("watering")
			.comment("Settings related to watering the cultivation tank with a Spout.");

		WATERING_YIELD_BONUS = BUILDER
			.comment("Extra yield multiplier applied on top of cropYieldMultiplier while the tank is watered. 1.5 means watered harvests produce 1.5x the crops; 1.0 disables the bonus.")
			.translation("create_cultivation.config.wateringYieldBonus")
			.defineInRange("wateringYieldBonus", 1.5, 1.0, 10.0);

		WATERING_GROWTH_BONUS = BUILDER
			.comment("Extra growth speed multiplier while the tank is watered. 2.0 means watered crops grow 2x as fast; 1.0 disables the bonus.")
			.translation("create_cultivation.config.wateringGrowthBonus")
			.defineInRange("wateringGrowthBonus", 2.0, 1.0, 10.0);

		WATERED_DURATION = BUILDER
			.comment("How long the 'watered' state lasts (in lazy ticks, ~0.5s each; 20 = 10 seconds) after a Spout waters the tank.")
			.translation("create_cultivation.config.wateredDuration")
			.defineInRange("wateredDuration", 20, 1, 600);

		BUILDER.pop().push("catalyst")
			.comment("Settings for the Cultivation Base catalyst slot.");

		CATALYSTS = BUILDER
			.comment("Accepted catalyst items and their effects. One entry per catalyst, formatted 'item;durationTicks;growthMultiplier;yieldMultiplier'.",
				"durationTicks: boosted ticks one item lasts (20 = 1 second).",
				"growthMultiplier/yieldMultiplier: multipliers while active; values below 1.0 slow crops down or reduce yield.",
				"Unknown item ids (e.g. from mods that are not installed) are ignored.")
			.translation("create_cultivation.config.catalysts")
			.defineListAllowEmpty("catalysts", List.of(
			  "minecraft:bone_meal;600;2.0;1.5",
			  "farmersdelight:organic_compost;1800;4.0;2.0",
			  "mynethersdelight:letios_compost;1800;2.0;4.0",
			  "create_cultivation:efficient_fertilizer;900;3.0;2.0"),
				o -> o instanceof String s && s.split(";").length == 4);

		BUILDER.pop().push("synergy")
			.comment("Bonus applied while watering and the catalyst are active at the same time.");

		WATER_CATALYST_SYNERGY_BONUS = BUILDER
			.comment("Extra multiplier applied to BOTH growth speed and harvest yield while the tank is watered AND the catalyst is active. 1.5 means 50% extra on top of the existing multipliers; 1.0 disables the synergy.")
			.translation("create_cultivation.config.waterCatalystSynergyBonus")
			.defineInRange("waterCatalystSynergyBonus", 1.5, 1.0, 10.0);

		BUILDER.pop();

		BUILDER.push("fertilizer")
			.comment("The standalone Efficient Fertilizer item's direct-use mode:")
			.comment("right-clicking a crop behaves like bone meal with a bonus application.");

		FERTILIZER_BONUS_CHANCE = BUILDER
			.comment("Chance (0.0-1.0) of a second full bone-meal application per use, on a copy of the stack (no extra item consumed). 0.5 = 1.5x bone meal effect on average; 0.0 = plain bone meal strength.")
			.translation("create_cultivation.config.fertilizerBonusChance")
			.defineInRange("bonusApplicationChance", 0.5, 0.0, 1.0);

		BUILDER.pop();

		BUILDER.pop();

		BUILDER.push("greenhouse")
			.comment("Greenhouse settings: climate control devices and the crop")
			.comment("bonuses granted by the controlled climate.");

		BUILDER.push("climate_control")
			.comment("Climate control: device capacity decides both how far")
			.comment("the climate can be pushed (|setpoint - ambient| * volume must fit")
			.comment("into the summed capacity) and how fast it moves there.");

		AIRCONDITIONER_CAPACITY = BUILDER
			.comment("Climate work one air conditioner provides, in degC*blocks. 4000 means")
			.comment("one unit shifts a 1000-block greenhouse by 4 degC or a 100-block one by 40 degC.")
			.translation("create_cultivation.config.airconditionerCapacity")
			.defineInRange("airconditionerCapacity", 4000.0, 1.0, 1000000.0);

		HUMIDIFIER_CAPACITY = BUILDER
			.comment("Climate work one humidifier provides, in %RH*blocks (raises humidity).")
			.translation("create_cultivation.config.humidifierCapacity")
			.defineInRange("humidifierCapacity", 4000.0, 1.0, 1000000.0);

		DEHUMIDIFIER_CAPACITY = BUILDER
			.comment("Climate work one dehumidifier provides, in %RH*blocks (lowers humidity).")
			.translation("create_cultivation.config.dehumidifierCapacity")
			.defineInRange("dehumidifierCapacity", 4000.0, 1.0, 1000000.0);

		CLIMATE_RATE = BUILDER
			.comment("Climate change speed factor. The live value moves at capacity/volume")
			.comment("units per tick multiplied by this; 1.0 = a full-capacity 1000-block")
			.comment("greenhouse shifts 4 degC per second.")
			.translation("create_cultivation.config.climateRate")
			.defineInRange("climateRate", 1.0, 0.05, 20.0);

		BUILDER.pop()
			.push("scan")
			.comment("Greenhouse enclosure scanning.");

		MAX_GREENHOUSE_VOLUME = BUILDER
			.comment("Hard cap on the scanned greenhouse volume (interior + boundary + floor, in blocks). Scans exceeding it are invalid; the flood fill itself is unbounded, so this is purely a performance/memory guard. 2048 fits a ~12x12x14 enclosure.")
			.translation("create_cultivation.config.maxGreenhouseVolume")
			.defineInRange("maxGreenhouseVolume", 2048, 8, 32768);

		SCAN_INTERVAL_TICKS = BUILDER
			.comment("Ticks between enclosure re-scans while powered (20 = 1 second). Faster scans pick up greenhouse edits sooner at a small scan cost.")
			.translation("create_cultivation.config.scanIntervalTicks")
			.defineInRange("scanIntervalTicks", 100, 20, 1200);

		BUILDER.pop()
			.push("sprinkler")
			.comment("The sprinkler's farmland hydration coverage.");

		SPRINKLER_AREA_RADIUS = BUILDER
			.comment("Horizontal radius (in blocks) of the farmland area the sprinkler keeps hydrated. 3 = a 7x7 area.")
			.translation("create_cultivation.config.sprinklerAreaRadius")
			.defineInRange("areaRadius", 3, 1, 8);

		BUILDER.pop()
			.push("crop_boost")
			.comment("Multipliers for crops growing inside a powered greenhouse, decided")
			.comment("by the live climate vs the crop's configured ranges: both dimensions")
			.comment("optimal, exactly one optimal (other within survival), both merely")
			.comment("surviving (no bonus), or stalled (outside survival: no growth, no yield).");

		CLIMATE_OPTIMAL_YIELD = BUILDER
			.comment("Harvest yield multiplier while temperature AND humidity are both inside the crop's optimal range.")
			.translation("create_cultivation.config.climateOptimalYieldBonus")
			.defineInRange("climateOptimalYieldBonus", 3.0, 0.0, 64.0);

		CLIMATE_OPTIMAL_GROWTH = BUILDER
			.comment("Growth speed multiplier while temperature AND humidity are both inside the crop's optimal range.")
			.translation("create_cultivation.config.climateOptimalGrowthBonus")
			.defineInRange("climateOptimalGrowthBonus", 9.0, 0.0, 64.0);

		CLIMATE_SURVIVAL_YIELD = BUILDER
			.comment("Harvest yield multiplier while exactly one dimension is optimal and the other is inside the survival range.")
			.translation("create_cultivation.config.climateSurvivalYieldBonus")
			.defineInRange("climateSurvivalYieldBonus", 1.5, 0.0, 64.0);

		CLIMATE_SURVIVAL_GROWTH = BUILDER
			.comment("Growth speed multiplier while exactly one dimension is optimal and the other is inside the survival range.")
			.translation("create_cultivation.config.climateSurvivalGrowthBonus")
			.defineInRange("climateSurvivalGrowthBonus", 3.0, 0.0, 64.0);

		CLIMATE_SURVIVAL_ONLY_YIELD = BUILDER
			.comment("Harvest yield multiplier while both dimensions are inside the survival ranges but neither is optimal.")
			.translation("create_cultivation.config.climateSurvivalOnlyYieldBonus")
			.defineInRange("climateSurvivalOnlyYieldBonus", 1.0, 0.0, 64.0);

		CLIMATE_SURVIVAL_ONLY_GROWTH = BUILDER
			.comment("Growth speed multiplier while both dimensions are inside the survival ranges but neither is optimal.")
			.translation("create_cultivation.config.climateSurvivalOnlyGrowthBonus")
			.defineInRange("climateSurvivalOnlyGrowthBonus", 1.0, 0.0, 64.0);

		BUILDER.pop();

		BUILDER.push("ambient_crops")
			.comment("Optional climate effects on crops growing OUTSIDE greenhouses,")
			.comment("from the ambient biome temperature and humidity. The ladder is")
			.comment("the same as the greenhouse one; a greenhouse registration always")
			.comment("takes priority and shields its interior from these effects.");

		AMBIENT_CROPS_ENABLED = BUILDER
			.comment("Enable ambient climate effects on outdoor soil crops. Default: false (vanilla behaviour).")
			.translation("create_cultivation.config.ambientCropsEnabled")
			.define("ambientCropsEnabled", false);

		AMBIENT_RECHECK_INTERVAL_TICKS = BUILDER
			.comment("Ticks between outdoor crop climate re-checks (20 = 1 second). Each pass only visits already-tracked crops; lower values follow biome/season changes sooner.")
			.translation("create_cultivation.config.ambientRecheckIntervalTicks")
			.defineInRange("recheckIntervalTicks", 200, 20, 2400);

		AMBIENT_OPTIMAL_YIELD = BUILDER
			.comment("Outdoor harvest yield multiplier while temperature AND humidity are both inside the crop's optimal range.")
			.translation("create_cultivation.config.ambientOptimalYieldBonus")
			.defineInRange("ambientOptimalYieldBonus", 1.5, 0.0, 64.0);

		AMBIENT_OPTIMAL_GROWTH = BUILDER
			.comment("Outdoor growth speed multiplier while temperature AND humidity are both inside the crop's optimal range.")
			.translation("create_cultivation.config.ambientOptimalGrowthBonus")
			.defineInRange("ambientOptimalGrowthBonus", 2.0, 0.0, 64.0);

		AMBIENT_PARTIAL_YIELD = BUILDER
			.comment("Outdoor harvest yield multiplier while exactly one dimension is optimal and the other is inside the survival range.")
			.translation("create_cultivation.config.ambientPartialYieldBonus")
			.defineInRange("ambientPartialYieldBonus", 1.0, 0.0, 64.0);

		AMBIENT_PARTIAL_GROWTH = BUILDER
			.comment("Outdoor growth speed multiplier while exactly one dimension is optimal and the other is inside the survival range.")
			.translation("create_cultivation.config.ambientPartialGrowthBonus")
			.defineInRange("ambientPartialGrowthBonus", 1.5, 0.0, 64.0);

		AMBIENT_SURVIVAL_ONLY_YIELD = BUILDER
			.comment("Outdoor harvest yield multiplier while both dimensions are merely surviving. 1.0 = no bonus.")
			.translation("create_cultivation.config.ambientSurvivalOnlyYieldBonus")
			.defineInRange("ambientSurvivalOnlyYieldBonus", 1.0, 0.0, 64.0);

		AMBIENT_SURVIVAL_ONLY_GROWTH = BUILDER
			.comment("Outdoor growth speed multiplier while both dimensions are merely surviving. 1.0 = no bonus.")
			.translation("create_cultivation.config.ambientSurvivalOnlyGrowthBonus")
			.defineInRange("ambientSurvivalOnlyGrowthBonus", 1.0, 0.0, 64.0);

		BUILDER.pop();

		BUILDER.pop();

		BUILDER.push("stall_fertilizer_block")
			.comment("While a crop is stalled (outside both of its survival ranges) fertilizing")
			.comment("it has no effect anyway; this optionally rejects the item outright so")
			.comment("the stall becomes visible to the player instead of silently wasting");

		STALL_BLOCK_ENABLED = BUILDER
			.comment("Block fertilizer items on crops the climate system currently holds in the stalled state. Default: true.")
			.translation("create_cultivation.config.stallBlockEnabled")
			.define("stallBlockEnabled", true);

		STALL_BLOCKED_FERTILIZERS = BUILDER
			.comment("Item ids rejected on stalled crops while the feature is enabled (default: bone meal and efficient fertilizer). Unknown ids are ignored.")
			.translation("create_cultivation.config.stallBlockedFertilizers")
			.defineListAllowEmpty("blockedFertilizers", List.of("minecraft:bone_meal", "create_cultivation:efficient_fertilizer"),
				o -> o instanceof String s && !s.isBlank());

		BUILDER.pop();

		BUILDER.push("sereneseasons")
			.comment("Serene Seasons integration: while a season is active, the resolved")
			.comment("biome humidity is shifted by the season's offset (in %RH). Temperature")
			.comment("follows Serene Seasons' own biome_temp_adjustment. Non-whitelisted")
			.comment("dimensions and blacklisted biomes are never offset.");

		SS_SPRING_HUMIDITY_OFFSET = BUILDER
			.comment("Humidity offset during Spring.")
			.translation("create_cultivation.config.springHumidityOffset")
			.defineInRange("springHumidityOffset", 5.0, -100.0, 100.0);

		SS_SUMMER_HUMIDITY_OFFSET = BUILDER
			.comment("Humidity offset during Summer.")
			.translation("create_cultivation.config.summerHumidityOffset")
			.defineInRange("summerHumidityOffset", 10.0, -100.0, 100.0);

		SS_AUTUMN_HUMIDITY_OFFSET = BUILDER
			.comment("Humidity offset during Autumn.")
			.translation("create_cultivation.config.autumnHumidityOffset")
			.defineInRange("autumnHumidityOffset", 0.0, -100.0, 100.0);

		SS_WINTER_HUMIDITY_OFFSET = BUILDER
			.comment("Humidity offset during Winter.")
			.translation("create_cultivation.config.winterHumidityOffset")
			.defineInRange("winterHumidityOffset", -15.0, -100.0, 100.0);

		BUILDER.pop();

		SPEC = BUILDER.build();
	}

	private CCConfig() {
	}
}
