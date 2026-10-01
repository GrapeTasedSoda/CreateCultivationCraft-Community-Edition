package euphy.upo.create_cultivation.content.greenhouse;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;

import euphy.upo.create_cultivation.registry.CCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Lets players encase Create's glass fluid pipe with greenhouse glass,
 * producing our greenhouse_glass_fluid_pipe (window pipe inside a glass
 * shell). Create's GlassFluidPipeBlock does not implement EncasableBlock, so
 * the vanilla encasing registry path is unavailable - this handler mirrors
 * EncasableBlock.tryEncase semantics instead: one glass consumed outside
 * creative, flows cached across the swap, axis and waterlogging preserved.
 * Regular fluid pipes encase into greenhouse_fluid_pipe through the vanilla
 * registry path instead.
 */
public final class GreenhouseGlassEncasingHandler {

    private GreenhouseGlassEncasingHandler() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        // Glass pipes: create's window pipe (ours already wears the shell and
        // must not re-encase). Regular pipes are handled by the vanilla
        // EncasableBlock path in FluidPipeBlock.useItemOn.
        boolean isGlassPipe = state.getBlock() instanceof GlassFluidPipeBlock
                && !(state.getBlock() instanceof GreenhouseGlassFluidPipeBlock);
        if (!isGlassPipe)
            return;

        ItemStack held = event.getItemStack();
        if (!held.is(CCBlocks.GREENHOUSE_GLASS.get().asItem()))
            return;

        Player player = event.getEntity();
        if (level.isClientSide) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        Axis axis = state.getValue(BlockStateProperties.AXIS);
        BlockState target = CCBlocks.GREENHOUSE_GLASS_FLUID_PIPE.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, axis)
                .setValue(GreenhouseGlassFluidPipeBlock.FROZEN_AXIS, axis)
                .setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));

        FluidTransportBehaviour.cacheFlows(level, pos);
        level.setBlockAndUpdate(pos, target);
        FluidTransportBehaviour.loadFlows(level, pos);

        if (!player.getAbilities().instabuild)
            held.shrink(1);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
