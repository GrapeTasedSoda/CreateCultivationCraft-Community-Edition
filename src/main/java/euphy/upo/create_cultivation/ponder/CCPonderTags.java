package euphy.upo.create_cultivation.ponder;

import com.tterrag.registrate.util.entry.RegistryEntry;
import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.registry.CCBlocks;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import static com.simibubi.create.infrastructure.ponder.AllCreatePonderTags.KINETIC_APPLIANCES;

public class CCPonderTags {

    /** Index page for the greenhouse module. */
    public static final ResourceLocation GREENHOUSE_TAG = CreateCultivationCraft.asResource("greenhouse");

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<RegistryEntry<?, ?>> entryHelper = helper.withKeyFunction(RegistryEntry::getId);

        entryHelper.addToTag(KINETIC_APPLIANCES)
                .add(CCBlocks.CULTIVATION_TANK)
                .add(CCBlocks.CULTIVATION_BASE);

        helper.registerTag(GREENHOUSE_TAG)
                .addToIndex()
                .item(CCBlocks.GREENHOUSE_CONTROLLER.get(), true, false)
                .title("Greenhouse Climate")
                .description("Build a greenhouse, control its temperature and humidity and boost your crops")
                .register();

        entryHelper.addToTag(GREENHOUSE_TAG)
                .add(CCBlocks.GREENHOUSE_CONTROLLER)
                .add(CCBlocks.GREENHOUSE_GLASS)
                .add(CCBlocks.AIR_CONDITIONER)
                .add(CCBlocks.HUMIDIFIER)
                .add(CCBlocks.DEHUMIDIFIER);
    }
}
