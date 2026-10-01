package euphy.upo.create_cultivation.content.greenhouse;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockRotation;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

/**
 * Greenhouse-encased fluid pipe: a frozen fluid pipe wearing a greenhouse
 * glass shell. Architecture mirrors Create's {@link EncasedPipeBlock}: the
 * block deliberately does NOT extend {@link FluidPipeBlock}, so neighbouring
 * pipes fall into the transport-behaviour branch of
 * {@code FluidPipeBlock.canConnectTo} instead of the unconditional
 * {@code isPipe} shortcut. That branch reads our connection properties,
 * which are frozen at encasing time - so neighbours can only attach at the
 * frozen openings (the shell's "inlets"), never at the other faces.
 *
 * <p>Functional freeze: nothing in this class ever rewrites the live
 * north/.../down connection properties after encasing (no updateShape
 * connection logic, unlike FluidPipeBlock), so the network shape and the
 * rendered shape (frozen_* snapshot; core multipart via blockstate keys,
 * rims and T-casing via {@link GreenhousePipeAttachmentModel}) are the same
 * snapshot and never change.
 *
 * <p>The transparent glass shell is non-light-blocking like greenhouse glass:
 * getLightBlock 0 and skylight propagation.
 *
 * <p>Economy parity: encasing consumes one greenhouse glass (survival), and
 * breaking the block drops both the pipe and the shell glass. The wrench is
 * fully disabled - shell shape must not be switchable (it is the wall).
 */
