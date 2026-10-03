package euphy.upo.create_cultivation.content.greenhouse;

import java.util.List;

import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.content.climate.ClimateUnits;
import euphy.upo.create_cultivation.content.airconditioner.AirConditionerBlock;
import euphy.upo.create_cultivation.content.dehumidifier.DehumidifierBlock;
import euphy.upo.create_cultivation.content.dehumidifier.DehumidifierBlockEntity;
import euphy.upo.create_cultivation.content.humidifier.HumidifierBlock;
import euphy.upo.create_cultivation.content.humidifier.HumidifierBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The greenhouse climate state machine (server side).
 *
 * <p>Model: the player's setpoints are compared against the ambient climate
 * (which the controller never controls). The required "climate work" is
 * {@code |delta| * volume}; the connected devices provide capacity
 * {@code count * perDeviceCapacity} in the same unit, so a bigger greenhouse
 * needs more (or stronger) devices. The reachable target is the ambient value
 * shifted by at most {@code capacity / volume}; if the demand exceeds the
 * capacity the dimension is flagged as device-starved and the climate simply
 * stops at the reachable value instead of failing.</p>
 *
 * <p>Maintaining, not reaching: as long as the setpoint differs from ambient,
 * the responsible devices keep running (they hold the climate against drift),
 * and the live value converges to the highest value the enabled devices can
 * sustain. Devices drop out individually when their water loop stalls - the
 * dehumidifier when its condensate tank is full, the humidifier when its
 * supply tank runs dry - and the remaining devices then carry the climate.</p>
 */
public final class ClimateController {

    /** Deadband: demands smaller than this count as "no demand". */
    public static final float DEADBAND = 0.05f;

    /** Passive drift towards ambient while unpowered or leaking (per tick, *20 = 10%/s). */
    public static final float PASSIVE_RATE = 0.005f;

    private ClimateController() {}

    /** Devices of one scan, classified by block type. */
    public record DeviceGroups(List<BlockPos> airConditioners, List<BlockPos> humidifiers,
            List<BlockPos> dehumidifiers) {

        public static final DeviceGroups EMPTY = new DeviceGroups(List.of(), List.of(), List.of());
    }

