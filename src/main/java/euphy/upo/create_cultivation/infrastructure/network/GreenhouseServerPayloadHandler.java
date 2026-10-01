package euphy.upo.create_cultivation.infrastructure.network;

import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlockEntity;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerMenu;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side handling of client payloads.
 */
public final class GreenhouseServerPayloadHandler {

    private GreenhouseServerPayloadHandler() {}

    public static void handleSetpoints(GreenhouseSetpointsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof GreenhouseControllerMenu menu
                    && menu.getBlockPos().equals(payload.pos())) {
                GreenhouseControllerBlockEntity be = menu.getBlockEntity();
                if (be != null) {
                    be.setSetpoints(payload.setTempC10() / 10f, payload.setHum10() / 10f,
                            payload.autoMode());
                }
            }
        });
    }
}
