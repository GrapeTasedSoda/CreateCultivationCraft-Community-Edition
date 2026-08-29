package euphy.upo.create_cultivation.compat.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.tterrag.registrate.util.entry.RegistryEntry;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import net.minecraft.core.registries.Registries;

import static euphy.upo.create_cultivation.CreateCultivationCraft.REGISTRATE;

/**
 * Registers Create Display Link sources for this addon's block entities.
 */
public class CCDisplaySources {

	public static final RegistryEntry<DisplaySource, CultivationDisplaySource> CULTIVATION_CROP =
		REGISTRATE.displaySource("cultivation_crop", () -> new CultivationDisplaySource(CultivationDisplaySource.MODE_CROP))
			.onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
				DisplaySource.BY_BLOCK_ENTITY.add(CCBlockEntities.CULTIVATION_TANK.get(), source))
			.register();

	public static final RegistryEntry<DisplaySource, CultivationDisplaySource> CULTIVATION_TIME =
		REGISTRATE.displaySource("cultivation_time", () -> new CultivationDisplaySource(CultivationDisplaySource.MODE_TIME))
			.onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
				DisplaySource.BY_BLOCK_ENTITY.add(CCBlockEntities.CULTIVATION_TANK.get(), source))
			.register();

	public static final RegistryEntry<DisplaySource, BaseDisplaySource> BASE_OUTPUT =
		REGISTRATE.displaySource("base_output", () -> new BaseDisplaySource(BaseDisplaySource.MODE_OUTPUT))
			.onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
				DisplaySource.BY_BLOCK_ENTITY.add(CCBlockEntities.CULTIVATION_BASE.get(), source))
			.register();

	public static final RegistryEntry<DisplaySource, BaseDisplaySource> BASE_CATALYST =
		REGISTRATE.displaySource("base_catalyst", () -> new BaseDisplaySource(BaseDisplaySource.MODE_CATALYST))
			.onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
				DisplaySource.BY_BLOCK_ENTITY.add(CCBlockEntities.CULTIVATION_BASE.get(), source))
			.register();

	public static final RegistryEntry<DisplaySource, BaseDisplaySource> BASE_MULTIPLIERS =
		REGISTRATE.displaySource("base_multipliers", () -> new BaseDisplaySource(BaseDisplaySource.MODE_MULTIPLIERS))
			.onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
				DisplaySource.BY_BLOCK_ENTITY.add(CCBlockEntities.CULTIVATION_BASE.get(), source))
			.register();

	public static void register() {
	}
}
