package euphy.upo.create_cultivation.content.greenhouse;

import java.util.List;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE for the greenhouse-encased fluid pipe.
 *
 * <p>The transport behaviour is a verbatim copy of Create's
 * {@code FluidPipeBlockEntity.StandardPipeFluidTransportBehaviour} (which is
 * package-private and therefore cannot be reused), with one difference: the
 * line suppressing attachment rendering for {@code EncasedPipeBlock} states is
 * dropped, because our glass shell is transparent and needs the interior
 * extension segments and rims to be rendered.
 *
 * <p>Brackets are never accepted: a bracket would clip straight through the
 * transparent glass shell.
 */
public class GreenhouseFluidPipeBlockEntity extends FluidPipeBlockEntity {

    public GreenhouseFluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new GreenhousePipeFluidTransportBehaviour(this));
        behaviours.add(new BracketedBlockEntityBehaviour(this, state -> false));
        registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
    }

    public static class GreenhousePipeFluidTransportBehaviour extends FluidTransportBehaviour {

        public GreenhousePipeFluidTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return (FluidPipeBlock.isPipe(state)
                    || state.getBlock() instanceof com.simibubi.create.content.fluids.pipes.EncasedPipeBlock
                    || state.getBlock() instanceof GreenhouseFluidPipeBlock)
                    && state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state,
                Direction direction) {
            // Freeze: evaluate all attachment logic against the connection
            // snapshot taken at encasing time. Neighbour-side checks (pump
            // facing, drains, connecting pipes) still read the live world.
            state = GreenhouseFluidPipeBlock.frozenView(state);
            AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);

            BlockPos offsetPos = pos.relative(direction);
            BlockState otherState = world.getBlockState(offsetPos);

            // difference vs create's StandardPipeFluidTransportBehaviour: no
            // EncasedPipeBlock suppression - the glass shell is transparent

            if (attachment == AttachmentTypes.RIM) {
                if (!FluidPipeBlock.isPipe(otherState) && !(otherState.getBlock() instanceof com.simibubi.create.content.fluids.pipes.EncasedPipeBlock)
                        && !(otherState.getBlock() instanceof GlassFluidPipeBlock)) {
                    FluidTransportBehaviour pipeBehaviour =
                            BlockEntityBehaviour.get(world, offsetPos, FluidTransportBehaviour.TYPE);
                    if (pipeBehaviour != null && pipeBehaviour.canHaveFlowToward(otherState, direction.getOpposite()))
                        return AttachmentTypes.DETAILED_CONNECTION;
                }

                if (!FluidPipeBlock.shouldDrawRim(world, pos, state, direction))
                    return GreenhouseFluidPipeBlock.frozenStraightAxis(state) == direction.getAxis()
                            ? AttachmentTypes.CONNECTION
                            : AttachmentTypes.DETAILED_CONNECTION;
            }

            if (attachment == AttachmentTypes.NONE
                    && state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction)))
                return AttachmentTypes.DETAILED_CONNECTION;

            return attachment;
        }
    }
}
