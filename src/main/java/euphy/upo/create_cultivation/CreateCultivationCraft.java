package euphy.upo.create_cultivation;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper;
import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.greenhouse.AmbientCropManager;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropBoostEvents;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropTracker;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseGlassEncasingHandler;
import euphy.upo.create_cultivation.compat.display.CCDisplaySources;
import euphy.upo.create_cultivation.compat.eclipticseasons.EclipticSeasonsCompat;
import euphy.upo.create_cultivation.compat.sereneseasons.SereneSeasonsCompat;
import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.config.CCConfigMigrations;
import euphy.upo.create_cultivation.datagen.DataGenerators;
import euphy.upo.create_cultivation.registry.*;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;


@Mod(CreateCultivationCraft.MODID)
public class CreateCultivationCraft {
    public static final String MODID = "create_cultivation";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            // Create-style item tooltips (shift-gated summary/behaviour lines): any
            // item with a lang file at <id>.tooltip.summary gets the full treatment,
            // items without the key are untouched
            .setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.GRAY_AND_BLUE))
            .defaultCreativeTab(CCCreativeModeTabs.MAIN_TAB.getKey());
    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
    public CreateCultivationCraft(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, CCConfig.SPEC);
        // config migration: upgrade old config files in place (version stamp +
        // key moves) while preserving the user's modified values
        modEventBus.addListener(CCConfigMigrations::onConfigLoad);
        CCBlocks.register();
        CCItems.register();
        CCBlockEntities.register();
        CCMenuTypes.register();
        CCAdvancementTriggers.register();
        CCParticles.register(modEventBus);
        CCSounds.register(modEventBus);
        CCCreativeModeTabs.register(modEventBus);
        CCRecipes.register(modEventBus);
        REGISTRATE.registerEventListeners(modEventBus);
        CCDisplaySources.register();
        modEventBus.addListener(CCDataMaps::onRegisterDataMapTypes);
        NeoForge.EVENT_BUS.addListener(GreenhouseGlassEncasingHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(GreenhouseCropBoostEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(GreenhouseCropBoostEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(AmbientCropManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        modEventBus.addListener(DataGenerators::gatherData);
        modEventBus.addListener(CCBlockEntities::registerCapabilities);

        // Serene Seasons integration: the compat feeds the season-adjusted
        // biome climate into the resolver hooks (temperature follows SS's own
        // biome_temp_adjustment; the humidity offset is ours, per season).
        if (ModList.get().isLoaded("sereneseasons")) {
            SereneSeasonsCompat.init();
        }

        // Ecliptic Seasons integration (24 solar terms, ships its own
        // temperature + humidity system). Registered after the SS block: if
        // both season mods are installed the solar-term hooks win.
        if (ModList.get().isLoaded("eclipticseasons")) {
            EclipticSeasonsCompat.init();
        }

        // Client bootstrap: @EventBusSubscriber only registers the client
        // class's STATIC @SubscribeEvent methods - it never constructs the
        // class, so its constructor (the config-screen extension point and
        // the tooltip-border listener) would stay dead code. Construct it
        // here on the client only; the dist guard keeps the client-only
        // class unloaded on a dedicated server.
        if (FMLEnvironment.dist.isClient()) {
            new CreateCultivationCraftClient(modContainer);
        }
    }
    private void commonSetup(FMLCommonSetupEvent event) {

        event.enqueueWork(CCStress::registerAllStressValues);
    }

    /**
     * The crop trackers and client snapshot caches are static, memory-only
     * maps keyed by dimension + position. They must be wiped when the server
     * stops, otherwise a second world joined in the same JVM session would
     * inherit the previous world's greenhouse/ambient registrations.
     */
    private void onServerStopped(ServerStoppedEvent event) {
        GreenhouseCropTracker.clearAllServerState();
        AmbientCropManager.clearAllServerState();
    }
}
