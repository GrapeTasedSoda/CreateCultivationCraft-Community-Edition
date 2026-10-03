package euphy.upo.create_cultivation.content.greenhouse;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.config.CCConfig;

/**
 * Greenhouse climate effects on soil-planted crops, event side: the harvest
 * multiplier scales the drops a broken crop produces (0 = no drops at all).
 * Growth handling lives in {@code CropBlockGrowthMixin}.
 */
public final class GreenhouseCropBoostEvents {

    private GreenhouseCropBoostEvents() {}

    /**
     * Stall fertilizer block, interaction path: when the climate system holds
     * the targeted crop in the stalled state and the item is on the blocked
     * list, the use is rejected with a message and the item is not consumed.
     * (Bonemeal from a dispenser grows via growCrops and is caught by the
     * mixin instead.)
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!CCConfig.STALL_BLOCK_ENABLED.get() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack stack = event.getItemStack();
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
        if (!GreenhouseCropTracker.isStalled(serverLevel.dimension(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            player.displayClientMessage(
                    Component.translatable("create_cultivation.stall.fertilizer_blocked"), true);
        }
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getState().getBlockHolder().getData(CCDataMaps.CROP_CLIMATE) == null) {
            // not a climate crop: no climate yield - mirrors the growth path,
            // which boosts any data-mapped crop, not just vanilla CropBlocks
            return;
        }
        GreenhouseCropTracker.Boost boost =
                GreenhouseCropTracker.get(event.getLevel().dimension(), event.getPos());
        if (boost == null && event.getLevel() instanceof ServerLevel serverLevel) {
            boost = AmbientCropManager.resolveOutdoor(serverLevel, event.getPos(), event.getState());
        }
        if (boost == null || boost.yieldMultiplier() == 1.0) {
            return;
        }
        final double yieldMultiplier = boost.yieldMultiplier();
        event.getDrops().removeIf(item -> {
            int scaled = (int) Math.floor(item.getItem().getCount() * yieldMultiplier);
            if (scaled <= 0) {
                return true;
            }
            item.getItem().setCount(scaled);
            return false;
        });
    }
}
