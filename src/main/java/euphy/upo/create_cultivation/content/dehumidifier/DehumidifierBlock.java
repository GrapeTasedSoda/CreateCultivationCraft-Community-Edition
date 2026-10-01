package euphy.upo.create_cultivation.content.dehumidifier;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.block.IBE;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * The dehumidifier: a furnace-style 4-way orientable machine that condenses
 * water into its glass bottle. The bottle interior (authored x 1.25..14.75,
 * y 2.25..6, z 6.25..12) is rendered with a real fluid box by
 * {@link DehumidifierRenderer}, so the fill level is visible from outside
 * through the cutout bottle windows - same trick as Create's fluid tank.
 *
 * <p>{@code facing} is the direction the bottle window faces; the fluid port
 * (accepts water input) is the back plate, i.e. the opposite side. Placement
 * is horizontal-only: beside a fluid source the port hugs it (facing points
 * away), otherwise the front turns towards the player. A wrench rotates it
 * through the four horizontal facings. Collision covers the authored hull
 * x 1..15, y 2..14, z 6..16 rotated to the four facings.
 */
public class DehumidifierBlock extends BaseEntityBlock implements IBE<DehumidifierBlockEntity>, IWrenchable {

    public static final MapCodec<DehumidifierBlock> CODEC = simpleCodec(DehumidifierBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Right-click toggle (test hook); later driven by the greenhouse controller. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** Authored-north bounds; every facing variant is prebuilt at class init. */
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // authored north (bottle window at -z, fluid port at +z): hull
        // x 1..15, y 2..14, z 6..16. Same rotation mapping as the humidifier:
        // y=90 sends authored south (port) to world west.
        SHAPES.put(Direction.NORTH, Block.box(1, 2, 6, 15, 14, 16));
        SHAPES.put(Direction.SOUTH, Block.box(1, 2, 0, 15, 14, 10));
        SHAPES.put(Direction.EAST, Block.box(0, 2, 1, 10, 14, 15));
        SHAPES.put(Direction.WEST, Block.box(6, 2, 1, 16, 14, 15));
    }

    public DehumidifierBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos placedPos = context.getClickedPos();
        // Priority 1: beside a fluid source on a horizontal side -> the port
        // (back plate, authored south = opposite of facing) hugs it.
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (FluidPropagator.hasFluidCapability(context.getLevel(), placedPos.relative(d), d.getOpposite()))
                return defaultBlockState().setValue(FACING, d.getOpposite());
        }
        // Priority 2: furnace-style - front (bottle window) towards the player.
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Create wrench: rotate the machine (sneak-wrench dismantles via default). */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        level.setBlock(pos, state.cycle(FACING), 3);
        AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1f, 1f);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Activation hook for the greenhouse controller (server side). */
    public static void setActive(Level level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DehumidifierBlock && state.getValue(ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ACTIVE, active), 3);
        }
    }

    @Override
    public Class<DehumidifierBlockEntity> getBlockEntityClass() {
        return DehumidifierBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DehumidifierBlockEntity> getBlockEntityType() {
        return CCBlockEntities.DEHUMIDIFIER.get();
    }
}
