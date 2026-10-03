package euphy.upo.create_cultivation.content.humidifier;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import euphy.upo.create_cultivation.registry.CCParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

/**
 * BE for the humidifier: a single 1000 mB tank filled by Create pipes from
 * the block's south face, plus the "active" flag that slides the head open.
 * Fluid contents sync to the client on every change so the controller UI can
 * read them.
 */
public class HumidifierBlockEntity extends SmartBlockEntity {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final int TANK_CAPACITY = 1000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            sendData();
        }
    };

    public HumidifierBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    public FluidStack getFluid() {
        return tank.getFluid();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // no behaviours yet - activation comes from the greenhouse controller
    }

    /** Sprays 1 mB every 2 ticks = 10 mB/s while active (mirror of the dehumidifier). */
    private static final int SPRAY_INTERVAL = 2;
    private static final int SPRAY_MB = 1;

    private int sprayCooldown;

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        if (!getBlockState().getValue(HumidifierBlock.ACTIVE))
            return;
        if (++sprayCooldown < SPRAY_INTERVAL)
            return;
        sprayCooldown = 0;
        // empty tank -> the controller notices next step and deactivates
        tank.drain(SPRAY_MB, IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * Client only: while active, spawn pale-blue mist inside the machine's
     * hollow body; each particle glides to a wobbling point on the open head
     * plane, scatters there and fades. The waypoint is smuggled through the
     * speed slots (HumidifierMistParticle consumes them).
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, HumidifierBlockEntity be) {
        if (!level.isClientSide || !state.getValue(HumidifierBlock.ACTIVE))
            return;

        Direction facing = state.getValue(HumidifierBlock.FACING);
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        float t = (float) (level.getGameTime() + pos.hashCode() * 7);

        // head-plane centre (u = 0.8125 along the facing), wobbling slowly
        Vec3 nozzle = Vec3.atLowerCornerOf(pos).add(0.5, 0.5, 0.5)
                .add(normal.scale(0.3125))
                .add(Mth.sin(t * 0.31f) * 0.045f + Mth.sin(t * 0.13f) * 0.02f,
                     Mth.cos(t * 0.23f) * 0.045f + Mth.sin(t * 0.09f) * 0.02f,
                     Mth.sin(t * 0.27f) * 0.045f + Mth.cos(t * 0.11f) * 0.02f);

        if (level.random.nextFloat() < 0.7f) {
            double ox = (level.random.nextDouble() - 0.5) * 0.32;
            double oy = (level.random.nextDouble() - 0.5) * 0.32;
            double oz = (level.random.nextDouble() - 0.5) * 0.32;
            level.addParticle(CCParticles.HUMIDIFIER_MIST.get(),
                    pos.getX() + 0.5 + ox, pos.getY() + 0.5 + oy, pos.getZ() + 0.5 + oz,
                    nozzle.x, nozzle.y, nozzle.z);
        }
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("Tank"))
            tank.readFromNBT(registries, compound.getCompound("Tank"));
    }

    /** Capability entry point registered in CCBlockEntities#registerCapabilities. */
    public IFluidHandler fluidHandler() {
        return tank;
    }
}
