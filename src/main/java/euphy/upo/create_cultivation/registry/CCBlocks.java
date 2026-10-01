package euphy.upo.create_cultivation.registry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import euphy.upo.create_cultivation.content.greenhouse.GreenhousePipeAttachmentModel;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.CreateRegistrate;
import static com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.interactionBehaviour;
import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import net.minecraft.client.renderer.RenderType;
import com.tterrag.registrate.util.entry.BlockEntry;
import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlock;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankCTBehaviour;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlock;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseGlassBlock;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseFluidPipeBlock;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseGlassCTBehaviour;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseGlassFluidPipeBlock;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseSlidingDoorBlock;
import euphy.upo.create_cultivation.content.humidifier.HumidifierBlock;
import euphy.upo.create_cultivation.content.dehumidifier.DehumidifierBlock;
import euphy.upo.create_cultivation.content.airconditioner.AirConditionerBlock;
import euphy.upo.create_cultivation.content.sprinkler.SprinklerBlock;
import net.minecraft.world.level.block.SoundType;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseBlock ;
import net.minecraft.world.level.material.MapColor;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;
import static euphy.upo.create_cultivation.CreateCultivationCraft.REGISTRATE;

public class CCBlocks {

    public static final CTSpriteShiftEntry CULTIVATION_TANK_SHIFT = new CTSpriteShiftEntry(AllCTTypes.VERTICAL);

    public static final CTSpriteShiftEntry GREENHOUSE_GLASS_SHIFT = new CTSpriteShiftEntry(AllCTTypes.VERTICAL);

    static {
        CULTIVATION_TANK_SHIFT.set(
                CreateCultivationCraft.asResource("block/cultivation_tank"),
                CreateCultivationCraft.asResource("block/cultivation_tank_connected")
        );
        GREENHOUSE_GLASS_SHIFT.set(
                CreateCultivationCraft.asResource("block/greenhouse_glass"),
                CreateCultivationCraft.asResource("block/greenhouse_glass_connected")
        );
    }

