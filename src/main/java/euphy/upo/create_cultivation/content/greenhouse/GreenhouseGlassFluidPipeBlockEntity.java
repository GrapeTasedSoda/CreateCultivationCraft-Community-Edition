package euphy.upo.create_cultivation.content.greenhouse;

import java.util.List;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE for the greenhouse-encased window pipe. Identical to Create's
 * {@link StraightPipeBlockEntity} (same transport behaviour - it renders rim
 * attachments without any encased-pipe suppression - and the fluid stream
 * visual works on it directly), except brackets are never accepted: a bracket
 * would clip through the transparent glass shell.
 */
public class GreenhouseGlassFluidPipeBlockEntity extends StraightPipeBlockEntity {

    public GreenhouseGlassFluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new StraightPipeFluidTransportBehaviour(this));
        behaviours.add(new BracketedBlockEntityBehaviour(this, state -> false));
        registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
    }
}
