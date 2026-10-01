package euphy.upo.create_cultivation.infrastructure.network;

import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerMenu;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropTracker;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side handling of server payloads.
 */
public final class GreenhouseClientPayloadHandler {

    private GreenhouseClientPayloadHandler() {}

    public static void handleSnapshot(GreenhouseSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            GreenhouseControllerMenu.handleSnapshotOnClient(payload);
            // Jade cache: fold the per-crop boosts the snapshot piggybacks.
            // The client resolves the dimension through its player because
            // the payload only carries positions.
            if (context.player() != null) {
                GreenhouseCropTracker.applySnapshotClient(
                        context.player().level().dimension(),
                        payload.pos(),
                        java.util.Arrays.asList(payload.boosts()),
                        context.player().level().getGameTime());
            }
        });
    }
}
