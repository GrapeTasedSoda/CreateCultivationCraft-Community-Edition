package euphy.upo.create_cultivation.registry;

import com.tterrag.registrate.util.entry.MenuEntry;
import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerMenu;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerScreen;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseMenu;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseScreen;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class CCMenuTypes {

    public static final MenuEntry<CultivationBaseMenu> CULTIVATION_BASE = CreateCultivationCraft.REGISTRATE
            .menu("cultivation_base", CultivationBaseMenu::new, () -> CultivationBaseScreen::new)
            .register();

    public static final MenuEntry<GreenhouseControllerMenu> GREENHOUSE_CONTROLLER = CreateCultivationCraft.REGISTRATE
            .menu("greenhouse_controller",
                    (MenuType<GreenhouseControllerMenu> type, int id, Inventory inv,
                            RegistryFriendlyByteBuf buf) -> new GreenhouseControllerMenu(type, id, inv, buf),
                    () -> GreenhouseControllerScreen::new)
            .register();

    public static void register() {
    }
}
