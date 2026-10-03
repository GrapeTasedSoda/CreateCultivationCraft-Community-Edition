package euphy.upo.create_cultivation.content.humidifier;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
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
 * The humidifier: a 6-way orientable machine. It stores 1000 mB of fluid and
 * "activates" by sliding its head 3px towards its front (authored =
 * blockbench north); activation is controlled by the greenhouse controller.
 *
 * <p>{@code facing} is the direction the opening (head) points; the fluid
 * inlet is the back plate, i.e. the opposite side. Placement: beside a fluid
 * source the inlet hugs it (facing points away); otherwise piston-style with
 * the head towards the player. A wrench rotates it. The collision box
 * follows the model: closed shell spans 2..14 x 2..14 x 6..16 (authored
 * north), the open head reaches z 3 - rotated to all six facings.
 */
public class HumidifierBlock extends BaseEntityBlock implements IBE<HumidifierBlockEntity>, IWrenchable {

    public static final MapCodec<HumidifierBlock> CODEC = simpleCodec(HumidifierBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** Authored-north bounds; every facing variant is prebuilt at class init. */
    private static final Map<Direction, VoxelShape> CLOSED_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> OPEN_SHAPES = new EnumMap<>(Direction.class);

    static {
        // authored north (head/opening at -z, back plate at +z): body
        // x 2..14, y 2..14, z 6..16; open head reaches z 3. Blockstate
        // rotations (observer reference): y=90 sends authored south (back
        // plate) to world WEST, so facing=east keeps the body at the west
        // edge; UP/DOWN use the observer x-rotation (270/90).
        CLOSED_SHAPES.put(Direction.NORTH, Block.box(2, 2, 6, 14, 14, 16));
        CLOSED_SHAPES.put(Direction.SOUTH, Block.box(2, 2, 0, 14, 14, 10));
        CLOSED_SHAPES.put(Direction.EAST, Block.box(0, 2, 2, 10, 14, 14));
        CLOSED_SHAPES.put(Direction.WEST, Block.box(6, 2, 2, 16, 14, 14));
        CLOSED_SHAPES.put(Direction.UP, Block.box(2, 0, 2, 14, 10, 14));
        CLOSED_SHAPES.put(Direction.DOWN, Block.box(2, 6, 2, 14, 16, 14));
        OPEN_SHAPES.put(Direction.NORTH, Block.box(2, 2, 3, 14, 14, 16));
        OPEN_SHAPES.put(Direction.SOUTH, Block.box(2, 2, 0, 14, 14, 13));
        OPEN_SHAPES.put(Direction.EAST, Block.box(0, 2, 2, 13, 14, 14));
        OPEN_SHAPES.put(Direction.WEST, Block.box(3, 2, 2, 16, 14, 14));
        OPEN_SHAPES.put(Direction.UP, Block.box(2, 0, 2, 14, 13, 14));
        OPEN_SHAPES.put(Direction.DOWN, Block.box(2, 3, 2, 14, 16, 14));
    }

    public HumidifierBlock(Properties properties) {
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
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ACTIVE);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide)
            return createTickerHelper(type, CCBlockEntities.HUMIDIFIER.get(), HumidifierBlockEntity::clientTick);
        return new SmartBlockEntityTicker<>();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos placedPos = context.getClickedPos();
        // Priority 1: placed beside a fluid source (Create pipe wall) ->
        // the INLET is the back plate (authored south = opposite of facing),
        // so the facing points AWAY from the source: back plate hugs the pipe.
        for (Direction d : Direction.values()) {
            if (FluidPropagator.hasFluidCapability(context.getLevel(), placedPos.relative(d), d.getOpposite()))
                return defaultBlockState().setValue(FACING, d.getOpposite()).setValue(ACTIVE, false);
        }
        // Priority 2: piston-style - the back rests on the clicked surface,
        // the head points back towards the player (works in all 6 dirs).
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(ACTIVE, false);
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
        Direction facing = state.getValue(FACING);
        return (state.getValue(ACTIVE) ? OPEN_SHAPES : CLOSED_SHAPES).get(facing);
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
        if (!level.isLoaded(pos))
            return; // never sync-load a chunk just to flip the flag
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HumidifierBlock && state.getValue(ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ACTIVE, active), 3);
        }
    }

    @Override
    public Class<HumidifierBlockEntity> getBlockEntityClass() {
        return HumidifierBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HumidifierBlockEntity> getBlockEntityType() {
        return CCBlockEntities.HUMIDIFIER.get();
    }
}
