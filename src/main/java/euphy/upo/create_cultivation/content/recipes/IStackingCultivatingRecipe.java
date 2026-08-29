package euphy.upo.create_cultivation.content.recipes;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;


public interface IStackingCultivatingRecipe {
    ProcessingOutput getResult();
    int getMaxHeight();
    Block getBlockToRender();

    /** The catalyst item accepted by the base's catalyst slot for this crop. */
    Ingredient getCatalyst();

    /** How many boosted ticks one catalyst item lasts. */
    int getCatalystUse();
}
