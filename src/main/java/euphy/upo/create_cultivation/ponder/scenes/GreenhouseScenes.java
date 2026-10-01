package euphy.upo.create_cultivation.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import euphy.upo.create_cultivation.content.airconditioner.AirConditionerBlock;
import euphy.upo.create_cultivation.content.dehumidifier.DehumidifierBlock;
import euphy.upo.create_cultivation.content.humidifier.HumidifierBlock;
import euphy.upo.create_cultivation.registry.CCBlocks;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the greenhouse module: assembly, the glass enclosure,
 * the three climate devices, setpoints & auto mode, the crop boost ladder,
 * display links and the optional ambient climate system.
 *
 * <p>The shell, controller, power train and two crops live in
 * {@code ponder/greenhouse.nbt} (no block-entity NBT - the fake world
 * constructs them fresh, avoiding the stale-inventory slot crash the
 * cultivation structure once had). Devices and display links are placed at
 * runtime with {@code setBlock + showSection}, the same pattern the
 * cultivation scene uses. The thirteen {@code showText} calls map to the
 * auto-generated keys {@code create_cultivation.ponder.greenhouse.text_1..13}
 * in order of appearance - keep the lang files in sync when reordering.
 */
public class GreenhouseScenes {

    public static void greenhouse(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("greenhouse", "Greenhouse");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        // --- Chapter 1: assembly & power -----------------------------------
        scene.world().showSection(util.select().fromTo(3, 1, 3, 4, 1, 3), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
            .text("温室控制器是整个温室的大脑：接上动力后，它会扫描温室内部并自动调节气候。")
            .pointAt(Vec3.atCenterOf(util.grid().at(3, 1, 3)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);
        scene.world().setKineticSpeed(util.select().position(3, 1, 3), 16);
        // meshing reverses the rotation
        scene.world().setKineticSpeed(util.select().position(4, 1, 3), -16);
        scene.idle(15);
        scene.overlay().showText(70)
            .text("手持任意物品右键控制器，即可打开控制界面。")
            .pointAt(Vec3.atCenterOf(util.grid().at(3, 1, 3)))
            .placeNearTarget();
        scene.overlay().showControls(Vec3.atCenterOf(util.grid().at(3, 1, 3)), Pointing.DOWN, 30)
            .rightClick();
        scene.idle(80);

        // --- Chapter 2: the glass enclosure --------------------------------
        Selection shell = util.select().fromTo(0, 1, 0, 6, 4, 0)
            .add(util.select().fromTo(0, 1, 6, 6, 4, 6))
            .add(util.select().fromTo(0, 1, 1, 0, 4, 5))
            .add(util.select().fromTo(6, 1, 1, 6, 4, 5))
            .add(util.select().fromTo(1, 4, 1, 5, 4, 5));
        scene.world().showSection(shell, Direction.DOWN);
        scene.idle(20);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 2, 2, 2), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 1, 2, 4, 2, 2), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
            .text("用温室玻璃把控制器围起来：控制器会从自身所在格向外泛洪，识别温室的内部空间。")
            .pointAt(Vec3.atCenterOf(util.grid().at(3, 3, 3)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
            .text("耕地和作物同样算入内部空间；温室越大，把气候推到设定值所需的设备功率就越多。")
            .pointAt(Vec3.atCenterOf(util.grid().at(2, 1, 2)))
            .placeNearTarget();
        scene.idle(90);

        // --- Chapter 3: the three climate devices --------------------------
        scene.world().setBlock(util.grid().at(1, 1, 3),
            CCBlocks.AIR_CONDITIONER.get().defaultBlockState()
                .setValue(AirConditionerBlock.FACING, Direction.EAST)
                .setValue(AirConditionerBlock.ACTIVE, true),
            false);
        scene.world().showSection(util.select().position(1, 1, 3), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70)
            .text("温室的温湿度由三台设备调节：空气调节器负责温度。")
            .pointAt(Vec3.atCenterOf(util.grid().at(1, 1, 3)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(80);

        scene.world().setBlock(util.grid().at(5, 1, 3),
            CCBlocks.HUMIDIFIER.get().defaultBlockState()
                .setValue(HumidifierBlock.FACING, Direction.WEST)
                .setValue(HumidifierBlock.ACTIVE, true),
            false);
        scene.world().showSection(util.select().position(5, 1, 3), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70)
            .text("加湿器负责升高湿度，会从背板进水口持续消耗水。")
            .pointAt(Vec3.atCenterOf(util.grid().at(5, 1, 3)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(80);

        scene.world().setBlock(util.grid().at(3, 1, 1),
            CCBlocks.DEHUMIDIFIER.get().defaultBlockState()
                .setValue(DehumidifierBlock.FACING, Direction.SOUTH)
                .setValue(DehumidifierBlock.ACTIVE, true),
            false);
        scene.world().showSection(util.select().position(3, 1, 1), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70)
            .text("除湿器负责降低湿度，冷凝出的水可从背板出水口抽出。")
            .pointAt(Vec3.atCenterOf(util.grid().at(3, 1, 1)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(80);

        // --- Chapter 4: setpoints & auto mode ------------------------------
        scene.overlay().showText(90)
            .text("在控制界面拖动指针设定目标温湿度，设备会把实时气候推向设定值；黄灯常亮说明设备不足以达标。")
            .pointAt(Vec3.atCenterOf(util.grid().at(3, 1, 3)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
            .text("两个拨动开关是自动模式：按温室内作物的适宜区间和株数，自动把设定值调到最优。")
            .pointAt(Vec3.atCenterOf(util.grid().at(3, 1, 3)))
            .placeNearTarget();
        scene.idle(90);

        // --- Chapter 5: the crop boost ladder ------------------------------
        scene.overlay().showText(90)
            .text("作物各有各的区间：温湿度都在适宜区间时，产量3倍、生长速度9倍；只有一项适宜时为1.5倍和3倍。")
            .pointAt(Vec3.atCenterOf(util.grid().at(2, 2, 2)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(30);
        for (int age = 3; age <= 7; age++) {
            scene.idle(12);
            BlockState growing = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age);
            scene.world().setBlock(util.grid().at(2, 2, 2), growing, false);
        }
        scene.idle(20);
        scene.overlay().showText(90)
            .text("两项都只处于生存区间则无加成但能生长；任一项跌出生存区间，作物彻底停滞——用Jade对准作物即可看到状态。")
            .pointAt(Vec3.atCenterOf(util.grid().at(4, 2, 2)))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);

        // --- Chapter 6: display links (kept to the end) --------------------
        BlockPos linkA = util.grid().at(2, 1, 3);
        BlockPos linkB = util.grid().at(4, 1, 4);
        scene.world().setBlock(linkA, AllBlocks.DISPLAY_LINK.getDefaultState()
            .setValue(DirectionalBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(linkB, AllBlocks.DISPLAY_LINK.getDefaultState()
            .setValue(DirectionalBlock.FACING, Direction.EAST), false);
        scene.world().showSection(util.select().position(2, 1, 3)
            .add(util.select().position(4, 1, 3)), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
            .text("用显示链接器读取控制器：实时气候、设备统计，以及带适宜区间的作物清单。")
            .pointAt(Vec3.atCenterOf(linkA))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);

        // --- Chapter 7: ambient climate (outdoor crops) --------------------
        scene.overlay().showText(90)
            .text("温室外的作物则受生物群系环境气候影响，默认关闭，可在配置中开启。")
            .pointAt(Vec3.atCenterOf(util.grid().at(0, 2, 3)))
            .placeNearTarget();
        scene.idle(100);

        scene.markAsFinished();
    }
}
