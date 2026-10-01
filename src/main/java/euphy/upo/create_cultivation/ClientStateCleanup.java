package euphy.upo.create_cultivation;

import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerMenu;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropTracker;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Client-side static caches must not leak across worlds: the controller
 * snapshot cache is keyed by block position, so a stale entry from a
 * previous world would briefly paint a same-position controller in the next
 * world with the wrong data until the next broadcast arrives.
 */
@EventBusSubscriber(modid = CreateCultivationCraft.MODID, value = Dist.CLIENT)
public final class ClientStateCleanup {

    private ClientStateCleanup() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GreenhouseControllerMenu.clearClientSnapshots();
        GreenhouseCropTracker.clearClientState();
    }
}
