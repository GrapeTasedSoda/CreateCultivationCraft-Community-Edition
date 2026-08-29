package euphy.upo.create_cultivation.content.cultivation_base;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import euphy.upo.create_cultivation.content.recipes.CultivatingRecipe;
import euphy.upo.create_cultivation.content.recipes.ICultivatingRecipe;
import euphy.upo.create_cultivation.content.recipes.IStackingCultivatingRecipe;
import euphy.upo.create_cultivation.content.recipes.StackingCultivatingRecipe;
import euphy.upo.create_cultivation.registry.CCBlocks;
import euphy.upo.create_cultivation.registry.CCMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.List;

public class CultivationBaseBlockEntity extends KineticBlockEntity implements MenuProvider {

    public static final int SLOT_COUNT = 8;
    /** Index of the catalyst slot (the 9th slot of the internal handler). */
    public static final int CATALYST_SLOT = SLOT_COUNT;
    /** How many boosted ticks one catalyst item lasts when the recipe does not specify {@code catalyst_use}. 30 seconds. */
    public static final int DEFAULT_CATALYST_USE = 600;

    private final ItemStackHandler itemHandler = createItemHandler();
    private final IItemHandler automationHandler = createAutomationHandler();
    private boolean isHarvesting = false;
    /** Remaining boosted ticks backed by the catalyst item currently in the slot. */
    private int catalystTicks = 0;
    public CultivationBaseBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);

        updateWorkingState();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * Handler exposed to hoppers and other automation: identical to the internal
     * handler except that the catalyst slot can never be extracted from.
     */
    public IItemHandler getAutomationHandler() {
        return automationHandler;
    }

    private ItemStackHandler createItemHandler() {
        return new ItemStackHandler(SLOT_COUNT + 1) {
            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (slot == CATALYST_SLOT) {
                    if (!isCatalyst(stack)) {
                        return stack;
                    }
                    return super.insertItem(slot, stack, simulate);
                }

                if (!isHarvesting) {
                    return stack;
                }

                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (slot == CATALYST_SLOT) {
                    return isCatalyst(stack);
                }
                return super.isItemValid(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    private IItemHandler createAutomationHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return itemHandler.getSlots();
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return itemHandler.getStackInSlot(slot);
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return itemHandler.insertItem(slot, stack, simulate);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot == CATALYST_SLOT) {
                    return ItemStack.EMPTY;
                }
                return itemHandler.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return itemHandler.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return itemHandler.isItemValid(slot, stack);
            }
        };
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (!clientPacket) {
            compound.put("Inventory", itemHandler.serializeNBT(registries));
            compound.putInt("CatalystTicks", catalystTicks);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (!clientPacket) {
            CompoundTag inventoryTag = compound.getCompound("Inventory");
            // Migration guard: structures/worlds saved before the catalyst slot
            // (SLOT_COUNT + 1 = 9) existed contain Inventory Size=8. NeoForge's
            // ItemStackHandler.deserializeNBT resizes to the saved size, dropping
            // the catalyst slot and later crashing on any insert. Restore first.
            if (inventoryTag.contains("Size") && inventoryTag.getInt("Size") != SLOT_COUNT + 1) {
                inventoryTag.putInt("Size", SLOT_COUNT + 1);
            }
            itemHandler.deserializeNBT(registries, inventoryTag);
            catalystTicks = compound.getInt("CatalystTicks");
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }


        if (lazyTickCounter-- < 0) {
            lazyTickCounter = 20;
            updateWorkingState();
        }

        if (getBlockState().getValue(CultivationBaseBlock.WORKING)) {
            tickCatalyst();
            tryHarvest();
        }
    }


    private void tryHarvest() {
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (!(be instanceof CultivationTankBlockEntity tankBE)) {
            return;
        }

        if (!tankBE.isReadyForHarvest()) {
            return;
        }


        switch (tankBE.getRecipeMode()) {
            case STAGE_BASED -> harvestStageBased(tankBE);
            case STACK_BASED -> harvestStackBased(tankBE);
        }
    }

            private void harvestStageBased(CultivationTankBlockEntity tankBE) {
        this.isHarvesting = true;
        try {
            double cropYield = CCConfig.CROP_YIELD.get();
            tankBE.getCurrentRecipe().ifPresent(recipeHolder -> {

            if (recipeHolder.value() instanceof CultivatingRecipe recipe) {


                Ingredient seedIngredient = recipe.getIngredients().get(0);
                if (recipe.getHeight() > 1 && tankBE.getHeight() < recipe.getHeight()) {

                    return;
                }

                boolean replanted = false;


                List<ProcessingOutput> results = recipe.getRollableResults();
                List<ItemStack> rolledResults = new ArrayList<>();

                boolean wasWatered = tankBE.isWatered();
                if (wasWatered) {
                    double wateredYield = cropYield * CCConfig.WATERING_YIELD_BONUS.get();
                    if (isCatalystBoostActive()) {
                        wateredYield *= CCConfig.WATER_CATALYST_SYNERGY_BONUS.get();
                    }
                    for (ProcessingOutput output : results) {
                        ItemStack full = applyYield(output.getStack().copy(), wateredYield);
                        if (!full.isEmpty()) {
                            rolledResults.add(full);
                        }
                    }
                }

                for (ProcessingOutput output : results) {
                    ItemStack rolled = applyYield(output.rollOutput(level.getRandom()), cropYield);
                    if (!rolled.isEmpty()) {
                        rolledResults.add(rolled);
                    }
                }

                for (int i = 0; i < rolledResults.size(); i++) {
                    ItemStack potentialSeed = rolledResults.get(i);
                    if (seedIngredient.test(potentialSeed)) {

                        potentialSeed.shrink(1);
                        replanted = true;


                        if (potentialSeed.isEmpty()) {
                            rolledResults.remove(i);
                        }
                        break;
                    }
                }

                if (canInsertAll(rolledResults)) {
                    insertAll(rolledResults);
                    tankBE.onHarvest(replanted);
                }
            }
        });
        } finally {
            this.isHarvesting = false;
        }
    }

    private void harvestStackBased(CultivationTankBlockEntity tankBE) {
        this.isHarvesting = true;
        try {
            double cropYield = CCConfig.CROP_YIELD.get();
            tankBE.getCurrentRecipe().ifPresent(recipeHolder -> {

            if (recipeHolder.value() instanceof StackingCultivatingRecipe recipe) {
                ProcessingOutput result = recipe.getResult();
                int height = tankBE.getCurrentHeight();
                int harvestedAmount = height - 1;

                if (harvestedAmount <= 0) return;

                int totalCount = (int) Math.floor(result.getStack().getCount() * harvestedAmount * cropYield);
                if (totalCount <= 0) {
                    return;
                }
                ItemStack totalResult = result.getStack().copyWithCount(totalCount);

                List<ItemStack> results = List.of(totalResult);

                if (canInsertAll(results)) {
                    insertAll(results);
                    tankBE.onHarvest();
                }
            }
        });
        } finally {
            this.isHarvesting = false;
        }
    }

    /**
     * Applies the configured crop yield multiplier to a stack, returning an empty
     * stack if the result rounds down to nothing.
     */
    private static ItemStack applyYield(ItemStack stack, double cropYield) {
        if (stack.isEmpty() || cropYield == 1.0) {
            return stack;
        }
        int count = (int) Math.floor(stack.getCount() * cropYield);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(count);
    }

    private boolean canInsertAll(List<ItemStack> stacks) {
        ItemStackHandler tempHandler = new ItemStackHandler(itemHandler.getSlots());
        tempHandler.deserializeNBT(level.registryAccess(), itemHandler.serializeNBT(level.registryAccess()));
        for (ItemStack stack : stacks) {
            if (!ItemHandlerHelper.insertItemStacked(tempHandler, stack, true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void insertAll(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemHandlerHelper.insertItemStacked(itemHandler, stack, false);
        }
    }

    /**
     * The catalyst ingredient required by the crop currently planted on top of
     * this base, or {@code null} when no crop is planted.
     */
    public Ingredient getCatalystIngredient() {
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (!(be instanceof CultivationTankBlockEntity tankBE)) {
            return null;
        }
        return tankBE.getCurrentRecipe().map(holder -> {
            if (holder.value() instanceof ICultivatingRecipe recipe) {
                return recipe.getCatalyst();
            }
            if (holder.value() instanceof IStackingCultivatingRecipe recipe) {
                return recipe.getCatalyst();
            }
            return null;
        }).orElse(null);
    }

    /**
     * Whether the given stack is a valid catalyst for the crop currently planted
     * on top of this base. Falls back to bone meal when no crop is planted, so
     * players can pre-fill the slot before planting.
     */
    public boolean isCatalyst(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Ingredient ingredient = getCatalystIngredient();
        if (ingredient == null) {
            return stack.is(Items.BONE_MEAL);
        }
        return ingredient.test(stack);
    }

    /** Whether a crop is currently planted in the tank above this base. */
    public boolean hasCrop() {
        if (level == null) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (!(be instanceof CultivationTankBlockEntity tankBE)) {
            return false;
        }
        return tankBE.getRecipeMode() != CultivationTankBlockEntity.RecipeMode.NONE;
    }

    /**
     * Drains the catalyst slot while the machine is working: each catalyst item
     * lasts {@code catalyst_use} ticks (default {@value #DEFAULT_CATALYST_USE},
     * i.e. 30 seconds). While the slot holds a catalyst and ticks remain, the
     * crop grows faster; with an empty slot the speed is unchanged. Without a
     * planted crop nothing is consumed.
     */
    private void tickCatalyst() {
        if (!hasCrop() || !hasCatalyst()) {
            // No crop or no catalyst: the boost is cancelled immediately.
            catalystTicks = 0;
            return;
        }
        if (catalystTicks > 0) {
            catalystTicks--;
        }
        if (catalystTicks <= 0) {
            // Renew in the same tick the previous catalyst expires so the boost
            // never lapses (not even for one tick) while more items remain.
            ItemStack catalystStack = itemHandler.getStackInSlot(CATALYST_SLOT);
            catalystStack.shrink(1);
            itemHandler.setStackInSlot(CATALYST_SLOT, catalystStack);
            catalystTicks = getCatalystUse();
        }
    }

    private int getCatalystUse() {
        if (level == null) {
            return DEFAULT_CATALYST_USE;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (!(be instanceof CultivationTankBlockEntity tankBE)) {
            return DEFAULT_CATALYST_USE;
        }
        return tankBE.getCurrentRecipe().map(holder -> {
            if (holder.value() instanceof ICultivatingRecipe recipe) {
                return recipe.getCatalystUse() > 0 ? recipe.getCatalystUse() : DEFAULT_CATALYST_USE;
            }
            if (holder.value() instanceof IStackingCultivatingRecipe recipe) {
                return recipe.getCatalystUse() > 0 ? recipe.getCatalystUse() : DEFAULT_CATALYST_USE;
            }
            return DEFAULT_CATALYST_USE;
        }).orElse(DEFAULT_CATALYST_USE);
    }

    /**
     * Growth speed multiplier granted by the catalyst slot: the configured bonus
     * while a catalyst item is present and boosted ticks remain, 1.0 otherwise.
     */
    public float getCatalystGrowthMultiplier() {
        if (isCatalystBoostActive()) {
            return CCConfig.CATALYST_GROWTH_BONUS.get().floatValue();
        }
        return 1.0f;
    }

    /** Whether the catalyst slot is actively granting a boost right now (item present and boosted ticks remaining). */
    public boolean isCatalystBoostActive() {
        return hasCatalyst() && catalystTicks > 0;
    }

    /** Whether the cultivation tank above this base is currently watered. */
    public boolean isTankWatered() {
        if (level == null) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        return be instanceof CultivationTankBlockEntity tankBE && tankBE.isWatered();
    }

    /** Whether the catalyst slot currently holds a valid catalyst item. */
    public boolean hasCatalyst() {
        return !itemHandler.getStackInSlot(CATALYST_SLOT).isEmpty();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.create_cultivation.cultivation_base");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CultivationBaseMenu(CCMenuTypes.CULTIVATION_BASE.get(), containerId, playerInventory, this);
    }

    public void updateWorkingState() {

        boolean currentState = getBlockState().getValue(CultivationBaseBlock.WORKING);
        boolean shouldBeWorking = false;


        boolean hasPower = getSpeed() != 0;
        boolean hasTankAbove = level.getBlockState(worldPosition.above()).is(CCBlocks.CULTIVATION_TANK.get());

        if (hasPower && hasTankAbove) {
            shouldBeWorking = true;
        }


        if (currentState != shouldBeWorking) {
            level.setBlock(worldPosition, getBlockState().setValue(CultivationBaseBlock.WORKING, shouldBeWorking), 3);
        }
    }

}
