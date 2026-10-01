package euphy.upo.create_cultivation.registry;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * GUI sound effects. The setpoint click is one gear-detent "tick"; the
 * greenhouse controller screen re-triggers it per detent of drag travel with
 * a small pitch wobble, so the bar ratchets like a mechanical gear train.
 */
public class CCSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreateCultivationCraft.MODID);

    /** One gear-detent click while dragging a temperature/humidity setpoint. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_SETPOINT_CLICK =
            SOUND_EVENTS.register("ui.setpoint_click",
                    () -> SoundEvent.createVariableRangeEvent(CreateCultivationCraft.asResource("ui.setpoint_click")));

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
