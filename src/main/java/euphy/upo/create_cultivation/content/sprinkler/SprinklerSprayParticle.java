package euphy.upo.create_cultivation.content.sprinkler;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Sprinkler spray: a water droplet arcing out of the machine's 9px spray
 * ring. Initial velocity arrives through the xSpeed/ySpeed/zSpeed slots of
 * {@code addParticle} (same smuggling pattern as the humidifier mist).
 *
 * <p>Motion: a weak constant gravity lets the droplet fly nearly flat out
 * of the ring before arcing down, while a per-tick horizontal drag caps
 * the travel distance - the geometric series of a velocity {@code v}
 * under a 0.94 drag factor sums to {@code ~10.4 * v}, and the droplet's
 * 12..16 tick lifetime keeps even the top spawn speed (0.28) inside a
 * 3-block radius (~2.9 blocks). A droplet that would step into a block
 * with a collision shape is removed (walls, ground and neighbouring
 * machines swallow the spray; {@code hasPhysics} is off so droplets don't
 * wedge), except its own machine cell: the spray ring sits inside the
 * machine's block, so the droplet must be allowed to fly out of it.
 */
@OnlyIn(Dist.CLIENT)
public class SprinklerSprayParticle extends TextureSheetParticle {

    private static final float RED = 0.45f;
    private static final float GREEN = 0.68f;
    private static final float BLUE = 1.0f;

    /** Constant downward acceleration per tick - kept weak so the arc stays flat. */
    private static final double GRAVITY = 0.006;
    /** Per-tick horizontal velocity multiplier; caps total travel at ~10.4 * v0. */
    private static final double DRAG = 0.94;

    private int tickCount;
    /** The machine cell the droplet was born in: exempt from the swallow
     *  check, because the spray ring sits inside the machine's own block and
     *  every droplet otherwise died on its very first tick. */
    private final BlockPos spawnBlock;

    public SprinklerSprayParticle(ClientLevel level, double x, double y, double z,
            double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.spawnBlock = BlockPos.containing(x, y, z);
        this.lifetime = 12 + this.random.nextInt(5);
        this.hasPhysics = false;
        this.quadSize = 0.09f + this.random.nextFloat() * 0.05f;
        this.setSize(0.15f, 0.15f);
        this.setColor(RED, GREEN, BLUE);
        this.setAlpha(0f);
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

        this.xd *= DRAG;
        this.zd *= DRAG;
        this.yd -= GRAVITY;

        // a droplet stepping into a collision shape is swallowed by it -
        // except the machine cell it was born in: the spray ring sits inside
        // the machine's own block, so the droplet must be able to leave it
        Vec3 next = new Vec3(this.x + this.xd, this.y + this.yd, this.z + this.zd);
        BlockPos target = BlockPos.containing(next);
        if (!target.equals(this.spawnBlock)) {
            BlockState state = this.level.getBlockState(target);
            if (!state.isAir() && !state.getCollisionShape(this.level, target).isEmpty()) {
                this.remove();
                return;
            }
        }
        this.move(this.xd, this.yd, this.zd);

        // near-instant fade-in: the fastest, most horizontal frames are the
        // first ones
        float t = this.tickCount / (float) this.lifetime;
        float alpha = t < 0.1f ? t / 0.1f : (t > 0.6f ? 1f - (t - 0.6f) / 0.4f : 1f);
        this.setAlpha(Mth.clamp(alpha, 0f, 0.85f));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double vx, double vy, double vz) {
            return new SprinklerSprayParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
