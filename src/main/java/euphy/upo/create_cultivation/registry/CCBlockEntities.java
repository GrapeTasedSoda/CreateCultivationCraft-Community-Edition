package euphy.upo.create_cultivation.registry;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import euphy.upo.create_cultivation.content.humidifier.HumidifierBlock;
import euphy.upo.create_cultivation.content.humidifier.HumidifierBlockEntity;
import euphy.upo.create_cultivation.content.dehumidifier.DehumidifierBlock;
import euphy.upo.create_cultivation.content.dehumidifier.DehumidifierBlockEntity;
import euphy.upo.create_cultivation.content.airconditioner.AirConditionerBlockEntity;
import euphy.upo.create_cultivation.content.sprinkler.SprinklerBlock;
import euphy.upo.create_cultivation.content.sprinkler.SprinklerBlockEntity;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseFluidPipeBlockEntity;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseGlassFluidPipeBlockEntity;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseBlockEntity;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import static euphy.upo.create_cultivation.CreateCultivationCraft.REGISTRATE;

public class CCBlockEntities {

    public static final BlockEntityEntry<CultivationBaseBlockEntity> CULTIVATION_BASE = REGISTRATE
            .blockEntity("cultivation_base", CultivationBaseBlockEntity::new)
            .validBlocks(CCBlocks.CULTIVATION_BASE)
            .register();

    public static final BlockEntityEntry<CultivationTankBlockEntity> CULTIVATION_TANK = REGISTRATE
            .blockEntity("cultivation_tank", CultivationTankBlockEntity::new)
            .validBlocks(CCBlocks.CULTIVATION_TANK)
            .register();

    public static final BlockEntityEntry<GreenhouseControllerBlockEntity> GREENHOUSE_CONTROLLER = REGISTRATE
            .blockEntity("greenhouse_controller", GreenhouseControllerBlockEntity::new)
            .validBlocks(CCBlocks.GREENHOUSE_CONTROLLER)
            .register();

    // Reuses Create's SlidingDoorBlockEntity + SlidingDoorRenderer verbatim -
    // the entity class is generic; only the valid-blocks set differs.
    public static final BlockEntityEntry<SlidingDoorBlockEntity> GREENHOUSE_SLIDING_DOOR = REGISTRATE
            .blockEntity("greenhouse_sliding_door", SlidingDoorBlockEntity::new)
            .renderer(() -> SlidingDoorRenderer::new)
            .validBlocks(CCBlocks.GREENHOUSE_GLASS_DOOR)
            .register();

    // Greenhouse-cased pipe: own BE subclass that restores normal-pipe
    // attachment rendering (extension segments + rims) which Create suppresses
    // for encased pipes - our glass shell makes the interior visible.
    public static final BlockEntityEntry<GreenhouseFluidPipeBlockEntity> GREENHOUSE_FLUID_PIPE = REGISTRATE
            .blockEntity("greenhouse_fluid_pipe", GreenhouseFluidPipeBlockEntity::new)
            .validBlocks(CCBlocks.GREENHOUSE_FLUID_PIPE)
            .register();

    // Greenhouse-cased window pipe. The fluid stream is drawn by a BER that we
    // register explicitly in CreateCultivationCraftClient: Registrate's deferred
    // .visual()/.renderer() client registration never fires for our mod, and
    // with no flywheel visualizer attached Flywheel leaves this BE to the
    // vanilla dispatcher, so the BER is authoritative.
    public static final BlockEntityEntry<GreenhouseGlassFluidPipeBlockEntity> GREENHOUSE_GLASS_FLUID_PIPE = REGISTRATE
            .blockEntity("greenhouse_glass_fluid_pipe", GreenhouseGlassFluidPipeBlockEntity::new)
            .validBlocks(CCBlocks.GREENHOUSE_GLASS_FLUID_PIPE)
            .register();

    public static final BlockEntityEntry<HumidifierBlockEntity> HUMIDIFIER = REGISTRATE
            .blockEntity("humidifier", HumidifierBlockEntity::new)
            .validBlocks(CCBlocks.HUMIDIFIER)
            .register();

    public static final BlockEntityEntry<DehumidifierBlockEntity> DEHUMIDIFIER = REGISTRATE
            .blockEntity("dehumidifier", DehumidifierBlockEntity::new)
            .validBlocks(CCBlocks.DEHUMIDIFIER)
            .register();

    public static final BlockEntityEntry<AirConditionerBlockEntity> AIR_CONDITIONER = REGISTRATE
            .blockEntity("airconditioner", AirConditionerBlockEntity::new)
            .validBlocks(CCBlocks.AIR_CONDITIONER)
            .register();

    public static final BlockEntityEntry<SprinklerBlockEntity> SPRINKLER = REGISTRATE
            .blockEntity("sprinkler", SprinklerBlockEntity::new)
            .validBlocks(CCBlocks.SPRINKLER)
            .register();

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CCBlockEntities.CULTIVATION_BASE.get(),
                (blockEntity, context) -> blockEntity.getAutomationHandler()
        );
        // Create pipes connect to any block exposing the fluid capability.
        // The humidifier's inlet is only the back plate (the face with the
        // inlet texture) = opposite of FACING; side == null is the internal
        // access used by later GUI/logic code and stays exposed.
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CCBlockEntities.HUMIDIFIER.get(),
                (blockEntity, side) -> {
                    if (side == null)
                        return blockEntity.fluidHandler();
                    BlockState state = blockEntity.getBlockState();
                    return side == state.getValue(HumidifierBlock.FACING).getOpposite()
                            ? blockEntity.fluidHandler()
                            : null;
                }
        );
        // The dehumidifier's water outlet is likewise only the back plate
        // (opposite of FACING) - external handlers may DRAIN from it, never
        // fill (topping the tank up would disable the machine).
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CCBlockEntities.DEHUMIDIFIER.get(),
                (blockEntity, side) -> {
                    if (side == null)
                        return blockEntity.fluidHandler();
                    BlockState state = blockEntity.getBlockState();
                    return side == state.getValue(DehumidifierBlock.FACING).getOpposite()
                            ? blockEntity.externalFluidHandler()
                            : null;
                }
        );
        // The sprinkler's fluid inlet is the model's bottom plate: the
        // upright form (FACING=UP) takes fluid on the block's DOWN face, the
        // hanging form (FACING=DOWN) is x-rotated so its inlet faces UP into
        // the ceiling. Create's pipe connection check queries exactly this
        // face, so a pipe butting against the inlet auto-connects.
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CCBlockEntities.SPRINKLER.get(),
                (blockEntity, side) -> {
                    if (side == null)
                        return blockEntity.fluidHandler();
                    BlockState state = blockEntity.getBlockState();
                    return side == state.getValue(SprinklerBlock.FACING).getOpposite()
                            ? blockEntity.fluidHandler()
                            : null;
                }
        );
    }

    public static void register() {}
}
