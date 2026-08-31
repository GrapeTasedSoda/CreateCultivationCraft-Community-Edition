package euphy.upo.create_cultivation.mixin;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.fluids.spout.SpoutRenderer;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create's Spout hard-codes its dripping fluid column to 1.2 blocks deep,
 * which looks wrong above multi-block cultivation tank stacks (the drip
 * vanishes mid-air instead of reaching the base). This redirect extends the
 * drip depth to the full tank stack height, keeping vanilla's proportion of
 * "one block + 0.2 penetration" per stack level so the column always reaches
 * the cultivation base top.
 */
@Mixin(value = SpoutRenderer.class, remap = false)
public abstract class SpoutRendererMixin {

    @Unique
    private static final int CC_MAX_DRIP_DEPTH = 8;

    /** The spout currently being rendered (renderSafe runs sequentially per BE on the render thread). */
    @Unique
    private static SpoutBlockEntity cc$currentSpout;

    @Unique
    private static int cc$getTankStackDepth(SpoutBlockEntity be) {
        Level level = be.getLevel();
        if (level == null) {
            return 0;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                be.getBlockPos().getX(), be.getBlockPos().getY(), be.getBlockPos().getZ());
        int depth = 0;
        while (depth < CC_MAX_DRIP_DEPTH) {
            cursor.move(Direction.DOWN);
            if (level.getBlockEntity(cursor) instanceof CultivationTankBlockEntity) {
                depth++;
            } else {
                break;
            }
        }
        return depth;
    }

    @Inject(method = "renderSafe", at = @At("HEAD"))
    private void cc$captureSpout(SpoutBlockEntity be, float partialTicks, com.mojang.blaze3d.vertex.PoseStack ms,
                                 net.minecraft.client.renderer.MultiBufferSource buffer, int light, int overlay,
                                 CallbackInfo ci) {
        cc$currentSpout = be;
    }

    @Redirect(
            method = "renderSafe",
            at = @At(value = "NEW", target = "Lnet/minecraft/world/phys/AABB;", remap = false)
    )
    private static AABB cc$extendDripToTankStack(double minX, double minY, double minZ,
                                                 double maxX, double maxY, double maxZ) {
        SpoutBlockEntity be = cc$currentSpout;
        int depth = be == null ? 0 : cc$getTankStackDepth(be);
        if (depth < 1) {
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
        // Vanilla uses 1.2 for a single block below (1 + 0.2 penetration);
        // scale the same way so the column always reaches the base top.
        return new AABB(minX, minY, minZ, maxX, -(depth + 0.2), maxZ);
    }
}
