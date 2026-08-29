package euphy.upo.create_cultivation.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseBlockEntity;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import euphy.upo.create_cultivation.registry.CCBlocks;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes describing the full gameplay loop of the cultivation machine:
 * assembly, planting, growth & harvest, catalyst boost, watering bonus, output
 * extraction, display links and tank stacking.
 */
public class CultivationScenes {

    /**
     * Main storyboard attached to both the base and the tank: demonstrates the
     * complete machine loop from assembly to harvest.
     */
    public static void cultivating(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cultivation", "栽培");
        scene.configureBasePlate(0, 0, 3);
        scene.showBasePlate();
        scene.idle(10);

        // Chapter 1: assembly. The base sits at (1,1,1) with the tank above it.
        ElementLink<WorldSectionElement> machine =
            scene.world().showIndependentSection(util.select().fromTo(1, 1, 1, 1, 2, 1), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
            .text("栽培机由栽培基座和栽培罐组成：把基座放在下方，作物种在栽培罐中。")
            .pointAt(Vec3.atCenterOf(util.grid().at(1, 1, 1)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);

        // Shaft + cogwheel power the base at 16 RPM.
        scene.world().showSection(util.select().fromTo(2, 1, 1, 2, 1, 1), Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().fromTo(1, 1, 1, 2, 1, 1), 16);
        scene.idle(20);
        scene.overlay().showText(70)
            .text("底座需要应力驱动；转速越高，作物长得越快（最高两倍速）。")
            .pointAt(Vec3.atCenterOf(util.grid().at(2, 1, 1)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(80);

        // Chapter 2: planting.
        BlockPos tankPos = util.grid().at(1, 2, 1);
        scene.overlay().showControls(util.vector().centerOf(1, 2, 1), Pointing.DOWN, 25)
            .rightClick()
            .withItem(Items.WHEAT_SEEDS.getDefaultInstance());
        scene.idle(10);
        scene.world().modifyBlockEntity(tankPos, CultivationTankBlockEntity.class,
            be -> be.plant(new ItemStack(Items.WHEAT_SEEDS)));
        scene.idle(20);
        scene.overlay().showText(70)
            .text("手持可种植物品右键栽培罐种下作物；空手右键可以取出种子。")
            .pointAt(Vec3.atCenterOf(tankPos))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(80);

        // Chapter 3: growth & auto-harvest. Push the ponder progress forward so
        // the crop visibly grows, then show the harvest items flowing out.
        scene.overlay().showText(70)
            .text("机器会自动照料作物直至成熟，然后自动收获。")
            .pointAt(Vec3.atCenterOf(tankPos))
            .placeNearTarget()
            .attachKeyFrame();
        for (int stage = 1; stage <= 9; stage++) {
            final int s = stage;
            scene.world().modifyBlockEntity(tankPos, CultivationTankBlockEntity.class,
                be -> be.setPonderProgress(s));
            scene.idle(10);
        }
        scene.idle(30);

        // Harvested items are stored in the base; extract with a funnel.
        scene.world().showSection(util.select().fromTo(0, 1, 1, 0, 1, 1), Direction.DOWN);
        scene.idle(10);
        BlockPos funnel = util.grid().at(0, 1, 1);
        Vec3 spawnPoint = util.vector().blockSurface(funnel, Direction.DOWN).add(0, -0.2, 0);
        scene.overlay().showText(70)
            .text("收获物存储在基座里，用漏斗或溜槽即可提取。")
            .pointAt(Vec3.atCenterOf(funnel))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(25);
        scene.world().flapFunnel(funnel, true);
        scene.world().createEntity((Level world) ->
            new ItemEntity(world, spawnPoint.x, spawnPoint.y, spawnPoint.z, new ItemStack(Items.WHEAT)));
        scene.idle(20);
        scene.world().flapFunnel(funnel, true);
        scene.world().createEntity((Level world) ->
            new ItemEntity(world, spawnPoint.x, spawnPoint.y, spawnPoint.z, new ItemStack(Items.WHEAT_SEEDS)));
        scene.idle(30);

        // Chapter 4: catalyst. Drop a bone-meal-fed catalyst highlight on the
        // base GUI-less flow: the base accepts bone meal while working.
        scene.overlay().showControls(util.vector().centerOf(1, 1, 1), Pointing.DOWN, 30)
            .rightClick()
            .withItem(Items.BONE_MEAL.getDefaultInstance());
        scene.idle(15);
        scene.world().modifyBlockEntity(util.grid().at(1, 1, 1), CultivationBaseBlockEntity.class,
            be -> be.getItemHandler().insertItem(CultivationBaseBlockEntity.CATALYST_SLOT, new ItemStack(Items.BONE_MEAL, 8), false));
        scene.idle(10);
        scene.overlay().showText(80)
            .text("在基座界面放入催化剂（默认骨粉）可让作物以3倍速生长，每30秒消耗一份。")
            .pointAt(Vec3.atCenterOf(util.grid().at(1, 1, 1)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);

        // Chapter 5: watering with a Spout.
        Selection spoutStack = util.select().fromTo(2, 3, 1, 2, 3, 2);
        Selection spoutIt = util.select().fromTo(2, 3, 1, 2, 3, 1);
        ElementLink<WorldSectionElement> spoutIn =
            scene.world().showIndependentSection(spoutStack, Direction.DOWN);
        scene.world().moveSection(spoutIn, util.vector().of(-1, 0, 0), 20);
        scene.idle(25);
        scene.world().modifyBlockEntityNBT(spoutIt, SpoutBlockEntity.class,
            nbt -> nbt.putInt("ProcessingTicks", 20));
        scene.idle(25);
        scene.world().modifyBlockEntityNBT(spoutIt, SpoutBlockEntity.class,
            nbt -> nbt.putInt("ProcessingTicks", 20));
        scene.idle(15);
        scene.overlay().showText(80)
            .text("用注液器浇灌作物，收获产量提升至2.5倍；催化剂与浇水同时生效时各项再加成50%%。")
            .pointAt(Vec3.atCenterOf(util.grid().at(2, 3, 1)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);
        scene.world().hideIndependentSection(spoutIn, Direction.UP);
        scene.idle(15);

        // Chapter 6: display link. Place two at runtime: one beside the tank
        // (facing away from it) and one beside the base (facing outward).
        // setBlock only writes the ponder world; the positions must also be
        // shown as a world section or the blocks exist but never render.
        BlockPos tankLinkPos = util.grid().at(0, 2, 1);
        BlockPos baseLinkPos = util.grid().at(1, 1, 0);
        scene.world().setBlock(tankLinkPos, AllBlocks.DISPLAY_LINK.getDefaultState()
            .setValue(DirectionalBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(baseLinkPos, AllBlocks.DISPLAY_LINK.getDefaultState()
            .setValue(DirectionalBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(util.select().position(0, 2, 1)
            .add(util.select().position(1, 1, 0)), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
            .text("用显示链接器可以读取作物种类、剩余时间、产物与倍率等信息。")
            .pointAt(Vec3.atCenterOf(tankLinkPos))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);

        // Chapter 7: stacking tanks for taller crops.
        Selection stackPrototype = util.select().position(1, 2, 0);
        ElementLink<WorldSectionElement> tank2 =
            scene.world().showIndependentSection(stackPrototype, Direction.DOWN);
        scene.world().moveSection(tank2, util.vector().of(0, 1, 1), 0);
        scene.idle(6);
        ElementLink<WorldSectionElement> tank3 =
            scene.world().showIndependentSection(stackPrototype, Direction.DOWN);
        scene.world().moveSection(tank3, util.vector().of(0, 2, 1), 0);
        scene.idle(6);
        scene.effects().indicateSuccess(util.grid().at(1, 3, 1));
        scene.effects().indicateSuccess(util.grid().at(1, 4, 1));
        scene.idle(15);
        scene.overlay().showText(80)
            .text("向上堆叠栽培罐可以增加种植高度，部分高株作物需要更高结构才能收获。")
            .pointAt(Vec3.atCenterOf(util.grid().at(1, 3, 1)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(120)
            .text("作物配方可用数据包自定义。")
            .independent();
        scene.markAsFinished();
    }
}
