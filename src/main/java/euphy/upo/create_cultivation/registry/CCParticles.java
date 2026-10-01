package euphy.upo.create_cultivation.registry;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle types. The humidifier mist travels from the machine's hollow body
 * to the open head plane; the waypoint is passed through the particle's
 * (xSpeed, ySpeed, zSpeed) slots by HumidifierBlockEntity#clientTick, so a
 * plain SimpleParticleType is enough (no custom options codec needed).
 */
public class CCParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CreateCultivationCraft.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HUMIDIFIER_MIST =
            PARTICLE_TYPES.register("humidifier_mist", () -> new SimpleParticleType(false));

    /** Air conditioner vent blast: straight jet from the front, 22.5 degrees down. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AIRCONDITIONER_BLAST =
            PARTICLE_TYPES.register("airconditioner_blast", () -> new SimpleParticleType(false));

    /** Sprinkler spray: gravity water droplet arcing out of the 9px spray ring. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPRINKLER_SPRAY =
            PARTICLE_TYPES.register("sprinkler_spray", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
