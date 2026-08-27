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

	public static final RegistryEntry<DisplaySource, CultivationDisplaySource> CULTIVATION =
		REGISTRATE.displaySource("cultivation", CultivationDisplaySource::new)
			.onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
				DisplaySource.BY_BLOCK_ENTITY.add(CCBlockEntities.CULTIVATION_TANK.get(), source))
			.register();

	public static void register() {
	}
}
