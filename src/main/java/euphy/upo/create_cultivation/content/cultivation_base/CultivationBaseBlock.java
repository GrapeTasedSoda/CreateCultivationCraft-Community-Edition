package euphy.upo.create_cultivation.content.cultivation_base;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlock;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import euphy.upo.create_cultivation.registry.CCBlockEntities;
import euphy.upo.create_cultivation.registry.CCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;

public class CultivationBaseBlock extends HorizontalKineticBlock implements IBE<CultivationBaseBlockEntity>, ICogWheel {

    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private static final VoxelShape TANK_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public CultivationBaseBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HORIZONTAL_FACING, Direction.NORTH)
                .setValue(WORKING, false));
    }



    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CultivationBaseBlockEntity baseBE) {
            baseBE.updateWorkingState();
        }
    }


    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return;

        if (fromPos.equals(pos.above())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CultivationBaseBlockEntity baseBE) {
                baseBE.updateWorkingState();
            }
        }
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Placing a cultivation tank directly on top of the base: right-click
        // with the tank item to build the tank at pos.above() instead of
        // opening any GUI. The GUI stays reachable with an empty hand or any
        // other item.
        if (stack.is(CCBlocks.CULTIVATION_TANK.get().asItem()) && level.getBlockState(pos.above()).isAir()) {
            if (!level.isClientSide) {
                BlockState tankState = CCBlocks.CULTIVATION_TANK.getDefaultState()
                    .setValue(CultivationTankBlock.TOP, true)
                    .setValue(CultivationTankBlock.BOTTOM, false);
                level.setBlock(pos.above(), tankState, 3);
                if (!player.getAbilities().instabuild) {
                    stack.consume(1, player);
                }
                BlockEntity placedBE = level.getBlockEntity(pos.above());
                if (placedBE instanceof CultivationTankBlockEntity placedTank) {
                    placedTank.updateWorkingState();
                }
                level.playSound(null, pos,
                    CCBlocks.CULTIVATION_TANK.get().defaultBlockState().getSoundType().getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Open the GUI regardless of what the player is holding, so the catalyst
        // slot can be filled straight from the hotbar. Normal right-click opens
        // the menu; sneak-right-click keeps default item behaviour for wrenches.
        if (!player.isShiftKeyDown()) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof CultivationBaseBlockEntity blockEntity) {
                player.openMenu(blockEntity, pos);
            }
            return ItemInteractionResult.CONSUME;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Empty hand: open the GUI directly (no sneak needed).
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CultivationBaseBlockEntity blockEntity) {
            player.openMenu(blockEntity, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return TANK_SHAPE;
    }

    @Override
    public Class<CultivationBaseBlockEntity> getBlockEntityClass() {
        return CultivationBaseBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CultivationBaseBlockEntity> getBlockEntityType() {
        return CCBlockEntities.CULTIVATION_BASE.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getValue(WORKING)) {
            return 8;
        }
        return 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity())) {

            if (state.getValue(WORKING)) {
                BlockPos posAbove = pos.above();
                if (level.getBlockEntity(posAbove) instanceof CultivationTankBlockEntity tankBE) {
                    tankBE.getCurrentRecipe().ifPresent(recipeHolder -> {
                        ItemStack seedStack = recipeHolder.value().getIngredients().get(0).getItems()[0].copy();
                        seedStack.setCount(1);
                        Containers.dropItemStack(level, posAbove.getX(), posAbove.getY(), posAbove.getZ(), seedStack);
                    });
                    tankBE.onHarvest();
                }
            }

            if (level.getBlockEntity(pos) instanceof CultivationBaseBlockEntity be) {
                IItemHandler itemHandler = be.getItemHandler();
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemHandler.getStackInSlot(i));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

}
