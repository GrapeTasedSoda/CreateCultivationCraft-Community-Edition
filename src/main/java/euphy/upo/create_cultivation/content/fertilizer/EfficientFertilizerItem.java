package euphy.upo.create_cultivation.content.fertilizer;

import euphy.upo.create_cultivation.config.CCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Efficient fertilizer, direct-use mode: right-clicking behaves exactly like
 * vanilla bone meal on any {@code BonemealableBlock} (green particles, sound,
 * game event, client-side arm swing), at roughly 1.5x the effect - a second
 * full application only triggers half of the time, and it runs on a COPY of
 * the stack so a single fertilizer is still consumed per use.
 *
 * <p>Delegating to {@link BoneMealItem#applyBonemeal} (the same method vanilla
 * bone meal's {@code useOn} calls) keeps every vanilla pathway intact for free
 * (client-side prediction that produces the arm swing, item shrink handling,
 * the BonemealEvent hook, and the greenhouse stall-zone fertilizer block
 * through the blocked-fertilizer config list plus {@code BoneMealItemMixin}).
 */
public class EfficientFertilizerItem extends Item {

    public EfficientFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        // mirror BoneMealItem#useOn: applyBonemeal runs on BOTH sides - the
        // client-side prediction returning SUCCESS is what plays the local arm
        // swing (BoneMealItem.growCrop is client-dead: it always returns false
        // off the server)
        if (!BoneMealItem.applyBonemeal(stack, level, pos, context.getPlayer())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            context.getPlayer().gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            level.levelEvent(1505, pos, 15);
            if (level.random.nextFloat() < CCConfig.FERTILIZER_BONUS_CHANCE.get().floatValue()) {
                // bonus application on a copy: doubles the effect without an
                // extra fertilizer being consumed
                BoneMealItem.applyBonemeal(stack.copy(), level, pos, context.getPlayer());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
