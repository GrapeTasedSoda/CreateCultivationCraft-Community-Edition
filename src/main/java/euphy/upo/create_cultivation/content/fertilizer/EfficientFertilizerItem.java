package euphy.upo.create_cultivation.content.fertilizer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Efficient fertilizer, direct-use mode: right-clicking behaves like vanilla
 * bone meal on any {@code BonemealableBlock}, at roughly 1.5x the effect -
 * a second full application only triggers half of the time, and it runs on a
 * COPY of the stack so a single fertilizer is still consumed per use.
 *
 * <p>Delegating to {@link BoneMealItem#growCrop} keeps every vanilla pathway
 * intact for free (grow-possible types, particles and sound events, item
 * shrink handling, and the greenhouse stall-zone fertilizer block through the
 * blocked-fertilizer config list plus {@code BoneMealItemMixin}).
 */
public class EfficientFertilizerItem extends Item {

    /** Chance of the second (bonus) application, on top of the base one. */
    private static final float DOUBLE_CHANCE = 0.5f;

    public EfficientFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        if (!BoneMealItem.growCrop(stack, level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            // vanilla bone meal parity: broadcast the arm swing on the server
            Player player = context.getPlayer();
            if (player != null) {
                player.swing(context.getHand());
            }
            if (level.random.nextFloat() < DOUBLE_CHANCE) {
                // bonus application on a copy: doubles the effect without an
                // extra fertilizer being consumed
                BoneMealItem.growCrop(stack.copy(), level, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
