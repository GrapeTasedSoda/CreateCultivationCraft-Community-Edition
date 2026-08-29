package euphy.upo.create_cultivation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * In-game configuration for Create: Cultivation Craft.
 *
 * <p>Registered as a NeoForge COMMON config so it is loaded on both the physical
 * client and the integrated/dedicated server, and so the values can be edited live
 * from the NeoForge config screen ("Mods &gt; create_cultivation &gt; Config") that
 * {@code CreateCultivationCraftClient} already wires up via
 * {@code IConfigScreenFactory/ConfigurationScreen}.</p>
 */
public final class CCConfig {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec SPEC;

	/** Scales how fast crops mature. 1.0 = vanilla speed of this addon. */
	public static final ModConfigSpec.DoubleValue GROWTH_RATE;

	/** Scales the amount of crops produced per harvest. */
	public static final ModConfigSpec.DoubleValue CROP_YIELD;

	/** Extra yield multiplier applied on top of {@link #CROP_YIELD} while the tank is watered. */
	public static final ModConfigSpec.DoubleValue WATERING_YIELD_BONUS;

	/** How long (in lazy ticks) a "watered" state lasts after a Spout waters the tank. */
	public static final ModConfigSpec.IntValue WATERED_DURATION;

	/** Extra growth speed multiplier while a valid catalyst is present in the base's catalyst slot. */
	public static final ModConfigSpec.DoubleValue CATALYST_GROWTH_BONUS;

	/** Extra multiplier applied to both growth speed and yield while the tank is watered AND the catalyst is active. */
	public static final ModConfigSpec.DoubleValue WATER_CATALYST_SYNERGY_BONUS;

	static {
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
			.comment("Extra yield multiplier applied on top of cropYieldMultiplier while the tank is watered. 2.5 means watered harvests produce 2.5x the crops; 1.0 disables the bonus.")
			.translation("create_cultivation.config.wateringYieldBonus")
			.defineInRange("wateringYieldBonus", 2.5, 1.0, 10.0);

		WATERED_DURATION = BUILDER
			.comment("How long the 'watered' state lasts (in lazy ticks, ~0.5s each; 20 = 10 seconds) after a Spout waters the tank.")
			.translation("create_cultivation.config.wateredDuration")
			.defineInRange("wateredDuration", 20, 1, 600);

		BUILDER.pop().push("catalyst")
			.comment("Settings for the Cultivation Base catalyst slot.");

		CATALYST_GROWTH_BONUS = BUILDER
			.comment("Growth speed multiplier applied while a valid catalyst is present in the base's catalyst slot. 3.0 means crops grow 3x as fast; 1.0 disables the bonus.")
			.translation("create_cultivation.config.catalystGrowthBonus")
			.defineInRange("catalystGrowthBonus", 3.0, 1.0, 10.0);

		BUILDER.pop().push("synergy")
			.comment("Bonus applied while watering and the catalyst are active at the same time.");

		WATER_CATALYST_SYNERGY_BONUS = BUILDER
			.comment("Extra multiplier applied to BOTH growth speed and harvest yield while the tank is watered AND the catalyst is active. 1.5 means 50% extra on top of the existing multipliers; 1.0 disables the synergy.")
			.translation("create_cultivation.config.waterCatalystSynergyBonus")
			.defineInRange("waterCatalystSynergyBonus", 1.5, 1.0, 10.0);

		BUILDER.pop();

		SPEC = BUILDER.build();
	}

	private CCConfig() {
	}
}
