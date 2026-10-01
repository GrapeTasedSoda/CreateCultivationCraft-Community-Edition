package euphy.upo.create_cultivation.content.sprinkler;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The sprinkler: exactly two vertical placement forms, no horizontal form.
 * Clicking a floor's top face places the upright form (FACING=UP, model as
 * authored, inlet on the block's bottom face); clicking a ceiling's bottom
 * face places the hanging form (FACING=DOWN, the blockstate x=180 rotation
 * flips the model so the inlet faces up into the ceiling and the spray ring
 * sits at 6..7px pointing down). Side-face clicks are also allowed - the
 * form follows the player's view direction (looking down = upright, looking
 * up = hanging; a level gaze defaults to upright). The block needs no
 * support: it floats like any normal block once placed.
 */
public class SprinklerBlock extends BaseEntityBlock implements IBE<SprinklerBlockEntity> {

    public static final MapCodec<SprinklerBlock> CODEC = simpleCodec(SprinklerBlock::new);

    /** Only UP (upright on a floor) and DOWN (hanging from a ceiling) are ever set. */
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.UP, Direction.DOWN);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** Upright form bounds: base plate 4..12 wide, machine 0..10 tall. */
    private static final VoxelShape SHAPE_UP = Block.box(4, 0, 4, 12, 10, 12);
    /** Hanging form: the same body rotated 180° around the X axis. */
    private static final VoxelShape SHAPE_DOWN = Block.box(4, 6, 4, 12, 16, 12);

    public SprinklerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ACTIVE);
    }

    /**
     * Two vertical forms only, selected by where the player aims: clicking a
     * floor's top face gives the upright form, a ceiling's bottom face the
     * hanging form. Side clicks are allowed and follow the view direction -
     * looking down places the upright form, looking up the hanging form
     * (a level gaze defaults to upright).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis() == Direction.Axis.Y
                ? face
                : (context.getNearestLookingDirection() == Direction.UP ? Direction.DOWN : Direction.UP);
        return defaultBlockState().setValue(FACING, facing).setValue(ACTIVE, false);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, CCBlockEntities.SPRINKLER.get(), SprinklerBlockEntity::clientTick);
        }
        return new SmartBlockEntityTicker<>();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING) == Direction.DOWN ? SHAPE_DOWN : SHAPE_UP;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public Class<SprinklerBlockEntity> getBlockEntityClass() {
        return SprinklerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SprinklerBlockEntity> getBlockEntityType() {
        return CCBlockEntities.SPRINKLER.get();
    }
}