public class GreenhouseFluidPipeBlock extends Block
        implements SpecialBlockItemRequirement, EncasedBlock, TransformableBlock, IWrenchable,
        IBE<GreenhouseFluidPipeBlockEntity> {

    private final Supplier<Block> casing;

    public GreenhouseFluidPipeBlock(Properties properties, Supplier<Block> casing) {
        super(properties);
        this.casing = casing;
        BlockState allFalse = defaultBlockState();
        for (Direction d : Direction.values())
            allFalse = allFalse.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(d), false)
                    .setValue(FROZEN_PROPERTY_BY_DIRECTION.get(d), false);
        registerDefaultState(allFalse);
    }

    /**
     * Frozen snapshot of the inner pipe's connection shape, taken at encasing
     * time. Live and frozen properties are equal from encasing on and are
     * never rewritten, so later neighbours never change shape or function.
     */
    public static final Map<Direction, BooleanProperty> FROZEN_PROPERTY_BY_DIRECTION = new IdentityHashMap<>();

    static {
        for (Direction d : Direction.values()) {
            String name = "frozen_" + d.getName();
            FROZEN_PROPERTY_BY_DIRECTION.put(d, BooleanProperty.create(name));
        }
    }

    /** View of this state with live connections replaced by the frozen ones (they are equal in practice). */
    public static BlockState frozenView(BlockState state) {
        BlockState frozen = state;
        for (Direction d : Direction.values())
            frozen = frozen.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(d),
                    state.getValue(FROZEN_PROPERTY_BY_DIRECTION.get(d)));
        return frozen;
    }

    /** Copy the (already transformed) live connections into the frozen snapshot. */
    public static BlockState snapshotFromLive(BlockState state) {
        BlockState snapshot = state;
        for (Direction d : Direction.values())
            snapshot = snapshot.setValue(FROZEN_PROPERTY_BY_DIRECTION.get(d),
                    state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(d)));
        return snapshot;
    }

    /** Straight-pipe axis of the frozen shape (null = corner/T), mirroring FluidPropagator.getStraightPipeAxis without its isPipe gate. */
    public static Axis frozenStraightAxis(BlockState state) {
        Axis axisFound = null;
        int connections = 0;
        for (Axis axis : Iterate.axes) {
            Direction d1 = Direction.get(AxisDirection.NEGATIVE, axis);
            Direction d2 = Direction.get(AxisDirection.POSITIVE, axis);
            boolean openAt1 = state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(d1));
            boolean openAt2 = state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(d2));
            if (openAt1)
                connections++;
            if (openAt2)
                connections++;
            if (openAt1 && openAt2)
                if (axisFound != null)
                    return null;
                else
                    axisFound = axis;
        }
        return connections == 2 ? axisFound : null;
    }

    /** T-junction casing check for the frozen shape, mirroring FluidPipeBlock.shouldDrawCasing without its isPipe gate. */
    public static boolean shouldDrawFrozenCasing(BlockState state) {
        for (Axis axis : Iterate.axes) {
            int connections = 0;
            for (Direction direction : Iterate.directions)
                if (direction.getAxis() != axis && state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction)))
                    connections++;
            if (connections > 2)
                return true;
        }
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction d : Direction.values())
            builder.add(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(d));
        for (BooleanProperty p : FROZEN_PROPERTY_BY_DIRECTION.values())
            builder.add(p);
    }

    /**
     * Creative-only item placement: connect (and freeze) against whatever the
     * neighbours allow at that moment. Afterwards the shape never changes.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) {
            BlockPos offsetPos = context.getClickedPos().relative(d);
            BlockState neighbour = context.getLevel().getBlockState(offsetPos);
            boolean connect = FluidPipeBlock.canConnectTo(context.getLevel(), offsetPos, neighbour, d);
            state = state.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(d), connect)
                    .setValue(FROZEN_PROPERTY_BY_DIRECTION.get(d), connect);
        }
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        boolean blockTypeChanged = state.getBlock() != newState.getBlock();
        if (blockTypeChanged && !world.isClientSide)
            FluidPropagator.propagateChangedPipe(world, pos, state);
        if (state.hasBlockEntity() && (blockTypeChanged || !newState.hasBlockEntity()))
            world.removeBlockEntity(pos);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!world.isClientSide && state != oldState)
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block otherBlock, BlockPos neighborPos,
            boolean isMoving) {
        DebugPackets.sendNeighborsUpdatePacket(world, pos);
        if (world.isClientSide)
            return;
        // FluidPropagator.validateNeighbourChange always returns null for this
        // block: it only recognises pipes/pumps/EncasedPipeBlock, so neighbour
        // updates at the frozen openings were silently dropped and the pipe
        // kept its stale drain/transport source (a tank placed at the outlet
        // was never fed). Resolve the changed side ourselves instead - the
        // shape stays frozen, but the fluid network re-evaluates.
        Direction d = null;
        for (Direction dir : Iterate.directions)
            if (pos.relative(dir).equals(neighborPos)) {
                d = dir;
                break;
            }
        if (d == null)
            return;
        if (!state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(d)))
            return;
        world.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
        FluidPropagator.propagateChangedPipe(world, pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return snapshotFromLive(FluidPipeBlockRotation.rotate(state, rotation));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return snapshotFromLive(FluidPipeBlockRotation.mirror(state, mirror));
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        return snapshotFromLive(FluidPipeBlockRotation.transform(state, transform));
    }

    /** Hide interior faces against the whole greenhouse glass family. */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return GreenhouseGlassBlock.isFamily(adjacent.getBlock()) || super.skipRendering(state, adjacent, side);
    }

    /** The shell is a full glass block: full-cube collision, light passes through. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** Belt-and-suspenders: never contribute an occlusion face for light. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public Block getCasing() {
        return casing.get();
    }

    /** Encasing swap in place, preserving the six-way connections; costs one glass outside creative. */
    @Override
    public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player,
            InteractionHand hand, BlockHitResult ray) {
        FluidTransportBehaviour.cacheFlows(level, pos);
        level.setBlockAndUpdate(pos, snapshotFromLive(
                EncasedPipeBlock.transferSixWayProperties(state, defaultBlockState())));
        FluidTransportBehaviour.loadFlows(level, pos);
        if (!player.getAbilities().instabuild)
            heldItem.shrink(1);
    }

    /** No wrench behaviour at all: no un-encasing, nothing. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    /** Drops (pipe + shell glass) come from the loot table, sharing survives_explosion. */

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        return AllBlocks.FLUID_PIPE.asStack();
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        return ItemRequirement.of(AllBlocks.FLUID_PIPE.getDefaultState(), be);
    }

    @Override
    public Class<GreenhouseFluidPipeBlockEntity> getBlockEntityClass() {
        return GreenhouseFluidPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GreenhouseFluidPipeBlockEntity> getBlockEntityType() {
        return CCBlockEntities.GREENHOUSE_FLUID_PIPE.get();
    }
}
