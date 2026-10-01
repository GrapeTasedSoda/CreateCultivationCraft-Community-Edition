package euphy.upo.create_cultivation.content.airconditioner;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import euphy.upo.create_cultivation.registry.CCParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * BE for the air conditioner. Server side it is a plain SmartBlockEntity
 * (the greenhouse controller drives the block's ACTIVE state); client side
 * its ticker spawns the cold-blast particles from the model's front vent,
 * aimed 22.5 degrees below horizontal by {@link AirConditionerBlastParticle}.
 */
public class AirConditionerBlockEntity extends SmartBlockEntity {

    public AirConditionerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // no behaviours - activation comes from the greenhouse controller
    }

    /** Client only: spawn the vent blast from the model's front slats. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, AirConditionerBlockEntity be) {
        if (!level.isClientSide || !state.getValue(AirConditionerBlock.ACTIVE))
            return;

        Direction facing = state.getValue(AirConditionerBlock.FACING);
        // muzzle sits just inside the front face (0.49 < 0.5 keeps the spawn
        // cell on this block so the particle can read the facing back)
        Vec3 muzzle = Vec3.atLowerCornerOf(pos)
                .add(0.5, 0.62, 0.5)
                .add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.49));

        // jitter only within the face plane: along the perpendicular
        // horizontal axis and vertically - never across the block boundary
        Direction side = facing.getClockWise();
        Vec3 sideAxis = Vec3.atLowerCornerOf(side.getNormal());
        Vec3 upAxis = new Vec3(0, 1, 0);
        SimpleParticleType type = CCParticles.AIRCONDITIONER_BLAST.get();
        for (int i = 0; i < 2; i++) {
            double s = (level.random.nextDouble() - 0.5) * 0.3;
            double u = (level.random.nextDouble() - 0.5) * 0.12;
            Vec3 p = muzzle.add(sideAxis.scale(s)).add(upAxis.scale(u));
            level.addParticle(type, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // server side: nothing yet - future stress/water consumption hooks
    }
}
