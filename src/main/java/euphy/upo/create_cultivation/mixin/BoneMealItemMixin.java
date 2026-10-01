package euphy.upo.create_cultivation.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropTracker;

/**
 * Stall fertilizer block, item-aware path: every vanilla bone-meal style
 * growth (player use AND dispenser) funnels through
 * {@code BoneMealItem.growCrop}, which carries the item identity the crop
 * growth hook lacks. While the stall-block feature is enabled and the
 * position is stalled, blocked fertilizers fail without growing anything
 * and without consuming the item.
 */
@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {

    @Inject(method = "growCrop(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void create_cultivation$stallFertilizerBlock(ItemStack stack, Level level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!CCConfig.STALL_BLOCK_ENABLED.get()) {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean blocked = false;
        for (String entry : CCConfig.STALL_BLOCKED_FERTILIZERS.get()) {
            if (id.equals(entry.trim())) {
                blocked = true;
                break;
            }
        }
        if (!blocked) {
            return;
        }
        if (GreenhouseCropTracker.isStalled(serverLevel.dimension(), pos)) {
            cir.setReturnValue(false);
        }
    }
}
