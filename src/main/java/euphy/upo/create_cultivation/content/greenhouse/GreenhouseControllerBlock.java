package euphy.upo.create_cultivation.content.greenhouse;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import euphy.upo.create_cultivation.registry.CCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Placeholder controller block for the planned greenhouse multiblock.
 * <p>
 * Follows Create's MechanicalCrafter pattern: a HorizontalKineticBlock that is
 * also a small ICogWheel. The rotation axis is the horizontal facing axis, so
 * the internal cogwheel always faces the direction the player placed it in
 * (the model's native front is north, rotated by the blockstate y rotation).
 * <p>
 * Power rules (vanilla Create engine, RotationPropagator):
 * <ul>
 *   <li>{@link #hasShaftTowards} stays false on every face (the gearbox is
 *       closed), so shafts and cogs can never couple axially from any face -
 *       top, front and back cannot take shafts.</li>
 *   <li>Small cogwheel meshing is accepted on the bottom and the two sides of
 *       the gear face (direction perpendicular to the facing axis, cog axis
 *       equal to the facing axis). The integrated cogwheel placement helper
 *       proposes exactly those positions.</li>
 *   <li>Front and back never conduct (direction parallel to the facing axis is
 *       rejected by the engine).</li>
 * </ul>
 * The top face is hard-vetoed engine-side via two mixins: CogWheelBlock
 * (isValidCogwheelPosition - kills top cogwheel ghost hints AND manual top
 * placement through canSurvive) and RotationPropagator (meshing conduction
 * for a cog already sitting in the cell above). The cell above the 22 px
 * tall model is considered part of the machine.
 * <p>
 * Two bulb lamps on top light up (display-link style fullbright glow) while
 * the machine is powered: green blinks while a valid greenhouse is scanned,
 * yellow stays lit when the connected devices cannot reach the setpoints.
 */
public class GreenhouseControllerBlock extends HorizontalKineticBlock implements IBE<GreenhouseControllerBlockEntity>, ICogWheel {

    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    /** Collision for north/south facings: 16 wide, 23 tall, 2px recess front/back. */
    private static final VoxelShape SHAPE_NS = Block.box(0.0D, 0.0D, 2.0D, 16.0D, 23.0D, 14.0D);

    /** Collision for east/west facings: same cabinet rotated 90 degrees. */
    private static final VoxelShape SHAPE_EW = Block.box(2.0D, 0.0D, 0.0D, 14.0D, 23.0D, 16.0D);

    public GreenhouseControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GreenhouseControllerBlockEntity be) {
            // no kinetic power -> no GUI (the controller is offline)
            if (be.getSpeed() == 0) {
                return InteractionResult.PASS;
            }
            player.openMenu(be, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Class<GreenhouseControllerBlockEntity> getBlockEntityClass() {
        return GreenhouseControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GreenhouseControllerBlockEntity> getBlockEntityType() {
        return CCBlockEntities.GREENHOUSE_CONTROLLER.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        // The internal cogwheel plane is vertical and contains the facing
        // direction, so the shaft axis points along the facing (front/back).
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    /**
     * The controller meshes with small cogwheels placed beside it
     * (perpendicular to the rotation axis), so greenhouse power trains
     * attach to its sides.
     */
    @Override
    public boolean isSmallCog() {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HORIZONTAL_FACING).getAxis() == Direction.Axis.X ? SHAPE_EW : SHAPE_NS;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WORKING) ? 6 : 0;
    }
}
