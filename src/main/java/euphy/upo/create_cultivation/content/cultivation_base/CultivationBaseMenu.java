package euphy.upo.create_cultivation.content.cultivation_base;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for the Cultivation Base. Exposes the 8 internal output slots
 * (read-only for players, items may only be inserted by the harvest logic), the
 * catalyst slot (players may insert valid catalysts and take them out, hoppers
 * may only insert) plus the player's inventory.
 */
public class CultivationBaseMenu extends AbstractContainerMenu {

    public static final int CONTAINER_SLOTS = CultivationBaseBlockEntity.SLOT_COUNT;
    public static final int CATALYST_SLOT_INDEX = CultivationBaseBlockEntity.CATALYST_SLOT;

    private final IItemHandler itemHandler;
    private final BlockPos blockPos;
    /** Client-side mirror of the synced catalyst-active data slot. */
    private int catalystActiveValue = 0;
    /** Client-side mirror of the synced tank-watered data slot. */
    private int wateredValue = 0;

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
        this(type, containerId, playerInventory, blockEntity.getItemHandler(), blockEntity.getBlockPos(), blockEntity);
    }

    private CultivationBaseMenu(MenuType<?> type, int containerId, Inventory playerInventory, IItemHandler itemHandler, BlockPos blockPos) {
        this(type, containerId, playerInventory, itemHandler, blockPos, null);
    }

    private CultivationBaseMenu(MenuType<?> type, int containerId, Inventory playerInventory, IItemHandler itemHandler, BlockPos blockPos, @Nullable CultivationBaseBlockEntity blockEntity) {
        super(type, containerId);
        this.itemHandler = itemHandler;
        this.blockPos = blockPos;

        // 8 output slots in a 4x2 grid, aligned with the texture at (93,14)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new OutputSlot(itemHandler, col + row * 4, 93 + col * 18, 14 + row * 18));
            }
        }

        // Catalyst slot, aligned with the 16x16 item area inside the gold-framed
        // slot drawn at (18,21) in the texture: the 18x18 inner area starts at
        // (19,22) and has a 1px border, so the item itself sits at (20,23).
        addSlot(new CatalystSlot(itemHandler, CATALYST_SLOT_INDEX, 20, 23));

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

        // Synced flag: whether the catalyst slot is currently granting a boost.
        // Server side returns the live state from the block entity; the client
        // receives updates through the vanilla data-slot mechanism.
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity != null && blockEntity.isCatalystBoostActive() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                catalystActiveValue = value;
            }
        });

        // Synced flag: whether the cultivation tank above is currently watered.
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity != null && blockEntity.isTankWatered() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                wateredValue = value;
            }
        });
    }

    /** Whether the catalyst boost animation should be shown (client side). */
    public boolean isCatalystActive() {
        return catalystActiveValue != 0;
    }

    /** Whether the watering animation should be shown (client side). */
    public boolean isWatered() {
        return wateredValue != 0;
    }

    private static IItemHandler handlerAt(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CultivationBaseBlockEntity blockEntity) {
            return blockEntity.getItemHandler();
        }
        return new ItemStackHandler(CONTAINER_SLOTS + 1);
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
            if (!moveItemStackTo(stack, CATALYST_SLOT_INDEX + 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == CATALYST_SLOT_INDEX) {
            // Move catalyst back into the player's inventory
            if (!moveItemStackTo(stack, CATALYST_SLOT_INDEX + 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Only valid catalysts may be shift-clicked into the catalyst slot;
            // the output slots never accept items from the player inventory.
            if (!itemHandler.isItemValid(CATALYST_SLOT_INDEX, stack)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, CATALYST_SLOT_INDEX, CATALYST_SLOT_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
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

    /**
     * Catalyst slot: accepts only the catalyst configured for the planted crop
     * (validated by the block entity's handler). Players may take items out.
     */
    private static class CatalystSlot extends SlotItemHandler {

        CatalystSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
    }
}
