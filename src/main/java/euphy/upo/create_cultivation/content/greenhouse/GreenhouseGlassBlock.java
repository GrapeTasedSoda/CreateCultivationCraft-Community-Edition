package euphy.upo.create_cultivation.content.greenhouse;

import com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Greenhouse glass: behaves like Create's framed glass (connected textures,
 * hidden internal faces between glass blocks) with a custom vertical-only,
 * two-block-max pairing behaviour - see {@link GreenhouseGlassCTBehaviour}.
 */
public class GreenhouseGlassBlock extends ConnectedGlassBlock {

    public GreenhouseGlassBlock(Properties properties) {
        super(properties);
    }

    /** Hide interior faces against the whole greenhouse glass family. */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return isFamily(adjacent.getBlock()) || super.skipRendering(state, adjacent, side);
    }

    /**
     * The greenhouse glass family: glass blocks and both greenhouse pipe
     * shells read as one continuous pane. Interior faces between family
     * members are culled outright (Block.skipRendering), and shared edges get
     * frameless CT through the family behaviour.
     */
    public static boolean isFamily(Block block) {
        return block instanceof GreenhouseGlassBlock
                || block instanceof GreenhouseFluidPipeBlock
                || block instanceof GreenhouseGlassFluidPipeBlock;
    }
}
