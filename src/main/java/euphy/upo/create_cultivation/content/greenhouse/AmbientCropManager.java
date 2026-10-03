package euphy.upo.create_cultivation.content.greenhouse;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.climate.ClimateResolver;
import euphy.upo.create_cultivation.content.climate.CropClimate;
import euphy.upo.create_cultivation.content.climate.CropState;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Ambient (outdoor) climate effects on soil-planted crops - the optional
 * counterpart to the greenhouse system. While {@code CCConfig.AMBIENT_CROPS_ENABLED}
 * is on, this manager periodically rescans its registered crops, evaluates
 * them against a FRESHLY RESOLVED biome climate (the same resolution the
 * greenhouse uses as its baseline) and stores the resulting boost in the
 * tracker's AMBIENT map.
 *
 * <p>Priority: the consumer (growth mixin / harvest handler) checks the
 * greenhouse map first, then {@link GreenhouseCropTracker#isProtected} - a
 * crop inside any powered greenhouse never reaches the ambient evaluation,
 * and stale ambient entries for positions that have become protected are
 * dropped lazily on the next refresh. Registrations are memory only and are
 * wiped when the server stops (see {@link #clearAllServerState()}), so
 * nothing leaks across worlds in the same JVM session; entries re-register
 * as their chunks tick. Off by default.
 */
public final class AmbientCropManager {

    /** dimension -> packed positions of the watched outdoor crops. */
    private static final Map<ResourceKey<Level>, Set<Long>> WATCHED = new ConcurrentHashMap<>();

    private static int tickCounter;

    private AmbientCropManager() {}

    /** Called from the server tick event; no-ops while the feature is disabled. */
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            serverTick(level);
        }
    }

    /** Called from the server tick event; no-ops while the feature is disabled. */
    public static void serverTick(ServerLevel level) {
        boolean enabled = CCConfig.AMBIENT_CROPS_ENABLED.get();
        if (!enabled) {
            // feature turned off: drop every ambient boost tracked for this
            // dimension, both the watch list and the registrations the
            // mixin/harvest handler consult
            ResourceKey<Level> key = level.dimension();
            if (WATCHED.remove(key) != null) {
                GreenhouseCropTracker.clearDimensionAmbient(key);
            }
            return;
        }
        if (++tickCounter % CCConfig.AMBIENT_RECHECK_INTERVAL_TICKS.get() != 0) {
            return;
        }
        refreshDimension(level);
    }

    private static void refreshDimension(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        Set<Long> dim = WATCHED.get(key);
        if (dim == null || dim.isEmpty()) {
            return;
        }
        Iterator<Long> it = dim.iterator();
        while (it.hasNext()) {
            BlockPos pos = BlockPos.of(it.next());
            if (!level.hasChunkAt(pos)) {
                continue; // unloaded chunk: skip without force-loading it
            }
            BlockState state = level.getBlockState(pos);
            CropClimate climate = state.getBlockHolder().getData(CCDataMaps.CROP_CLIMATE);
            if (climate == null || GreenhouseCropTracker.isProtected(key, pos)) {
                // crop gone (harvested, broken, replaced) or swallowed by a
                // greenhouse: forget it
                it.remove();
                GreenhouseCropTracker.clearAmbient(key, pos);
                continue;
            }
            // re-resolve the biome climate EVERY cycle so season-style mods
            // and live config edits are honoured
            ClimateResolver.Climate ambient = ClimateResolver.resolve(level, level.getBiome(pos));
            register(key, pos, climate, ambient.tempC(), ambient.humidity());
        }
    }

    /**
     * Registers/refreshes one outdoor crop (lazily, from the growth mixin).
     */
    private static void noteCrop(ServerLevel level, BlockPos pos, BlockState state) {
        CropClimate climate = state.getBlockHolder().getData(CCDataMaps.CROP_CLIMATE);
        if (climate == null) {
            return;
        }
        ResourceKey<Level> key = level.dimension();
        Set<Long> dim = WATCHED.computeIfAbsent(key, d -> ConcurrentHashMap.newKeySet());
        if (!dim.add(pos.asLong())) {
            return; // already watched; the next refresh re-evaluates it
        }
        ClimateResolver.Climate ambient = ClimateResolver.resolve(level, level.getBiome(pos));
        register(key, pos, climate, ambient.tempC(), ambient.humidity());
    }

    /**
     * The boost to apply for an outdoor crop, or null to run vanilla. Called
     * from the growth mixin when the greenhouse map has no entry: registers
     * unknown crops into the ambient system lazily (only while the feature is
     * enabled) and returns the stored ambient boost.
     */
    @Nullable
    public static GreenhouseCropTracker.Boost resolveOutdoor(ServerLevel level, BlockPos pos, BlockState state) {
        if (!CCConfig.AMBIENT_CROPS_ENABLED.get()) {
            return null;
        }
        if (GreenhouseCropTracker.isProtected(level.dimension(), pos)) {
            return null; // inside a greenhouse interior: vanilla until its scan covers it
        }
        noteCrop(level, pos, state);
        return GreenhouseCropTracker.getAmbient(level.dimension(), pos);
    }

    /** Drops the whole watch list (server stopped / world switched). */
    public static void clearAllServerState() {
        WATCHED.clear();
    }

    private static void register(ResourceKey<Level> key, BlockPos pos, CropClimate climate,
            float tempC, float humidity) {
        CropState state = climate.evaluate(tempC, humidity);
        float growth;
        double yield;
        switch (state) {
            case OPTIMAL -> {
                growth = CCConfig.AMBIENT_OPTIMAL_GROWTH.get().floatValue();
                yield = CCConfig.AMBIENT_OPTIMAL_YIELD.get();
            }
            case PARTIAL -> {
                growth = CCConfig.AMBIENT_PARTIAL_GROWTH.get().floatValue();
                yield = CCConfig.AMBIENT_PARTIAL_YIELD.get();
            }
            case SURVIVAL_ONLY -> {
                growth = CCConfig.AMBIENT_SURVIVAL_ONLY_GROWTH.get().floatValue();
                yield = CCConfig.AMBIENT_SURVIVAL_ONLY_YIELD.get();
            }
            default -> {
                growth = 0.0f;
                yield = 0.0;
            }
        }
        GreenhouseCropTracker.registerAmbient(key, pos, growth, yield, state == CropState.FAIL);
    }
}
