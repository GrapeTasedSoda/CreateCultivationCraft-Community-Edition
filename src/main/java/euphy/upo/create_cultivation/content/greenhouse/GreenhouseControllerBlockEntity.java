package euphy.upo.create_cultivation.content.greenhouse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import euphy.upo.create_cultivation.content.airconditioner.AirConditionerBlock;
import euphy.upo.create_cultivation.content.climate.ClimateInterval;
import euphy.upo.create_cultivation.content.climate.ClimateResolver;
import euphy.upo.create_cultivation.content.climate.ClimateUnits;
import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.climate.CropClimate;
import euphy.upo.create_cultivation.content.climate.CropState;
import euphy.upo.create_cultivation.content.dehumidifier.DehumidifierBlock;
import euphy.upo.create_cultivation.content.humidifier.HumidifierBlock;
import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.registry.CCAdvancementTriggers;
import euphy.upo.create_cultivation.infrastructure.network.GreenhouseSnapshotPayload;

import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

/**
 * Kinetic block entity for the greenhouse controller.
 *
 * <p>Display-link bulb pattern for the halo, periodic enclosure scans, and the
 * data behind the GUI: a snapshot payload (scan stats, current climate, crop
 * rows) is pushed to any open menu on every scan, and the player's target
 * temperature/humidity setpoints are stored server-side (NBT persisted).
 */
public class GreenhouseControllerBlockEntity extends KineticBlockEntity implements MenuProvider {

    public static final float GLOW_RADIUS_THRESHOLD = 0.125f;

    /** Lamp signalling: green flashes once per this many ticks (5 s). */
    public static final int GREEN_BLINK_PERIOD = 100;
    /** How long (ticks) the green flash stays visible. */
    public static final int GREEN_BLINK_ON = 8;

    /** Server ticks between automatic enclosure scans while powered. */
    
    /** Default target temperature when no player setting exists (deg C). */
    public static final float DEFAULT_SET_TEMP = 20.0f;
    /** Default target humidity when no player setting exists (%RH). */
    public static final float DEFAULT_SET_HUMIDITY = 50.0f;

    /** Whether the controller successfully sees a greenhouse (drives green blink). */
    private boolean lampWorking;
    /** Whether connected devices cannot reach the setpoints (yellow steady). */
    private boolean lampShort;

    /** Previous working state, for the activation advancement edge. */
    private boolean wasWorking;

    @Nullable
    private GreenhouseScanner.ScanResult lastScan;
    private int scanCooldown;
    private boolean wasPowered;

    /** Player-set target temperature (deg C). */
    private float setTempC = DEFAULT_SET_TEMP;
    /** Player-set target humidity (%RH). */
    private float setHumidity = DEFAULT_SET_HUMIDITY;
    /** Last resolved climate reading, cached for the GUI. */
    @Nullable
    private ClimateResolver.Climate lastClimate;

    /** Server-side live climate + feasibility cache. */
    private final ClimateController.ClimateState climate = new ClimateController.ClimateState();

    /** Devices of the last scan, classified by type. */
    @Nullable
    private ClimateController.DeviceGroups lastDeviceGroups;

    /** Live climate + feasibility cache for the Jade provider. */
    public ClimateController.ClimateState getClimateState() {
        return climate;
    }

    public GreenhouseControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        boolean powered = getSpeed() != 0;
        updateWorkingState(powered);

        if (level == null || level.isClientSide())
            return;

        // Scanning requires kinetic power. Scan once right after power-on,
        // then periodically to track greenhouse edits and door changes.
        if (powered) {
            if (!wasPowered || scanCooldown <= 0) {
                runScan();
                scanCooldown = CCConfig.SCAN_INTERVAL_TICKS.get();
            } else {
                scanCooldown--;
            }
            lastClimate = ClimateResolver.resolve(level, level.getBiome(worldPosition));
        }

        // Climate simulation every tick (server side only, level is non-null here)
        var deviceGroups = lastDeviceGroups != null ? lastDeviceGroups : ClimateController.DeviceGroups.EMPTY;
        int volume = lastScan != null && lastScan.valid() ? lastScan.volume() : 0;
        boolean controlling = powered && volume > 0 && climate.isInitialised();
        if (!climate.isInitialised() && lastClimate != null) {
            // first contact: the greenhouse starts at the ambient climate
            climate.snapTo(lastClimate.tempC(), lastClimate.humidity());
        }
        ClimateController.step((net.minecraft.server.level.ServerLevel) level, climate,
                getAmbientTemp(), getAmbientHumidity(), setTempC, setHumidity,
                volume, deviceGroups, controlling);

