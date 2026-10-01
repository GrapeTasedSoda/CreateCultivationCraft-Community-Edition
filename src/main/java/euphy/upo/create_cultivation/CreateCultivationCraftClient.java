package euphy.upo.create_cultivation;

import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseRenderer;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorRenderer;
import com.simibubi.create.content.fluids.pipes.TransparentStraightPipeRenderer;
import euphy.upo.create_cultivation.content.airconditioner.AirConditionerBlastParticle;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerRenderer;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerScreen;
import euphy.upo.create_cultivation.content.humidifier.HumidifierMistParticle;
import euphy.upo.create_cultivation.content.sprinkler.SprinklerSprayParticle;
import euphy.upo.create_cultivation.ponder.CCPonderPlugin;
import euphy.upo.create_cultivation.registry.CCBlockEntities;
import euphy.upo.create_cultivation.registry.CCParticles;
import euphy.upo.create_cultivation.registry.CCPartialModels;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.particle.ParticleEngine;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankRenderer;
import euphy.upo.create_cultivation.content.dehumidifier.DehumidifierRenderer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = CreateCultivationCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateCultivationCraftClient {
    public CreateCultivationCraftClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // crop-list tooltips recolour their border while rendering on this
        // screen (RenderTooltipEvent.Color fires synchronously inside the
        // renderTooltip call, the screen marks exactly that call)
        NeoForge.EVENT_BUS.addListener(GreenhouseControllerScreen::onRenderTooltipColor);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CCPartialModels::init);
        PonderIndex.addPlugin(new CCPonderPlugin());
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CCParticles.HUMIDIFIER_MIST.get(), HumidifierMistParticle.Provider::new);
        event.registerSpriteSet(CCParticles.AIRCONDITIONER_BLAST.get(), AirConditionerBlastParticle.Provider::new);
        event.registerSpriteSet(CCParticles.SPRINKLER_SPRAY.get(), SprinklerSprayParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CCBlockEntities.CULTIVATION_BASE.get(), CultivationBaseRenderer::new);
        event.registerBlockEntityRenderer(CCBlockEntities.CULTIVATION_TANK.get(), CultivationTankRenderer::new);
        event.registerBlockEntityRenderer(CCBlockEntities.DEHUMIDIFIER.get(), DehumidifierRenderer::new);
        event.registerBlockEntityRenderer(CCBlockEntities.GREENHOUSE_CONTROLLER.get(), GreenhouseControllerRenderer::new);
        // Fluid stream rendering for the greenhouse-cased window pipe, registered
        // explicitly here (see CCBlockEntities note on why not via Registrate).
        event.registerBlockEntityRenderer(CCBlockEntities.GREENHOUSE_GLASS_FLUID_PIPE.get(),
                TransparentStraightPipeRenderer::new);
        // sliding doors hide their static model while open (visible=false) and
        // rely on this renderer to draw the sliding animation - without it the
        // open door turns completely invisible
        event.registerBlockEntityRenderer(CCBlockEntities.GREENHOUSE_SLIDING_DOOR.get(), SlidingDoorRenderer::new);
    }
}
