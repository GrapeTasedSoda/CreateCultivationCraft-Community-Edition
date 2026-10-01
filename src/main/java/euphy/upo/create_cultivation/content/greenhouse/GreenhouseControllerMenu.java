package euphy.upo.create_cultivation.content.greenhouse;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import euphy.upo.create_cultivation.infrastructure.network.GreenhouseSnapshotPayload;
import euphy.upo.create_cultivation.registry.CCMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.MenuType;

/**
 * Slot-less menu for the greenhouse controller GUI: all data reaches the
 * client through {@link GreenhouseSnapshotPayload} pushes, not through slots
 * or data slots (the GUI has no inventory per the design).
 */
public class GreenhouseControllerMenu extends AbstractContainerMenu {

    /** Per-controller client snapshots, matched by block position. */
    private static final Map<BlockPos, GreenhouseSnapshotPayload> clientSnapshots = new HashMap<>();

    private final BlockPos blockPos;
    @Nullable
    private final GreenhouseControllerBlockEntity blockEntity;

    /** Client constructor (network factory). */
    public GreenhouseControllerMenu(MenuType<?> type, int containerId, Inventory playerInventory,
            RegistryFriendlyByteBuf extraData) {
        this(type, containerId, playerInventory, extraData.readBlockPos(), null);
    }

    /** Server constructor (MenuProvider). */
    public GreenhouseControllerMenu(int containerId, Inventory playerInventory,
            GreenhouseControllerBlockEntity blockEntity) {
        this(CCMenuTypes.GREENHOUSE_CONTROLLER.get(), containerId, playerInventory,
                blockEntity.getBlockPos(), blockEntity);
    }

    private GreenhouseControllerMenu(MenuType<?> type, int containerId, Inventory playerInventory,
            BlockPos blockPos, @Nullable GreenhouseControllerBlockEntity blockEntity) {
        super(type, containerId);
        this.blockPos = blockPos;
        this.blockEntity = blockEntity;
        // Initial snapshot straight to the opening player (tracker updates
        // follow on every scan / setpoint change).
        if (blockEntity != null && playerInventory.player.level().isClientSide() == false
                && playerInventory.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            blockEntity.sendSnapshotToPlayer(serverPlayer);
        }
    }

    /** Client payload sink (called on the client thread). */
    public static void handleSnapshotOnClient(GreenhouseSnapshotPayload payload) {
        clientSnapshots.put(payload.pos(), payload);
    }

    /** Client only: drops all cached snapshots (disconnect / world switch). */
    public static void clearClientSnapshots() {
        clientSnapshots.clear();
    }

    /** Snapshot of the controller this menu belongs to (client side). */
    @Nullable
    public GreenhouseSnapshotPayload getClientSnapshot() {
        return clientSnapshots.get(blockPos);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Nullable
    public GreenhouseControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(blockPos) instanceof GreenhouseControllerBlockEntity
                && blockPos.closerToCenterThan(player.position(), 8.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no slots
    }
}
