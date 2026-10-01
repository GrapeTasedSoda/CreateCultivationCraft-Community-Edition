package euphy.upo.create_cultivation.content.airconditioner;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
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

/**
 * The air conditioner: a furnace-style 4-way machine block. The greenhouse
 * controller drives its ACTIVE state while regulating temperature. Placement
 * is furnace-style: the front turns towards the player; a wrench rotates it
 * through the four horizontal facings. While active, the client ticker emits
 * a cold vent blast from the front, 22.5 degrees below horizontal.
 */
public class AirConditionerBlock extends BaseEntityBlock implements IBE<AirConditionerBlockEntity>, IWrenchable {

    public static final MapCodec<AirConditionerBlock> CODEC = simpleCodec(AirConditionerBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Driven by the greenhouse controller: the machine is actively regulating. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AirConditionerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** Furnace-style: front towards the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
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

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock defaults to INVISIBLE - the static model must stay
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide)
            return createTickerHelper(type, CCBlockEntities.AIR_CONDITIONER.get(), AirConditionerBlockEntity::clientTick);
        return new SmartBlockEntityTicker<>();
    }

    /** Controller hook: switches the running state (same pattern as the humidifier). */
    public static void setActive(Level level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AirConditionerBlock && state.getValue(ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ACTIVE, active), 3);
        }
    }

    @Override
    public Class<AirConditionerBlockEntity> getBlockEntityClass() {
        return AirConditionerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirConditionerBlockEntity> getBlockEntityType() {
        return CCBlockEntities.AIR_CONDITIONER.get();
    }
}
