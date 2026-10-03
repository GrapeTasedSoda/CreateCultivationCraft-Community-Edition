package euphy.upo.create_cultivation.content.sprinkler;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.registry.CCParticles;

import java.util.List;

/**
 * The sprinkler: an autonomous water consumer. Its inlet is the bottom face;
 * it stores up to 1000 mB of water (water tag fluids only). While water
 * remains the machine is ACTIVE: it consumes 1 mB every 2 ticks (10 mB/s),
 * emits spray particles from the model's 9px spray ring, and keeps the
 * farmland in the 7x7 area below it fully hydrated. Columns blocked by a
 * full opaque block are skipped, and the moment the tank runs dry (with no
 * refill) the machine deactivates - no more particles, no more hydration.
 */
public class SprinklerBlockEntity extends SmartBlockEntity {

    public static final int TANK_CAPACITY = 1000;
    /** Water consumed per spray step: 1 mB every 2 ticks = 10 mB per second. */
    public static final int SPRAY_INTERVAL = 2;
    public static final int SPRAY_MB = 1;
    /** Farmland hydration refresh cadence: 20 ticks = 1 second. */
    public static final int MOISTEN_INTERVAL = 20;
    /** Hydrated area: 7x7 centred on the sprinkler. */
        /** How deep each column scan follows below the sprinkler. */
    public static final int MAX_SCAN_DEPTH = 6;
    /** Vanilla "fully hydrated" moisture value. */
    private static final int MAX_MOISTURE = 7;

