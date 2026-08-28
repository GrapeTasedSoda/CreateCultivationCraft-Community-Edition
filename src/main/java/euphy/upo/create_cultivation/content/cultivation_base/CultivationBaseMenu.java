package euphy.upo.create_cultivation.content.cultivation_base;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Container menu for the Cultivation Base. Exposes the 8 internal output slots
 * (read-only for players, items may only be inserted by the harvest logic)
 * plus the player's inventory.
 */
public class CultivationBaseMenu extends AbstractContainerMenu {

    public static final int CONTAINER_SLOTS = CultivationBaseBlockEntity.SLOT_COUNT;

    private final IItemHandler itemHandler;
    private final BlockPos blockPos;

    /**
     * Client-side constructor, invoked by the networked menu type. The block pos
     * is written into the extra data buffer when the menu is opened.
     */
    public CultivationBaseMenu(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(type, containerId, playerInventory, extraData.readBlockPos());
    }

    private CultivationBaseMenu(MenuType<?> type, int containerId, Inventory playerInventory, BlockPos pos) {
        this(type, containerId, playerInventory, handlerAt(playerInventory, pos), pos);
    }

    /**
     * Server-side constructor, used by the block entity acting as MenuProvider.
     */
    public CultivationBaseMenu(MenuType<?> type, int containerId, Inventory playerInventory, CultivationBaseBlockEntity blockEntity) {
        this(type, containerId, playerInventory, blockEntity.getItemHandler(), blockEntity.getBlockPos());
    }

    private CultivationBaseMenu(MenuType<?> type, int containerId, Inventory playerInventory, IItemHandler itemHandler, BlockPos blockPos) {
        super(type, containerId);
        this.itemHandler = itemHandler;
        this.blockPos = blockPos;

        // 8 output slots in a 4x2 grid, aligned with the texture at (53,23)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new OutputSlot(itemHandler, col + row * 4, 53 + col * 18, 23 + row * 18));
            }
        }

        // Player inventory, aligned with the texture at (8,85)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 143));
        }
    }

    private static IItemHandler handlerAt(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CultivationBaseBlockEntity blockEntity) {
            return blockEntity.getItemHandler();
        }
        return new ItemStackHandler(CONTAINER_SLOTS);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(blockPos) instanceof CultivationBaseBlockEntity
                && blockPos.closerToCenterThan(player.position(), 8.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index < CONTAINER_SLOTS) {
            // Move harvested items from the base into the player's inventory
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Container slots are output-only, no inserting from the player inventory
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    /**
     * Output-only slot: players can view and take items but never insert them.
     */
    private static class OutputSlot extends SlotItemHandler {

        OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
