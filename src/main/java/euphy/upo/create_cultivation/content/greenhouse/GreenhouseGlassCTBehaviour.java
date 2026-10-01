package euphy.upo.create_cultivation.content.greenhouse;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Family connected textures for the greenhouse glass surface: greenhouse glass
 * blocks and both greenhouse pipe shells read as one continuous pane.
 *
 * <p>Two duties:
 * <ul>
 * <li>{@link #skipRendering} removes interior faces outright - where a family
 * block touches another family block, neither draws a pane there, so pipe
 * shells merge with the glass wall the way glass blocks merge with each other
 * (the pipe core inside stays visible through the gap).</li>
 * <li>VERTICAL CT with the hard two-block pairing cap still applies to every
 * rendered face: shared edges drop the frame, exposed edges keep it. Pairing
 * is relative to the contiguous vertical run of FAMILY blocks, not world Y
 * parity: the scan counts how many family blocks sit directly below, even
 * indices connect upwards, odd indices connect downwards. Both blocks of a
 * shared edge derive the same answer, so the two face textures always agree.
 * Top and bottom faces get no connected textures at all.</li>
 * </ul>
 *
 * <p>Pipe shell panels are single world-aligned faces with cullfaces, exactly
 * like the glass block's own faces, so the vanilla face culling this behaviour
 * relies on works identically for all family members.
 */
public class GreenhouseGlassCTBehaviour extends ConnectedTextureBehaviour {

    private static final int MAX_RUN_SCAN = 256;

    private final CTSpriteShiftEntry shift;

    public GreenhouseGlassCTBehaviour(CTSpriteShiftEntry shift) {
        this.shift = shift;
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        // Same pattern as WeatheredIronWindowCTBehaviour: no CT remap on
        // vertical faces (their texture-vertical is a horizontal world axis).
        return direction.getAxis().isVertical() ? null : shift;
    }

    @Override
    public CTType getDataType(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis().isVertical() ? null : AllCTTypes.VERTICAL;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
            BlockPos otherPos, Direction face, Direction primaryOffset, Direction secondaryOffset) {
        if (isBeingBlocked(state, reader, pos, otherPos, face) || !isFamily(other.getBlock())) {
            return false;
        }
        // Only reached for side faces. On side faces the texture-vertical
        // direction is world UP/DOWN.
        boolean checkingUp = secondaryOffset == Direction.UP;
        boolean checkingDown = secondaryOffset == Direction.DOWN;
        if (!checkingUp && !checkingDown) {
            return true;
        }
        int index = runIndex(reader, pos);
        return checkingUp ? index % 2 == 0 : index % 2 == 1;
    }

    private static boolean isFamily(Block block) {
        return block instanceof GreenhouseGlassBlock
                || block instanceof GreenhouseFluidPipeBlock
                || block instanceof GreenhouseGlassFluidPipeBlock;
    }

    /**
     * Zero-based index of this block inside its contiguous vertical run of
     * family glass (0 = the run's bottom block). Scans the whole family so
     * mixed glass/pipe columns still pair consistently.
     */
    private int runIndex(BlockAndTintGetter reader, BlockPos pos) {
        int index = 0;
        BlockPos cursor = pos.below();
        while (index < MAX_RUN_SCAN && isFamily(reader.getBlockState(cursor).getBlock())) {
            index++;
            cursor = cursor.below();
        }
        return index;
    }
}
