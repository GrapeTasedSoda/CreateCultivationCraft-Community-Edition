package euphy.upo.create_cultivation.content.airconditioner;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Air conditioner vent blast: a pale-cold jet that leaves the machine's front
 * at 22.5 degrees below horizontal, spreads slightly over its lifetime and
 * sinks gently (weak gravity) like conditioned air pooling on the floor.
 *
 * <p>The spawn position is pre-offset by the block's clientTick into the
 * particle slots (same smuggling pattern as the humidifier mist), so this
 * class only needs the facing (read from the spawn block state) to compute
 * the 22.5-degree-down jet direction.
 */
@OnlyIn(Dist.CLIENT)
public class AirConditionerBlastParticle extends TextureSheetParticle {

    private static final float RED = 0.72f;
    private static final float GREEN = 0.88f;
    private static final float BLUE = 1.0f;
    /** Jet angle below horizontal, degrees. */
    private static final float JET_ANGLE_DEG = 22.5f;

    private final Vec3 velocity;
    private final int fadeStart;
    private int tickCount;

    public AirConditionerBlastParticle(ClientLevel level, double x, double y, double z,
            double spread, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);

        Direction facing = Direction.NORTH;
        BlockState state = level.getBlockState(BlockPos.containing(x, y, z));
        if (state.getBlock() instanceof AirConditionerBlock)
            facing = state.getValue(AirConditionerBlock.FACING);

        Vec3 forward = Vec3.atLowerCornerOf(facing.getNormal());
        double rad = Math.toRadians(JET_ANGLE_DEG);
        // 22.5 degrees below horizontal, with a little lateral spread
        Vec3 lateral = Vec3.atLowerCornerOf(facing.getClockWise().getNormal());
        double drift = (this.random.nextDouble() - 0.5) * spread;
        this.velocity = new Vec3(
                forward.x * Math.cos(rad) + lateral.x * drift,
                -Math.sin(rad) + (this.random.nextDouble() - 0.5) * 0.01,
                forward.z * Math.cos(rad) + lateral.z * drift).scale(0.07 + this.random.nextDouble() * 0.03);

        this.lifetime = 12 + this.random.nextInt(5);
        this.fadeStart = this.lifetime / 2;
        this.hasPhysics = false;
        this.gravity = 0.006f;
        this.quadSize = 0.1f + this.random.nextFloat() * 0.06f;
        this.setSize(0.2f, 0.2f);
        this.setColor(RED, GREEN, BLUE);
        this.setAlpha(0f);
        this.xd = this.velocity.x;
        this.yd = this.velocity.y;
        this.zd = this.velocity.z;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.tickCount++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.xd = this.velocity.x;
        this.yd = this.velocity.y + this.gravity * this.tickCount;
        this.zd = this.velocity.z;

        // dissipate the moment the next step would enter a block with a
        // collision shape (walls, floor, other machines) - checked manually
        // because hasPhysics would wedge the particle inside its own machine
        double nx = this.x + this.xd;
        double ny = this.y + this.yd;
        double nz = this.z + this.zd;
        BlockPos target = BlockPos.containing(nx, ny, nz);
        BlockState state = this.level.getBlockState(target);
        if (!state.isAir() && !state.getCollisionShape(this.level, target).isEmpty()) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);

        // quick fade-in, slow fade-out
        float t = this.tickCount / (float) this.lifetime;
        float alpha = t < 0.2f ? t / 0.2f : (this.tickCount >= this.fadeStart
                ? 1f - (t - 0.5f) / 0.5f : 1f);
        this.setAlpha(Mth.clamp(alpha, 0f, 0.55f));
        this.quadSize += 0.004f;
    }

    @Override
    public net.minecraft.client.particle.ParticleRenderType getRenderType() {
        return net.minecraft.client.particle.ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<net.minecraft.core.particles.SimpleParticleType> {
        @Override
        public Particle createParticle(net.minecraft.core.particles.SimpleParticleType type,
                ClientLevel level, double x, double y, double z, double sx, double sy, double sz) {
            return new AirConditionerBlastParticle(level, x, y, z, 0.06, sprites);
        }
    }
}