        // crop climate boosts: evaluate each scanned soil crop against the
        // live climate and register it in the global tracker
        if (controlling) {
            applyCropClimateBoosts();
        } else if (lastScan != null && (level.getGameTime() & 19) == 0) {
            // no longer controlling (power loss, broken enclosure, removed):
            // stop boosting/stalling the previously registered crops
            clearCropRegistrations();
        }

        // lamp signalling: green = healthy blink once per 5 s, yellow =
        // steady "connected devices cannot reach the setpoints"; both dark
        // when the controller is not working. State syncs via the block
        // entity's update packet.
        boolean newWorking = powered && lastScan != null && lastScan.valid();
        boolean newShort = controlling && (climate.isTempShort() || climate.isHumShort());
        if (newWorking != lampWorking || newShort != lampShort) {
            lampWorking = newWorking;
            lampShort = newShort;
            sendData();
        }

        // advancement: the first tick this controller is seen running with a
        // recognised greenhouse grants "Off-Season Agriculture" to the players
        // present
        if (newWorking && !wasWorking) {
            CCAdvancementTriggers.ACTIVATE_GREENHOUSE_CONTROLLER.awardNearby((net.minecraft.server.level.ServerLevel) level, getBlockPos());
        }
        wasWorking = newWorking;

        // push the live climate to open GUIs once per second - also while
        // not controlling, so an open GUI keeps showing live (idle) data
        // instead of freezing on the last snapshot after a power loss
        if ((level.getGameTime() & 19) == 0)
            notifySnapshot();
        wasPowered = powered;
    }

    /**
     * Evaluates every scanned soil-planted crop against the live greenhouse
     * climate and registers the result in the global tracker that the vanilla
     * growth mixin and the harvest-drop handler consult. Cultivation tanks
     * (mod machine crops) are deliberately untouched - their growth is owned
     * by the tank itself and must not react to the greenhouse climate.
     */
    private void applyCropClimateBoosts() {
        if (lastScan == null || !lastScan.valid()) {
            return;
        }
        float liveTemp = (float) climate.getCurrentTemp();
        float liveHum = (float) climate.getCurrentHumidity();
        List<BlockPos> stale = null;
        for (BlockPos cropPos : lastScan.crops()) {
            // stagger: re-evaluate each crop once every 16 ticks (bucketed by
            // position) - the tracker keeps the previous boost in between, the
            // climate moves gradually, and the per-tick cost drops ~16x
            if (((level.getGameTime() + cropPos.asLong()) & 15) != 0) {
                continue;
            }
            BlockState cropState = level.getBlockState(cropPos);
            CropClimate cropClimate = cropState.getBlockHolder().getData(CCDataMaps.CROP_CLIMATE);
            if (cropClimate == null) {
                // vanished crop: schedule removal of a stale registration
                if (stale == null) stale = new ArrayList<>();
                stale.add(cropPos);
                continue;
            }
            CropState state = cropClimate.evaluate(liveTemp, liveHum);
            float growth;
            double yield;
            switch (state) {
                case OPTIMAL -> {
                    growth = CCConfig.CLIMATE_OPTIMAL_GROWTH.get().floatValue();
                    yield = CCConfig.CLIMATE_OPTIMAL_YIELD.get();
                }
                case PARTIAL -> {
                    growth = CCConfig.CLIMATE_SURVIVAL_GROWTH.get().floatValue();
                    yield = CCConfig.CLIMATE_SURVIVAL_YIELD.get();
                }
                case SURVIVAL_ONLY -> {
                    growth = CCConfig.CLIMATE_SURVIVAL_ONLY_GROWTH.get().floatValue();
                    yield = CCConfig.CLIMATE_SURVIVAL_ONLY_YIELD.get();
                }
                default -> {
                    growth = 0.0f;
                    yield = 0.0;
                }
            }
            GreenhouseCropTracker.register(level.dimension(), cropPos, growth, yield,
                    state == CropState.FAIL);
        }
        if (stale != null) {
            for (BlockPos p : stale)
                GreenhouseCropTracker.clear(level.dimension(), p);
        }
    }

    /** Unregisters every crop this controller had boosted (power loss etc.). */
    private void clearCropRegistrations() {
        if (lastScan == null) {
            return;
        }
        for (BlockPos cropPos : lastScan.crops()) {
            GreenhouseCropTracker.clear(level.dimension(), cropPos);
        }
        GreenhouseCropTracker.clearProtectedArea(level.dimension(), worldPosition);
    }

    /**
     * Block broken (or replaced): drop this controller's tracker
     * registrations and its protected interior immediately, so crops and the
     * ambient system do not keep reacting to a greenhouse that no longer
     * exists. The registrations are memory-only and are rebuilt from the
     * next controller's scans. ({@code setRemoved} is final in Create's
     * {@code SmartBlockEntity}; {@code remove} is its overridable hook.)
     */
    @Override
    public void remove() {
        super.remove();
        if (level != null && !level.isClientSide()) {
            clearCropRegistrations();
        }
    }

    /** Chunk unloading: same cleanup as removal (state is memory-only). */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide()) {
            clearCropRegistrations();
        }
    }

    private void runScan() {
        GreenhouseScanner.ScanResult oldScan = lastScan;
        GreenhouseScanner.ScanResult result = GreenhouseScanner.scan(level, worldPosition);
        if (lastScan == null || !lastScan.equals(result)) {
            lastScan = result;
            setChanged();
        } else {
            lastScan = result;
        }
        lastDeviceGroups = classifyDevices(result);
        // crop list changes retrigger the auto setpoint solver
        applyAutoOnCropChange();
        // tracker bookkeeping: never leave stale registrations behind when
        // the enclosure shrinks or breaks - crops dropped from the scan
        // would otherwise keep their greenhouse boost (or stall) forever,
        // because the not-controlling fallback below only sees the NEW
        // (empty) crop list
        if (result.valid()) {
            GreenhouseCropTracker.setProtectedArea(level.dimension(), worldPosition, result.interior());
            if (oldScan != null && oldScan.valid()) {
                for (BlockPos p : oldScan.crops()) {
                    if (!result.crops().contains(p)) {
                        GreenhouseCropTracker.clear(level.dimension(), p);
                    }
                }
            }
        } else {
            GreenhouseCropTracker.clearProtectedArea(level.dimension(), worldPosition);
            if (oldScan != null) {
                for (BlockPos p : oldScan.crops()) {
                    GreenhouseCropTracker.clear(level.dimension(), p);
                }
            }
        }
        notifySnapshot();
    }
    /** Splits the scanned device positions into the three machine types. */
    private ClimateController.DeviceGroups classifyDevices(GreenhouseScanner.ScanResult scan) {
        List<BlockPos> acs = new ArrayList<>();
        List<BlockPos> hums = new ArrayList<>();
        List<BlockPos> dehums = new ArrayList<>();
        for (BlockPos pos : scan.devices()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof AirConditionerBlock)
                acs.add(pos);
            else if (state.getBlock() instanceof HumidifierBlock)
                hums.add(pos);
            else if (state.getBlock() instanceof DehumidifierBlock)
                dehums.add(pos);
        }
        return new ClimateController.DeviceGroups(List.copyOf(acs), List.copyOf(hums), List.copyOf(dehums));
    }

    /** Latest enclosure scan result, or null before the first scan. */
    @Nullable
    public GreenhouseScanner.ScanResult getLastScan() {
        return lastScan;
    }

    /** Forces an immediate re-scan (future API hook). */
    public void requestScan() {
        if (level != null && !level.isClientSide() && getSpeed() != 0)
            runScan();
    }

    public float getSetTemp() {
        return setTempC;
    }

    public float getSetHumidity() {
        return setHumidity;
    }

    /** Stores new player setpoints (already clamped) and syncs the open menu. */
    public void setSetpoints(float tempC, float humidity, int autoMode) {
        // an auto axis is owned by the crop solver: the manual setpoint part
        // of a commit never clobbers it; switching an axis to auto right here
        // runs the solver once so the machine applies the optimum on save
        int appliedAuto = autoMode & (AUTO_TEMP | AUTO_HUM);
        boolean humTurnedOn = (appliedAuto & AUTO_HUM) != 0 && !isAutoHum();
        boolean tempTurnedOn = (appliedAuto & AUTO_TEMP) != 0 && !isAutoTemp();
        this.autoMode = appliedAuto;
        if (!isAutoTemp())
            this.setTempC = ClimateUnits.clampTempC(tempC);
        if (!isAutoHum())
            this.setHumidity = ClimateUnits.clampHumidity(humidity);
        if (tempTurnedOn || humTurnedOn) {
            lastCropSignature = ""; // force re-apply on the next crop scan
            applyAutoSetpoints();
        }
        setChanged();
        notifySnapshot();
    }

    /** Auto mode per axis: the controller keeps setpoints at the crop optimum. */
    public static final int AUTO_NONE = 0;
    public static final int AUTO_TEMP = 1;
    public static final int AUTO_HUM = 2;

    private int autoMode = AUTO_NONE;
    /** Crop list signature of the last auto application (block id x count). */
    private String lastCropSignature = "";

    /** Client toggle request for an axis' auto mode. */
    public void toggleAutoMode(int axis) {
        int next = autoMode ^ axis;
        setAutoMode(next);
    }

    public void setAutoMode(int mode) {
        autoMode = mode & (AUTO_TEMP | AUTO_HUM);
        setChanged();
        notifySnapshot();
        // switching on recalculates immediately from the current crop list;
        // switching off keeps the last auto-set value as the manual setpoint
        applyAutoSetpoints();
    }

    public int getAutoMode() {
        return autoMode;
    }

    public boolean isAutoTemp() {
        return (autoMode & AUTO_TEMP) != 0;
    }

    public boolean isAutoHum() {
        return (autoMode & AUTO_HUM) != 0;
    }

    /**
     * Recomputes the crop-optimum setpoints for the auto axes and writes
     * them; called whenever the crop list changed while auto is active.
     */
    private void applyAutoSetpoints() {
        if (autoMode == AUTO_NONE)
            return;
        GreenhouseSnapshotPayload.CropRow[] rows = buildCropRows(scanCropCounts());
        if (rows.length == 0) {
            return; // no crops: keep last setpoints until something is planted
        }
        if (isAutoTemp()) {
            int t = CropOptimumSolver.bestTempC10(rows);
            if (t != Integer.MIN_VALUE)
                setTempC = ClimateUnits.clampTempC(t / 10f);
        }
        if (isAutoHum()) {
            int h = CropOptimumSolver.bestHum10(rows);
            if (h != Integer.MIN_VALUE)
                setHumidity = ClimateUnits.clampHumidity(h / 10f);
        }
    }

    private Map<Block, Integer> scanCropCounts() {        List<BlockPos> crops = lastScan != null ? lastScan.crops() : List.of();
        Map<Block, Integer> counts = new LinkedHashMap<>();
        for (BlockPos pos : crops) {
            Block block = level.getBlockState(pos).getBlock();
            counts.merge(block, 1, Integer::sum);
        }
        return counts;
    }

    /**
     * While an auto axis is on, any change to the crop list (plant, grow,
     * harvest, remove) retriggers the optimum solver for that axis.
     */
    private void applyAutoOnCropChange() {
        if (autoMode == AUTO_NONE)
            return;
        Map<Block, Integer> counts = scanCropCounts();
        StringBuilder sig = new StringBuilder();
        counts.forEach((block, count) -> sig.append(BuiltInRegistries.BLOCK.getKey(block)).append('x')
                .append(count).append(';'));
        String signature = sig.toString();
        if (signature.equals(lastCropSignature))
            return;
        lastCropSignature = signature;
        applyAutoSetpoints();
    }

    public float getAmbientTemp() {
        return lastClimate != null ? lastClimate.tempC() : ClimateUnits.nativeTempToCelsius(0.5f);
    }

    public float getAmbientHumidity() {
        return lastClimate != null ? lastClimate.humidity() : 50.0f;
    }

    /** Builds the GUI snapshot and pushes it to nearby players (covers open menus). */
    private void notifySnapshot() {
        if (level == null)
            return;
        buildCropRowsAsync();
    }

    /** Builds the current snapshot payload (crop rows + boosts included). */
    private GreenhouseSnapshotPayload buildSnapshotNow() {
        List<BlockPos> crops = lastScan != null ? lastScan.crops() : List.of();
        Map<Block, Integer> counts = new LinkedHashMap<>();
        for (BlockPos pos : crops) {
            Block block = level.getBlockState(pos).getBlock();
            counts.merge(block, 1, Integer::sum);
        }
        return buildSnapshot(buildCropRows(counts));
    }

    /** Sends the current snapshot to exactly one player (the menu opener). */
    public void sendSnapshotToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildSnapshotNow());
    }

    /** Broadcasts to nearby players: open menus + the Jade crop-boost cache. */
    private void buildCropRowsAsync() {
        GreenhouseSnapshotPayload payload = buildSnapshotNow();
        if (level.getServer() != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // every snapshot names its controller; the client keeps only the
            // payload of the machine whose menu it currently has open
            PacketDistributor.sendToPlayersNear(serverLevel, null,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 64.0, payload);
        }
    }

    private GreenhouseSnapshotPayload.CropRow[] buildCropRows(Map<Block, Integer> counts) {
        GreenhouseSnapshotPayload.CropRow[] rows = new GreenhouseSnapshotPayload.CropRow[counts.size()];
        int i = 0;
        for (Map.Entry<Block, Integer> e : counts.entrySet()) {
            Block block = e.getKey();
            CropClimate climate = block.defaultBlockState().getBlockHolder().getData(CCDataMaps.CROP_CLIMATE);
            if (climate == null)
                climate = CropClimate.DEFAULT;
            rows[i++] = new GreenhouseSnapshotPayload.CropRow(
                    BuiltInRegistries.BLOCK.getKey(block).toString(),
                    t10(climate.tempOptimal(), true, false), t10(climate.tempOptimal(), false, false),
                    t10(climate.tempSurvival(), true, false), t10(climate.tempSurvival(), false, false),
                    t10(climate.humidityOptimal(), true, true), t10(climate.humidityOptimal(), false, true),
                    t10(climate.humiditySurvival(), true, true), t10(climate.humiditySurvival(), false, true),
                    e.getValue());
        }
        return rows;
    }

    private GreenhouseSnapshotPayload buildSnapshot(GreenhouseSnapshotPayload.CropRow[] rows) {
        // the flap counter shows total plants (sum of the list rows), not
        // the number of species - it must match what the list adds up to
        int totalPlants = 0;
        for (GreenhouseSnapshotPayload.CropRow row : rows)
            totalPlants += row.count();
        var deviceGroups = lastDeviceGroups != null ? lastDeviceGroups : ClimateController.DeviceGroups.EMPTY;
        return new GreenhouseSnapshotPayload(
                worldPosition,
                lastScan != null && lastScan.valid(),
                lastScan != null ? lastScan.volume() : 0,
                lastScan != null ? lastScan.devices().size() : 0,
                totalPlants,
                Math.round(climate.getCurrentTemp() * 10),
                Math.round(climate.getCurrentHumidity() * 10),
                Math.round(setTempC * 10),
                Math.round(setHumidity * 10),
                climate.isTempShort(),
                climate.isHumShort(),
                Math.round(climate.getReachableTemp() * 10),
                Math.round(climate.getReachableHumidity() * 10),
                deviceGroups.airConditioners().size(),
                deviceGroups.humidifiers().size(),
                deviceGroups.dehumidifiers().size(),
                autoMode,
                rows,
                buildBoosts());
    }

    /** Packs the tracker's greenhouse boosts into the snapshot for Jade. */
    private GreenhouseSnapshotPayload.CropBoost[] buildBoosts() {
        if (lastScan == null || !lastScan.valid()) {
            return new GreenhouseSnapshotPayload.CropBoost[0];
        }
        List<GreenhouseSnapshotPayload.CropBoost> out = new ArrayList<>(lastScan.crops().size());
        for (BlockPos cropPos : lastScan.crops()) {
            GreenhouseCropTracker.Boost b = GreenhouseCropTracker.get(level.dimension(), cropPos);
            if (b != null) {
                out.add(new GreenhouseSnapshotPayload.CropBoost(cropPos.asLong(),
                        Math.round(b.growthMultiplier() * 1000),
                        (int) Math.round(b.yieldMultiplier() * 1000),
                        b.stalled()));
            }
        }
        return out.toArray(new GreenhouseSnapshotPayload.CropBoost[0]);
    }

    private static int t10(java.util.Optional<ClimateInterval> interval, boolean min, boolean humidity) {
        if (interval.isEmpty())
            return Integer.MIN_VALUE; // "no range" marker
        return Math.round((min ? interval.get().min() : interval.get().max()) * 10);
    }

    private void updateWorkingState(boolean powered) {
        if (level == null) {
            return;
        }
        boolean current = getBlockState().getValue(GreenhouseControllerBlock.WORKING);
        if (current != powered) {
            // Flag 2: a pure property flip needs no neighbour updates.
            level.setBlock(worldPosition, getBlockState().setValue(GreenhouseControllerBlock.WORKING, powered), 2);
        }
    }

    /** Whether the controller is working (greenhouse recognised). */
    public boolean isLampWorking() {
        return lampWorking;
    }

    /** Whether the connected devices cannot reach the setpoints. */
    public boolean isLampShort() {
        return lampShort;
    }

    /**
     * Green lamp halo strength: one 8-tick flash every 5 s while working,
     * with a short eased ramp on both edges; always dark otherwise.
     */
    public float getGreenGlow(float partialTicks) {
        if (!lampWorking)
            return 0.0f;
        long t = level == null ? 0 : level.getGameTime();
        long phase = ((t + Math.round(partialTicks)) % GREEN_BLINK_PERIOD);
        return phase < GREEN_BLINK_ON
                ? Mth.clampedLerp(0f, 1f, Math.min(phase + partialTicks, 1.5f))
                : Mth.clampedLerp(1f, 0f, Math.max(0f, phase + partialTicks - GREEN_BLINK_ON));
        }

    /** Yellow lamp halo strength: steady while setpoints are out of reach. */
    public float getYellowGlow(float partialTicks) {
        return lampShort ? 1.0f : 0.0f;
    }

    /** Halo max alpha, shared with the renderer (display link uses 200). */
    public static final int MAX_ALPHA = 200;

    /**
     * Geometric centre of the left bulb housing (authored facing north:
     * 3 x 3 x 3 housing element [3.5,20,4.5]..[6.5,23,7.5]). The renderer
     * rotates this offset with the facing.
     */
    public Vec3 getYellowBulbOffset() {
        return new Vec3(5 / 16.0, 21.5 / 16.0, 6 / 16.0);
    }

    /**
     * Geometric centre of the right bulb housing (authored facing north:
     * 3 x 3 x 3 housing element [9.5,20,4.5]..[12.5,23,7.5]). The renderer
     * rotates this offset with the facing.
     */
    public Vec3 getGreenBulbOffset() {
        return new Vec3(11 / 16.0, 21.5 / 16.0, 6 / 16.0);
    }

    // ---- MenuProvider: right-click opens the controller GUI ----------------

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GreenhouseControllerMenu(containerId, inventory, this);
    }

    // ---- NBT ---------------------------------------------------------------

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (lastScan != null) {
            // Persist validity, volume and device positions so the GUI has
            // data immediately after a world reload.
            tag.putBoolean("ScanValid", lastScan.valid());
            tag.putInt("ScanVolume", lastScan.volume());
            long[] deviceLongs = new long[lastScan.devices().size()];
            for (int i = 0; i < deviceLongs.length; i++)
                deviceLongs[i] = lastScan.devices().get(i).asLong();
            tag.put("ScanDevices", new LongArrayTag(deviceLongs));
        }
        tag.putFloat("SetTemp", setTempC);
        tag.putFloat("SetHumidity", setHumidity);
        tag.putBoolean("LampWorking", lampWorking);
        tag.putBoolean("LampShort", lampShort);
        tag.putInt("AutoMode", autoMode);
        tag.putString("CropSignature", lastCropSignature);
        climate.save(tag);
    }
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        // lamp flags ride every sync packet (they drive the BER halos)
        if (tag.contains("LampWorking"))
            lampWorking = tag.getBoolean("LampWorking");
        if (tag.contains("LampShort"))
            lampShort = tag.getBoolean("LampShort");
        if (tag.contains("AutoMode"))
            autoMode = tag.getInt("AutoMode") & (AUTO_TEMP | AUTO_HUM);
        if (tag.contains("CropSignature"))
            lastCropSignature = tag.getString("CropSignature");
        if (clientPacket)
            return;
        if (tag.contains("ScanValid")) {
            long[] deviceLongs = tag.getLongArray("ScanDevices");
            List<BlockPos> devices = new ArrayList<>(deviceLongs.length);
            for (long packed : deviceLongs)
                devices.add(BlockPos.of(packed));
            lastScan = new GreenhouseScanner.ScanResult(tag.getBoolean("ScanValid"), tag.getInt("ScanVolume"),
                    List.copyOf(devices), List.of(), java.util.Set.of());
        } else {
            lastScan = null;
        }
        setTempC = ClimateUnits.clampTempC(tag.getFloat("SetTemp"));
        setHumidity = ClimateUnits.clampHumidity(tag.getFloat("SetHumidity"));
        climate.load(tag);
    }
}