    /** True when the device's water loop lets it run right now. */
    private static boolean waterAllows(ServerLevel level, BlockPos pos, boolean humidifier) {
        if (!level.isLoaded(pos))
            return humidifier ? false : true;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DehumidifierBlockEntity dehum)
            return dehum.getTank().getFluidAmount() < dehum.getTank().getCapacity();
        if (be instanceof HumidifierBlockEntity hum)
            return hum.getTank().getFluidAmount() > 0;
        // unknown BE types (or missing chunk): don't let them block the group
        return humidifier ? false : true;
    }

    /** Counts the devices of one group whose water loop currently allows running. */
    private static int countRunnable(ServerLevel level, List<BlockPos> group, boolean humidifier) {
        int runnable = 0;
        for (BlockPos pos : group)
            if (waterAllows(level, pos, humidifier))
                runnable++;
        return runnable;
    }

    /**
     * One simulation step. Mutates {@code s} and refreshes its cached
     * feasibility data for the GUI/Jade.
     *
     * @param controlling false when unpowered or the greenhouse is invalid
     */
    public static void step(ServerLevel level, ClimateState s, float ambientT, float ambientH,
            float setT, float setH, int volume, DeviceGroups devices, boolean controlling) {
        if (!controlling || volume <= 0) {
            // passive mode: everything relaxes back to the environment
            s.currentT = drift(s.currentT, ambientT, PASSIVE_RATE * 20);
            s.currentH = drift(s.currentH, ambientH, PASSIVE_RATE * 20);
            s.tempShort = false;
            s.humShort = false;
            s.reachT = ambientT;
            s.reachH = ambientH;
            s.tempNeed = 0;
            s.humNeed = 0;
            setGroupActive(level, AirConditionerBlock::setActive, devices.airConditioners(), false);
            setGroupActive(level, HumidifierBlock::setActive, devices.humidifiers(), false);
            setGroupActive(level, DehumidifierBlock::setActive, devices.dehumidifiers(), false);
            return;
        }

        float dT = setT - ambientT;
        float dH = setH - ambientH;
        if (Math.abs(dT) < DEADBAND)
            dT = 0;
        if (Math.abs(dH) < DEADBAND)
            dH = 0;

        // --- temperature: capacity from air conditioners ---
        float acCap = CCConfig.AIRCONDITIONER_CAPACITY.get().floatValue();
        int acRunnable = devices.airConditioners().size();
        float capT = acRunnable * acCap;
        float maxShiftT = capT / volume;
        s.tempShort = Math.abs(dT) > maxShiftT + 1e-4f;
        s.reachT = ClimateUnits.clampTempC(ambientT + clampSigned(dT, maxShiftT));
        s.tempNeed = dT != 0 ? (int) Math.ceil(Math.abs(dT) * volume / acCap) : 0;

        // --- humidity: direction picks the device type; stalled units drop out ---
        boolean raising = dH > 0;
        boolean lowering = dH < 0;
        float hCapPer = raising ? CCConfig.HUMIDIFIER_CAPACITY.get().floatValue()
                : CCConfig.DEHUMIDIFIER_CAPACITY.get().floatValue();
        List<BlockPos> humGroup = raising ? devices.humidifiers() : devices.dehumidifiers();
        int hRunnable = countRunnable(level, humGroup, raising);
        float capH = hRunnable * hCapPer;
        float maxShiftH = capH / volume;
        s.humShort = Math.abs(dH) > maxShiftH + 1e-4f;
        s.reachH = ClimateUnits.clampHumidity(ambientH + clampSigned(dH, maxShiftH));
        s.humNeed = dH != 0 ? (int) Math.ceil(Math.abs(dH) * volume / hCapPer) : 0;

        // --- device activation ---
        // Maintain, not reach: whenever the setpoint differs from ambient, the
        // responsible group stays on (it holds the climate against ambient
        // drift); the live value converges to the sustained value regardless.
        // Zero humidity demand keeps BOTH groups idle (the old "!raising" form
        // ran the dehumidifiers whenever the setpoint equalled the ambient
        // humidity), water-stalled units drop out individually, and the group
        // for the opposite humidity direction is forced off.
        setGroupActive(level, AirConditionerBlock::setActive, devices.airConditioners(), dT != 0);
        setGroupActive(level, HumidifierBlock::setActive, devices.humidifiers(), raising);
        setGroupActive(level, DehumidifierBlock::setActive, devices.dehumidifiers(), lowering);
        for (BlockPos pos : devices.humidifiers())
            if (!raising || !waterAllows(level, pos, true))
                HumidifierBlock.setActive(level, pos, false);
        for (BlockPos pos : devices.dehumidifiers())
            if (!lowering || !waterAllows(level, pos, false))
                DehumidifierBlock.setActive(level, pos, false);

        // --- motion: capacity decides speed too ---
        float stepT = CCConfig.CLIMATE_RATE.get().floatValue() * capT / volume;
        s.currentT = ClimateUnits.clampTempC(drift(s.currentT, s.reachT, stepT));
        float stepH = CCConfig.CLIMATE_RATE.get().floatValue() * capH / volume;
        s.currentH = ClimateUnits.clampHumidity(drift(s.currentH, s.reachH, stepH));
    }

    /** Activates one device block; matches the blocks' setActive(Level, BlockPos, boolean). */
    interface DeviceActivator {
        void accept(ServerLevel level, BlockPos pos, boolean active);
    }

    private static void setGroupActive(ServerLevel level, DeviceActivator activator,
            List<BlockPos> group, boolean active) {
        for (BlockPos pos : group)
            activator.accept(level, pos, active);
    }

    /** Moves {@code current} towards {@code target} by at most {@code maxStep}. */
    private static float drift(float current, float target, float maxStep) {
        float delta = target - current;
        if (Math.abs(delta) <= maxStep)
            return target;
        return current + Math.signum(delta) * maxStep;
    }

    private static float clampSigned(float delta, float max) {
        return Math.max(-max, Math.min(max, delta));
    }

    /**
     * Mutable server-side climate state of one greenhouse (NBT persisted by
     * the controller BE), plus cached feasibility info for the UI.
     */
    public static final class ClimateState {

        private float currentT = Float.NaN;
        private float currentH = Float.NaN;

        private boolean tempShort;
        private boolean humShort;
        private float reachT;
        private float reachH;
        private int tempNeed;
        private int humNeed;

        public float getCurrentTemp() {
            return currentT;
        }

        public float getCurrentHumidity() {
            return currentH;
        }

        public boolean isTempShort() {
            return tempShort;
        }

        public boolean isHumShort() {
            return humShort;
        }

        public float getReachableTemp() {
            return reachT;
        }

        public float getReachableHumidity() {
            return reachH;
        }

        public int getTempDevicesNeeded() {
            return tempNeed;
        }

        public int getHumDevicesNeeded() {
            return humNeed;
        }

        /** True once the live values are initialised (snapped to ambient). */
        public boolean isInitialised() {
            return !Float.isNaN(currentT) && !Float.isNaN(currentH);
        }

        /** Snaps the live climate to the ambient baseline (first valid scan). */
        public void snapTo(float ambientT, float ambientH) {
            currentT = ambientT;
            currentH = ambientH;
        }

        public void save(CompoundTag tag) {
            tag.putFloat("CurrentTemp", currentT);
            tag.putFloat("CurrentHumidity", currentH);
        }

        public void load(CompoundTag tag) {
            currentT = tag.contains("CurrentTemp") ? tag.getFloat("CurrentTemp") : Float.NaN;
            currentH = tag.contains("CurrentHumidity") ? tag.getFloat("CurrentHumidity") : Float.NaN;
        }
    }
}
