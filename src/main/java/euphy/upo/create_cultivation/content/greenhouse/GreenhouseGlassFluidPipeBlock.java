package euphy.upo.create_cultivation.content.greenhouse;

import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import euphy.upo.create_cultivation.registry.CCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Greenhouse-encased window fluid pipe: Create's {@link GlassFluidPipeBlock}
 * (the transparent pipe showing the flowing fluid) wearing a greenhouse glass
 * shell. Extending the glass pipe keeps axis connections, waterlogging,
 * collision and the fluid-stream rendering pipeline intact; only the block
 * entity type is redirected so brackets can be refused and the fluid visual
 * reused as-is (GlassPipeVisual is typed on StraightPipeBlockEntity).
 */
public class GreenhouseGlassFluidPipeBlock extends GlassFluidPipeBlock {

    public GreenhouseGlassFluidPipeBlock(Properties properties) {
        super(properties);
    }

    /**
     * The window core is axis-aligned and can only ever be a straight pipe,
     * so a single frozen axis captures its whole shape (set at encasing /
     * placement time; neighbours never change it).
     */
    public static final EnumProperty<Direction.Axis> FROZEN_AXIS =
            EnumProperty.create("frozen_axis", Direction.Axis.class);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FROZEN_AXIS);
    }

    /** Item placement: freeze the visible core axis to the placed axis. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null)
            state = state.setValue(FROZEN_AXIS,
                    state.getValue(BlockStateProperties.AXIS));
        return state;
    }

    @Override
    public BlockEntityType<? extends StraightPipeBlockEntity> getBlockEntityType() {
        return CCBlockEntities.GREENHOUSE_GLASS_FLUID_PIPE.get();
    }

    /** Hide interior faces against the whole greenhouse glass family. */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return GreenhouseGlassBlock.isFamily(adjacent.getBlock()) || super.skipRendering(state, adjacent, side);
    }

    /** The shell is a full glass block: full-cube collision and occlusion. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    /** Transparent shell: light passes through like greenhouse glass. */
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

    /** No wrench behaviour: no back-conversion to the regular pipe. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, net.minecraft.world.level.block.entity.BlockEntity be) {
        return ItemRequirement.of(CCBlocks.GREENHOUSE_GLASS_FLUID_PIPE.getDefaultState(), be);
    }
}
