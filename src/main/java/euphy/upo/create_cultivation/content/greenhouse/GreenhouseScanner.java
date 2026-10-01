package euphy.upo.create_cultivation.content.greenhouse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.registry.CCBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flood-fill scanner that decides whether the blocks around a greenhouse
 * controller form a closed enclosure, and measures it.
 *
 * <p>Rules:
 * <ul>
 * <li>Blocks in {@code create_cultivation:greenhouse_boundary} seal the
 * enclosure and count towards the volume - walls AND ceiling. Greenhouse glass
 * doors are part of this tag; an open sliding door still occupies its block
 * cell, so open/closed makes no difference to the scan.</li>
 * <li>The fill starts at the controller cell and spreads through air and
 * liquids.</li>
 * <li>Any full opaque block seals the fill - the floor may be built from any
 * material, and machines or furniture placed inside the greenhouse are
 * perfectly fine. An untagged full block is only rejected when it actually
 * acts as a wall or ceiling: after the fill, each candidate is checked for a
 * direct face touching open space that the fill never reached (outside air or
 * liquid). Interior furniture is surrounded by visited interior air or the
 * floor below, so it passes; a block replacing a wall or ceiling pane faces
 * the open sky and fails. The floor itself is reached from above and is
 * exempt. Mod devices ({@code greenhouse_device}) are always exempt.</li>
 * <li>Partial blocks (crops, farmland, vanilla glass, slabs, ...) reached from
 * the side stop the fill without a leak; they seal nothing but are common
 * interior clutter. Partial cells entered from ABOVE (the fill falls onto
 * tilled soil or a crop) never spread sideways - the flood only continues
 * downwards through them - so a tilled floor cannot leak around the wall
 * bases.</li>
 * <li>Leaking out of the world height or exceeding {@link #MAX_VOLUME} also
 * fails the scan. Fill cells inside unloaded chunks read as air, which makes
 * an over-large fill run into the volume cap - greenhouses spanning unloaded
 * chunks are therefore invalid until everything is loaded.</li>
 * </ul>
 */
public final class GreenhouseScanner {

    /** Hard cap on the measured volume (interior + boundary + floor). */
    public static final int MAX_VOLUME = 2048;

    /** Result of one scan. Immutable. */
    public record ScanResult(boolean valid, int volume, List<BlockPos> devices, List<BlockPos> crops,
            Set<BlockPos> interior) {

        public static final ScanResult INVALID = new ScanResult(false, 0, List.of(), List.of(), Set.of());
    }

    /** Fill cell; {@code downOnly} cells (floor material entered from above) may only expand downwards. */
    private record Cell(BlockPos pos, boolean downOnly) {}

    private GreenhouseScanner() {}

    /**
     * Scans the enclosure around {@code controller}. The controller cell
     * itself is always treated as passable so the machine may sit anywhere
     * inside the greenhouse.
     */
    public static ScanResult scan(BlockGetter level, BlockPos controller) {
        Deque<Cell> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> devices = new ArrayList<>();
        List<BlockPos> crops = new ArrayList<>();
        // untagged full blocks the fill met from the side or below - checked
        // after the fill for "is this actually an exterior wall?" via probe
        List<BlockPos> shellChecks = new ArrayList<>();
        int volume = 0;

        queue.add(new Cell(controller.immutable(), false));
        visited.add(controller);

        while (!queue.isEmpty()) {
            Cell cell = queue.poll();
            BlockPos pos = cell.pos();

            for (Direction direction : Direction.values()) {
                if (cell.downOnly() && direction != Direction.DOWN)
                    continue; // floor material: never creep sideways under the walls

                BlockPos next = pos.relative(direction);
                if (next.getY() < level.getMinBuildHeight() || next.getY() >= level.getMaxBuildHeight())
                    return ScanResult.INVALID; // leaked out of the world
                if (!visited.add(next))
                    continue;

                BlockState state = level.getBlockState(next);

                if (state.is(CCBlockTags.GREENHOUSE_BOUNDARY)) {
                    volume++;
                    if (volume > MAX_VOLUME)
                        return ScanResult.INVALID;
                    continue;
                }

                boolean device = state.is(CCBlockTags.GREENHOUSE_DEVICE);
                boolean crop = isCrop(state);

                if (state.isSolidRender(level, next)) {
                    // Full opaque block: always seals, never a leak by itself.
                    // Floor (reached from above) is exempt from the shell check;
                    // side/ceiling candidates are probed after the fill.
                    volume++;
                    if (volume > MAX_VOLUME)
                        return ScanResult.INVALID;
                    if (device)
                        devices.add(next.immutable());
                    if (!device && direction != Direction.DOWN)
                        shellChecks.add(next.immutable());
                    continue;
                }

                boolean airLike = state.isAir() || !state.getFluidState().isEmpty();
                if (!airLike && direction != Direction.DOWN) {
                    // Partial block hit from the side (plants, farmland, glass,
                    // slabs, ...): the fill stops here - never a leak.
                    if (device)
                        devices.add(next.immutable());
                    if (crop)
                        crops.add(next.immutable());
                    continue;
                }
                if (cell.downOnly() && airLike) {
                    // A floor-material cell hanging over open space: stop, do
                    // not turn the cavity below into part of the enclosure.
                    continue;
                }

                // Fully passable (air, liquids) or floor material entered from
                // above (farmland, planted crops, ...).
                volume++;
                if (volume > MAX_VOLUME)
                    return ScanResult.INVALID;
                if (device)
                    devices.add(next.immutable());
                if (crop)
                    crops.add(next.immutable());
                queue.add(new Cell(next.immutable(), !airLike));
            }

            if (volume > MAX_VOLUME)
                return ScanResult.INVALID;
        }

        // An untagged full block is a leak only if it directly faces open
        // space the fill never reached - i.e. it acts as a wall or ceiling.
        // Interior furniture is surrounded by visited air / the floor below:
        // it passes. The check runs after the fill so every interior air cell
        // is already visited; no cluster walking, so the ground below the
        // floor is never probed.
        for (BlockPos shell : shellChecks) {
            if (facesOpenSpace(level, shell, visited))
                return ScanResult.INVALID;
        }

        return new ScanResult(true, volume, List.copyOf(devices), List.copyOf(crops),
                Set.copyOf(visited));
    }

    /** True if any of the six direct faces of {@code pos} touches outside air or liquid. */
    private static boolean facesOpenSpace(BlockGetter level, BlockPos pos, Set<BlockPos> visited) {
        for (Direction direction : Direction.values()) {
            BlockPos n = pos.relative(direction);
            if (n.getY() < level.getMinBuildHeight() || n.getY() >= level.getMaxBuildHeight())
                return true; // build-limit or void side: open
            if (visited.contains(n))
                continue; // enclosure side
            BlockState state = level.getBlockState(n);
            if (state.isAir() || !state.getFluidState().isEmpty())
                return true;
            // solid or partial neighbours are neither open nor interesting
        }
        return false;
    }

    private static boolean isCrop(BlockState state) {
        // a crop is exactly a block carrying a crop_climate data-map entry;
        // vanilla crops (wheat, carrots, ...) have no block entity, and
        // BE-based modded crops are registered through the same data map, so
        // the lookup stands alone - never gate it on hasBlockEntity()
        return state.getBlockHolder().getData(CCDataMaps.CROP_CLIMATE) != null;
    }
}
