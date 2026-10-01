package euphy.upo.create_cultivation.content.dehumidifier;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

/**
 * BE for the dehumidifier: a single {@link #TANK_CAPACITY} mB tank that
 * collects condensed water. The tank contents sync to the client on every
 * change; {@link #fluidLevel} chases the fill fraction so
 * {@link DehumidifierRenderer} can animate the visible water level in the
 * glass bottle (same pattern as Create's fluid tank).
 */
public class DehumidifierBlockEntity extends SmartBlockEntity {

    public static final int TANK_CAPACITY = 1000;
    /** Condenses 1 mB every 2 ticks = 10 mB per second while active. */
    private static final int CONDENSATION_INTERVAL = 2;
    private static final int CONDENSATION_MB = 1;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            sendData();
        }
    };

    private final LerpedFloat fluidLevel = LerpedFloat.linear()
            .startWithValue(0);

    private int condensationCooldown;

    public DehumidifierBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    public FluidStack getFluid() {
        return tank.getFluid();
    }

    public float getFillState() {
        return (float) tank.getFluidAmount() / tank.getCapacity();
    }

    public LerpedFloat getFluidLevel() {
        return fluidLevel;
    }

    /** Capability entry point registered in CCBlockEntities#registerCapabilities. */
    public IFluidHandler fluidHandler() {
        return tank;
    }

    /**
     * External view of the condensate tank: extraction only. Pipes may drain
     * the collected water through the outlet plate, but refilling from
     * outside would (a) make no sense for a machine that produces water and
     * (b) let anyone disable the device by topping the tank up. The internal
     * (side == null) capability keeps returning the raw tank, so the device's
     * own condensation logic is unaffected.
     */
    public IFluidHandler externalFluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return tank.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tankSlot) {
                return tank.getFluidInTank(tankSlot);
            }

            @Override
            public int getTankCapacity(int tankSlot) {
                return tank.getTankCapacity(tankSlot);
            }

            @Override
            public boolean isFluidValid(int tankSlot, FluidStack stack) {
                return tank.isFluidValid(tankSlot, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return 0; // extraction only: never accept fluid from outside
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return tank.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return tank.drain(maxDrain, action);
            }
        };
    }

    @Override
    public void tick() {
        super.tick();
        fluidLevel.tickChaser();

        if (level == null || level.isClientSide)
            return;
        if (!getBlockState().getValue(DehumidifierBlock.ACTIVE))
            return;
        if (++condensationCooldown < CONDENSATION_INTERVAL)
            return;
        condensationCooldown = 0;
        if (tank.getFluidAmount() >= TANK_CAPACITY)
            return;
        tank.fill(new FluidStack(Fluids.WATER, CONDENSATION_MB), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // the greenhouse controller drives ACTIVE and the climate logic
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    public void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        tank.readFromNBT(registries, compound.getCompound("Tank"));
        fluidLevel.chase(getFillState(), 0.5f, LerpedFloat.Chaser.EXP);
    }
}