    public static final BlockEntry<CultivationTankBlock> CULTIVATION_TANK = REGISTRATE.block("cultivation_tank", CultivationTankBlock::new)
            .onRegister(connectedTextures(CultivationTankCTBehaviour::new))
            .properties(p -> p
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.METAL)
                    .strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> {
            })
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("item/cultivation_tank_alt")))
            .build()
            .register();

    public static final BlockEntry<CultivationBaseBlock> CULTIVATION_BASE = REGISTRATE.block("cultivation_base", CultivationBaseBlock::new)
            .properties(p -> p
                    .mapColor(MapColor.PODZOL)
                    .sound(SoundType.STONE)
                    .strength(1.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> prov.simpleBlock(
                    ctx.getEntry(),
                    prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName()))
            ))
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("item/cultivation_base_full")))
            .build()
            .register();


    public static final BlockEntry<GreenhouseControllerBlock> GREENHOUSE_CONTROLLER = REGISTRATE.block("greenhouse_controller", GreenhouseControllerBlock::new)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.STONE)
                    .strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> {
            })
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("item/greenhouse_controller")))
            .build()
            .register();


    public static final BlockEntry<GreenhouseGlassBlock> GREENHOUSE_GLASS = REGISTRATE.block("greenhouse_glass", GreenhouseGlassBlock::new)
            .onRegister(connectedTextures(() -> new GreenhouseGlassCTBehaviour(GREENHOUSE_GLASS_SHIFT)))
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.GLASS)
                    .strength(0.3f, 6.0f)
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> prov.simpleBlock(
                    ctx.getEntry(),
                    prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName()))
            ))
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
            .build()
            .register();


    public static final BlockEntry<GreenhouseSlidingDoorBlock> GREENHOUSE_GLASS_DOOR = REGISTRATE.block("greenhouse_glass_door", GreenhouseSlidingDoorBlock::new)
            // behaviour parity with Create's BuilderTransformers.slidingDoor:
            .addLayer(() -> RenderType::cutoutMipped)
            .onRegister(interactionBehaviour(new DoorMovingInteraction()))
            .onRegister(movementBehaviour(new SlidingDoorMovementBehaviour()))
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.GLASS)
                    .strength(0.3f, 6.0f)
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> {
            })
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), "minecraft:item/generated")
                        .texture("layer0", prov.modLoc("item/" + ctx.getName())))
            .build()
            .register();


    public static final BlockEntry<GreenhouseFluidPipeBlock> GREENHOUSE_FLUID_PIPE = REGISTRATE.block("greenhouse_fluid_pipe",
            p -> new GreenhouseFluidPipeBlock(p, () -> GREENHOUSE_GLASS.get()))
            // behaviour parity with create's ENCASED_FLUID_PIPE registration
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.GLASS)
                    .strength(0.5f, 6.0f)
                    .noOcclusion()
            )
            .transform(pickaxeOnly())
            // Frozen visuals: the attachment-rim model reads the frozen
            // connection snapshot (see GreenhousePipeAttachmentModel), the
            // core multipart reads frozen_* keys (generated by
            // models_workshop/install_greenhouse_fluid_pipe.py).
            .onRegister(CreateRegistrate.blockModel(() -> GreenhousePipeAttachmentModel::withoutAO))
            // Glass shell panels share the greenhouse glass CT family
            // (GreenhouseGlassCTBehaviour): shared edges drop the frame,
            // exposed edges keep it. Chained after the pipe attachment wrapper
            // (CustomBlockModels andThen-composes registrations).
            .onRegister(connectedTextures(() -> new GreenhouseGlassCTBehaviour(GREENHOUSE_GLASS_SHIFT)))
            .blockstate((ctx, prov) -> {
            })
            .loot((p, b) -> p.dropOther(b, AllBlocks.FLUID_PIPE.get()))
            .transform(EncasingRegistry.addVariantTo(AllBlocks.FLUID_PIPE))
            .register();

    public static final BlockEntry<GreenhouseGlassFluidPipeBlock> GREENHOUSE_GLASS_FLUID_PIPE = REGISTRATE.block("greenhouse_glass_fluid_pipe",
            GreenhouseGlassFluidPipeBlock::new)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.GLASS)
                    .strength(0.5f, 6.0f)
                    .noOcclusion()
            )
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(pickaxeOnly())
            .onRegister(CreateRegistrate.blockModel(() -> GreenhousePipeAttachmentModel::withoutAO))
            .onRegister(connectedTextures(() -> new GreenhouseGlassCTBehaviour(GREENHOUSE_GLASS_SHIFT)))
            .blockstate((ctx, prov) -> {
            })
            .loot((p, b) -> p.dropOther(b, AllBlocks.FLUID_PIPE.get()))
            .register();


    public static final BlockEntry<HumidifierBlock> HUMIDIFIER = REGISTRATE.block("humidifier", HumidifierBlock::new)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.METAL)
                    .strength(2.0f, 6.0f)
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> {
            })
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
            .build()
            .register();

    public static final BlockEntry<DehumidifierBlock> DEHUMIDIFIER = REGISTRATE.block("dehumidifier", DehumidifierBlock::new)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.METAL)
                    .strength(2.0f, 6.0f)
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> {
            })
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
            .build()
            .register();

    public static final BlockEntry<AirConditionerBlock> AIR_CONDITIONER = REGISTRATE.block("airconditioner", AirConditionerBlock::new)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.METAL)
                    .strength(2.0f, 6.0f)
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> {
            })
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
            .build()
            .register();

    // Vertical-only sprinkler: no facing property; blockstate hand-written by
    // models_workshop/install_sprinkler.py (active=true/false variants).
    public static final BlockEntry<SprinklerBlock> SPRINKLER = REGISTRATE.block("sprinkler", SprinklerBlock::new)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.METAL)
                    .strength(1.0f, 6.0f)
                    .noOcclusion()
            )
            .blockstate((ctx, prov) -> {
            })
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
            .build()
            .register();

    public static void register() {}
}