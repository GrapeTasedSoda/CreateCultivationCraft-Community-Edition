package euphy.upo.create_cultivation.registry;

import com.tterrag.registrate.util.entry.MenuEntry;
import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseMenu;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseScreen;

public class CCMenuTypes {

    public static final MenuEntry<CultivationBaseMenu> CULTIVATION_BASE = CreateCultivationCraft.REGISTRATE
            .menu("cultivation_base", CultivationBaseMenu::new, () -> CultivationBaseScreen::new)
            .register();

    public static void register() {
    }
}
