package euphy.upo.create_cultivation.content.humidifier;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Humidifier mist: two-phase waypoint particle.
 *
 * <p>Phase 1 (travel): rises from inside the machine's hollow body and drifts
 * to a random point on the open head plane, following a smoothstep path so it
 * accelerates out of the box and settles at the head.
 *
 * <p>Phase 2 (burst): at the head plane the velocity is replaced with a radial
 * burst (perpendicular to the facing, plus a small forward component through
 * the open head); the puff then expands and fades out.
 *
 * <p>The waypoint arrives through the xSpeed/ySpeed/zSpeed slots of
 * {@code addParticle} (see CCParticles), so no custom particle options are
 * needed. Fullbright + translucent sheet gives the pale blue steam look.
 */
@OnlyIn(Dist.CLIENT)
public class HumidifierMistParticle extends TextureSheetParticle {

    private static final float RED = 0.65f;
    private static final float GREEN = 0.82f;
    private static final float BLUE = 1.0f;

    private final Vec3 start;
    private final Vec3 waypoint;
    private final int travelTicks;
    private final Vec3 forward;
    private final Vec3 burstDir;
    private Vec3 burstVelocity;
    private int tickCount;

    public HumidifierMistParticle(ClientLevel level, double x, double y, double z,
            double wpX, double wpY, double wpZ, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);

        this.start = new Vec3(x, y, z);
        this.waypoint = new Vec3(wpX, wpY, wpZ);
        this.travelTicks = Mth.clamp((int) Math.round(this.start.distanceTo(this.waypoint) * 16), 5, 16);
        this.lifetime = this.travelTicks + 8 + this.random.nextInt(4);

        Direction facing = Direction.UP;
        BlockState state = level.getBlockState(new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
        if (state.getBlock() instanceof HumidifierBlock)
            facing = state.getValue(HumidifierBlock.FACING);
        this.forward = Vec3.atLowerCornerOf(facing.getNormal());

        // radial burst direction in the plane perpendicular to the facing
        Vec3 u = facing.getAxis() == Direction.Axis.X ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 radial = u.cross(this.forward);
        double angle = this.random.nextDouble() * Math.PI * 2;
        double radius = 0.06 + this.random.nextDouble() * 0.08;
        this.burstDir = radial.scale(Math.cos(angle) * radius)
                .add(u.scale(Math.sin(angle) * radius))
                .add(this.forward.scale(0.02 + this.random.nextDouble() * 0.02));
        this.burstVelocity = null;

        this.hasPhysics = false;
        this.gravity = 0;
        this.quadSize = 0.08f + this.random.nextFloat() * 0.05f;
        this.setSize(0.2f, 0.2f);
        this.setColor(RED, GREEN, BLUE);
        this.setAlpha(0f);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.tickCount++;

        if (this.tickCount <= this.travelTicks) {
            // phase 1: smoothstep glide from the body centre to the head plane
            float t = this.tickCount / (float) this.travelTicks;
            float eased = t * t * (3 - 2 * t);
            Vec3 p = this.start.lerp(this.waypoint, eased);
            this.setPos(p.x, p.y, p.z);
            this.setAlpha(0.75f * Math.min(1f, t * 4f));
            return;
        }

        if (this.burstVelocity == null) {
            // phase 2 start: scatter from the head plane
            this.burstVelocity = this.burstDir;
            this.setPos(this.waypoint.x, this.waypoint.y, this.waypoint.z);
        }

        // expand, decelerate and fade out
        this.quadSize *= 1.07f;
        this.burstVelocity = this.burstVelocity.scale(0.82);
        this.setPos(this.x + this.burstVelocity.x, this.y + this.burstVelocity.y,
                this.z + this.burstVelocity.z);
        int burstAge = this.tickCount - this.travelTicks;
        int burstLength = this.lifetime - this.travelTicks;
        this.setAlpha(0.75f * (1f - burstAge / (float) burstLength));

        if (this.tickCount >= this.lifetime)
            this.remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0; // fullbright so the mist reads as soft glow anywhere
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double wpX, double wpY, double wpZ) {
            return new HumidifierMistParticle(level, x, y, z, wpX, wpY, wpZ, this.sprites);
        }
    }
}
