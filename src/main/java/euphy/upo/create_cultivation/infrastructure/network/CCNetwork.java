package euphy.upo.create_cultivation.infrastructure.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Mod-bus registration for all CC payloads (NeoForge 1.21 payload pattern).
 */
@EventBusSubscriber(modid = "create_cultivation", bus = EventBusSubscriber.Bus.MOD)
public final class CCNetwork {

    private CCNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(GreenhouseSnapshotPayload.TYPE, GreenhouseSnapshotPayload.STREAM_CODEC,
                GreenhouseClientPayloadHandler::handleSnapshot);
        registrar.playToServer(GreenhouseSetpointsPayload.TYPE, GreenhouseSetpointsPayload.STREAM_CODEC,
                GreenhouseServerPayloadHandler::handleSetpoints);

    }
}
