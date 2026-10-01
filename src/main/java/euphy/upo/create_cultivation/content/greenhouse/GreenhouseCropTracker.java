package euphy.upo.create_cultivation.content.greenhouse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import euphy.upo.create_cultivation.infrastructure.network.GreenhouseSnapshotPayload;

/**
 * Server-side registry of per-crop climate boosts, keyed by dimension +
 * packed BlockPos and holding only server-relevant crops.
 *
 * <p>Two boost sources live here with a strict priority: a crop registered
 * by a powered greenhouse controller (greenhouse climate) always wins, and
 * the controller's scanned interior cells are registered as a
 * <em>protected area</em> so the ambient (outdoor) system never touches
 * anything inside a greenhouse - including freshly planted crops that are
 * not yet part of a scan. Only crops outside every protected area fall back
 * to the ambient registration, and only while the ambient feature is
 * enabled in the config. Everything absent from both maps behaves vanilla.
 *
 * <p>Data lives in memory only: greenhouse registrations are rebuilt from
 * the next controller scan and wiped when a controller stops controlling;
 * ambient registrations are refreshed on their own schedule by
 * {@code AmbientCropManager}. Cultivation tanks never register here - their
 * growth is machine-owned.
 */
public final class GreenhouseCropTracker {

    /** Climate boost of one registered crop. */
    public record Boost(float growthMultiplier, double yieldMultiplier, boolean stalled) {
    }

    private static final Map<ResourceKey<Level>, Map<Long, Boost>> CROPS = new ConcurrentHashMap<>();

    /** Outdoor crops under ambient climate effects (only when the feature is enabled). */
    private static final Map<ResourceKey<Level>, Map<Long, Boost>> AMBIENT = new ConcurrentHashMap<>();

    /**
     * Greenhouse interiors, keyed by controller position so each controller
     * manages its own protected area (replace on scan, remove on shutdown).
     */
    private static final Map<ResourceKey<Level>, Map<Long, Set<Long>>> PROTECTED = new ConcurrentHashMap<>();

    private GreenhouseCropTracker() {}