    /** Y height of the model's spray ring centre in the upright form (9.5px of the 9..10px disc). */
    public static final double SPRAY_RING_Y_UP = 9.5 / 16.0;
    /** Hanging form (FACING=DOWN): the x=180 blockstate rotation mirrors the ring to 6.5px. */
    public static final double SPRAY_RING_Y_DOWN = 6.5 / 16.0;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack != null && stack.is(FluidTags.WATER);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
            sendData();
        }
    };

    private int sprayCooldown;
    private int moistenCooldown;

    public SprinklerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    /** Capability entry point registered in CCBlockEntities#registerCapabilities. */
    public IFluidHandler fluidHandler() {
        return tank;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // no behaviours - the sprinkler is autonomous, driven by its tank
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean hasWater = tank.getFluidAmount() > 0;
        if (hasWater) {
            if (++sprayCooldown >= SPRAY_INTERVAL) {
                sprayCooldown = 0;
                tank.drain(SPRAY_MB, IFluidHandler.FluidAction.EXECUTE);
            }
            if (++moistenCooldown >= MOISTEN_INTERVAL) {
                moistenCooldown = 0;
                moistenFarmlandBelow();
            }
        } else {
            sprayCooldown = 0;
            moistenCooldown = 0;
        }
        setActive(hasWater);
    }

    /** Flips the block's ACTIVE state (drives the particles). */
    private void setActive(boolean active) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof SprinklerBlock && state.getValue(SprinklerBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(SprinklerBlock.ACTIVE, active), 3);
        }
    }

    /**
     * Keeps the farmland in the 7x7 area below the sprinkler fully hydrated.
     * Each of the 49 columns is scanned downwards from the block below the
     * sprinkler: farmland is hydrated and ends the column, any full opaque
     * block blocks the column (its soil stays vanilla-dry), everything else -
     * air, crops, plants, glass, water - is passed through. Bounded by
     * {@link #MAX_SCAN_DEPTH}.
     */
    private void moistenFarmlandBelow() {
        for (int dx = -CCConfig.SPRINKLER_AREA_RADIUS.get(); dx <= CCConfig.SPRINKLER_AREA_RADIUS.get(); dx++) {
            for (int dz = -CCConfig.SPRINKLER_AREA_RADIUS.get(); dz <= CCConfig.SPRINKLER_AREA_RADIUS.get(); dz++) {
                BlockPos cursor = worldPosition.offset(dx, -1, dz);
                for (int depth = 0; depth < MAX_SCAN_DEPTH; depth++) {
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(Blocks.FARMLAND)) {
                        if (state.getValue(BlockStateProperties.MOISTURE) < MAX_MOISTURE) {
                            // flag 2: sync to clients, skip neighbour updates -
                            // moisture changes never affect neighbouring blocks
                            level.setBlock(cursor, state.setValue(BlockStateProperties.MOISTURE, MAX_MOISTURE), 2);
                        }
                        break;
                    }
                    if (state.isSolidRender(level, cursor)) {
                        break; // blocked: this column's soil stays dry
                    }
                    cursor = cursor.below();
                }
            }
        }
    }

    /**
     * Mirror of the coverage above: is this farmland inside an ACTIVE
     * sprinkler's hydrated 7x7 area? The vanilla farmland drying random tick
     * is not aware of sprinklers (the machine is not a water block) and dried
     * the soil one step per random tick, which the sprinkler then hydrated
     * back on its next cycle - the reported visible moisture flip-flop. The
     * vanilla farm hook consults this and skips those blocks entirely; the
     * sprinkler owns their moisture.
     */
    public static boolean isFarmlandCoveredBySprinkler(ServerLevel level, BlockPos farmlandPos) {
        for (int dx = -CCConfig.SPRINKLER_AREA_RADIUS.get(); dx <= CCConfig.SPRINKLER_AREA_RADIUS.get(); dx++) {
            for (int dz = -CCConfig.SPRINKLER_AREA_RADIUS.get(); dz <= CCConfig.SPRINKLER_AREA_RADIUS.get(); dz++) {
                BlockPos cursor = farmlandPos.offset(dx, 1, dz);
                for (int depth = 0; depth < MAX_SCAN_DEPTH; depth++) {
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof SprinklerBlock && state.getValue(SprinklerBlock.ACTIVE)) {
                        return true;
                    }
                    if (state.is(Blocks.FARMLAND)) {
                        break;
                    }
                    if (state.isSolidRender(level, cursor)) {
                        break;
                    }
                    cursor = cursor.above();
                }
            }
        }
        return false;
    }

    /**
     * Client only: while ACTIVE, emit spray from the model's orange ring at
     * 9px height (upright form) or the mirrored 6.5px ring (hanging form).
     * Droplets leave purely horizontally and evenly in all directions: the
     * ring's phase rotates by the golden angle every tick, so the ten
     * droplets per tick cover the circle uniformly. Gravity then pulls them
     * down (see the particle class); horizontal speeds are drag-capped so
     * the spread never exceeds a 3-block radius.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, SprinklerBlockEntity be) {
        if (!level.isClientSide || !state.getValue(SprinklerBlock.ACTIVE)) {
            return;
        }
        boolean hanging = state.getValue(SprinklerBlock.FACING) == Direction.DOWN;
        double ringY = hanging ? SPRAY_RING_Y_DOWN : SPRAY_RING_Y_UP;
        // golden-angle step (~137.5 deg per tick) keeps successive droplet
        // rings out of each other's lanes; the ten-way spacing inside a tick
        // spreads each batch evenly around the ring, and a small random
        // jitter keeps the circle from looking mechanically perfect
        double base = (level.getGameTime() + pos.asLong()) * 2.399963229728653;
        for (int i = 0; i < 10; i++) {
            double angle = base + i * Math.PI / 5 + (level.random.nextDouble() - 0.5) * 0.25;
            double ring = 0.10 + level.random.nextDouble() * 0.08;
            // 0.16..0.28: with the particle's 0.94 drag and 12..16 tick
            // lifetime the worst-case travel is 0.28 * 10.41 = ~2.9 blocks,
            // still inside the 3-block spread cap
            double speed = 0.16 + level.random.nextDouble() * 0.12;
            level.addParticle(CCParticles.SPRINKLER_SPRAY.get(),
                    pos.getX() + 0.5 + Math.cos(angle) * ring,
                    pos.getY() + ringY,
                    pos.getZ() + 0.5 + Math.sin(angle) * ring,
                    Math.cos(angle) * speed,
                    0,
                    Math.sin(angle) * speed);
        }
        // rare vanilla falling-water drop for the drip read - kept at a low
        // chance: the vanilla drip is strictly vertical and would visually
        // drown out the horizontal spray if used more often
        if (level.random.nextFloat() < 0.05f) {
            level.addParticle(ParticleTypes.FALLING_WATER,
                    pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5,
                    pos.getY() + ringY,
                    pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5,
                    0, -0.05, 0);
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
        if (compound.contains("Tank")) {
            tank.readFromNBT(registries, compound.getCompound("Tank"));
        }
    }
}
