package euphy.upo.create_cultivation.registry;

import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Advancement triggers for the mod's two "first activation" milestones: the
 * Cultivation Tank starting to run, and a Greenhouse Controller successfully
 * recognising a greenhouse.
 *
 * <p>The triggers are parameterless - the interesting condition is the firing
 * point itself (the working-state edge in the block entities), so the codec
 * accepts an empty conditions object and every instance matches.
 *
 * <p>Trigger types are vanilla-registry objects ({@code Registries.TRIGGER_TYPE})
 * and must go through a {@link DeferredRegister} attached to the mod event
 * bus: calling {@code CriteriaTriggers.register} during mod construction
 * crashes with "Registry is already frozen" on NeoForge - and even without
 * the crash it would register under the wrong {@code minecraft:} namespace,
 * so the advancement JSONs (referencing {@code create_cultivation:...})
 * could never match.
 */
public final class CCAdvancementTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, CreateCultivationCraft.MODID);

    public static final Supplier<MachineActivatedTrigger> ACTIVATE_CULTIVATION_TANK =
            TRIGGERS.register("activate_cultivation_tank", MachineActivatedTrigger::new);
    public static final Supplier<MachineActivatedTrigger> ACTIVATE_GREENHOUSE_CONTROLLER =
            TRIGGERS.register("activate_greenhouse_controller", MachineActivatedTrigger::new);

    /** Attaches the deferred register to the mod event bus (constructor call). */
    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }

    private CCAdvancementTriggers() {}

    /** Parameterless trigger: fires whenever a machine starts working. */
    public static class MachineActivatedTrigger extends SimpleCriterionTrigger<MachineActivatedTrigger.Instance> {

        /**
         * Awards the trigger to every player near the machine that just
         * started: the assembler may have walked away before it powers on
         * (redstone clock, funnel-fed tanks), so the players present at the
         * moment of activation get the credit. Each player is granted once
         * by the advancement system.
         */
        public void awardNearby(ServerLevel level, BlockPos pos) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 16 * 16) {
                    this.trigger(player, Instance::matches);
                }
            }
        }

        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public record Instance(Optional<ContextAwarePredicate> player)
                implements SimpleCriterionTrigger.SimpleInstance {

            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)
            ).apply(instance, Instance::new));

            public boolean matches() {
                return true;
            }
        }
    }
}