    /** Greenhouse registration: wins over everything else for this position. */
    public static void register(ResourceKey<Level> dimension, BlockPos pos,
            float growthMultiplier, double yieldMultiplier, boolean stalled) {
        CROPS.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>())
                .put(pos.asLong(), new Boost(growthMultiplier, yieldMultiplier, stalled));
    }

    public static void clear(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Boost> dim = CROPS.get(dimension);
        if (dim != null) {
            dim.remove(pos.asLong());
        }
    }

    /** Ambient (outdoor) registration; only consulted when nothing else claims the crop. */
    public static void registerAmbient(ResourceKey<Level> dimension, BlockPos pos,
            float growthMultiplier, double yieldMultiplier, boolean stalled) {
        AMBIENT.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>())
                .put(pos.asLong(), new Boost(growthMultiplier, yieldMultiplier, stalled));
    }

    public static void clearAmbient(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Boost> dim = AMBIENT.get(dimension);
        if (dim != null) {
            dim.remove(pos.asLong());
        }
    }

    /** Replaces one controller's protected interior (called after every scan). */
    public static void setProtectedArea(ResourceKey<Level> dimension, BlockPos controller,
            Set<BlockPos> interior) {
        Set<Long> packed = ConcurrentHashMap.newKeySet(interior.size());
        for (BlockPos p : interior) {
            packed.add(p.asLong());
        }
        PROTECTED.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>())
                .put(controller.asLong(), packed);
    }

    /** Drops one controller's protected area (power loss, broken enclosure, removed). */
    public static void clearProtectedArea(ResourceKey<Level> dimension, BlockPos controller) {
        Map<Long, Set<Long>> dim = PROTECTED.get(dimension);
        if (dim != null) {
            dim.remove(controller.asLong());
        }
    }

    /** True while the position lies inside any powered greenhouse's interior. */
    public static boolean isProtected(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Set<Long>> dim = PROTECTED.get(dimension);
        if (dim == null || dim.isEmpty()) {
            return false;
        }
        long packed = pos.asLong();
        for (Set<Long> area : dim.values()) {
            if (area.contains(packed)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Boost get(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Boost> dim = CROPS.get(dimension);
        return dim == null ? null : dim.get(pos.asLong());
    }

    /** Client-only mirror of the protected-area check (same map, same keys). */
    public static boolean isProtectedClient(ResourceKey<Level> dimension, BlockPos pos) {
        return isProtected(dimension, pos);
    }

    /** Ambient boost of a position, or null. Caller must check {@link #isProtected} first. */
    @Nullable
    public static Boost getAmbient(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Boost> dim = AMBIENT.get(dimension);
        return dim == null ? null : dim.get(pos.asLong());
    }

    /** Drops every ambient registration of one dimension (unloaded / server stop). */
    public static void clearDimensionAmbient(ResourceKey<Level> dimension) {
        AMBIENT.remove(dimension);
    }

    /**
     * True while the climate system currently holds the crop at this
     * position in the stalled state (outside both survival ranges). Greenhouse
     * registrations win; outdoor crops fall back to the ambient registry,
     * which only exists while the ambient feature is enabled.
     */
    public static boolean isStalled(ResourceKey<Level> dimension, BlockPos pos) {
        Boost b = get(dimension, pos);
        if (b == null && !isProtected(dimension, pos)) {
            b = getAmbient(dimension, pos);
        }
        return b != null && b.stalled();
    }

    // ------------------------------------------------------------------
    // Client view (Jade): the controller's periodic snapshot broadcast
    // carries the boost entries; the client caches them for tooltips.
    // ------------------------------------------------------------------

    /** Client-side boost snapshot with the game time it was received at. */
    public record ClientBoost(float growthMultiplier, double yieldMultiplier, boolean stalled, long receivedAt) {
    }

    /** Entries older than this (ticks) are treated as stale and ignored. */
    public static final long CLIENT_VIEW_TTL_TICKS = 80;

    private static final Map<ResourceKey<Level>, Map<Long, ClientBoost>> CLIENT_VIEW = new ConcurrentHashMap<>();

    /** Client only: fold one snapshot's crop boosts into the tooltip cache. */
    public static void applySnapshotClient(ResourceKey<Level> dimension, BlockPos controller,
            java.util.List<GreenhouseSnapshotPayload.CropBoost> boosts, long now) {
        Map<Long, ClientBoost> dim = CLIENT_VIEW.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>());
        if (!boosts.isEmpty()) {
            for (GreenhouseSnapshotPayload.CropBoost b : boosts) {
                // wire format is fixed point (x1000): convert back to the
                // real multipliers before caching for the tooltip
                dim.put(b.packedPos(), new ClientBoost(b.growth() / 1000.0f,
                        b.yield() / 1000.0, b.stalled(), now));
            }
        }
        if ((now & 63) == 0) {
            dim.values().removeIf(b -> now - b.receivedAt() > CLIENT_VIEW_TTL_TICKS);
        }
    }

    /**
     * Client only: the greenhouse boost shown for a crop, or null when the
     * position is not (or no longer freshly) covered by a controller broadcast.
     */
    @Nullable
    public static ClientBoost getForClient(ResourceKey<Level> dimension, BlockPos pos, long now) {
        Map<Long, ClientBoost> dim = CLIENT_VIEW.get(dimension);
        if (dim == null) {
            return null;
        }
        ClientBoost b = dim.get(pos.asLong());
        if (b == null || now - b.receivedAt() > CLIENT_VIEW_TTL_TICKS) {
            return null;
        }
        return b;
    }

    /** Clears every server-side registry (server stopped / world switched). */
    public static void clearAllServerState() {
        CROPS.clear();
        AMBIENT.clear();
        PROTECTED.clear();
    }

    /** Clears the client-side Jade cache (disconnect / world switch). */
    public static void clearClientState() {
        CLIENT_VIEW.clear();
    }
}
