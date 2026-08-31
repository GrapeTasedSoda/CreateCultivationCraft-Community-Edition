package euphy.upo.create_cultivation.content.cultivation_base;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import euphy.upo.create_cultivation.config.CCCatalysts;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.ArrayList;
import java.util.List;

public class CultivationBaseBlockEntity extends KineticBlockEntity implements MenuProvider {

    public static final int SLOT_COUNT = 8;
    /** Index of the catalyst slot (the 9th slot of the internal handler). */
    public static final int CATALYST_SLOT = SLOT_COUNT;
    /** How many boosted ticks one catalyst item lasts when the recipe does not specify {@code catalyst_use}. 30 seconds. */
    public static final int DEFAULT_CATALYST_USE = 600;
    /** Type of the catalyst currently being consumed (resolved at renewal). */
    @Nullable
    private CCCatalysts.CatalystType activeCatalystType;
    /** Registry id of the last consumed catalyst; lets an empty slot still report the active boost type after reload. */
    @Nullable
    private ResourceLocation lastConsumedCatalystId;

    private final ItemStackHandler itemHandler = createItemHandler();
    private final IItemHandler automationHandler = createAutomationHandler();
    private boolean isHarvesting = false;
    /** Remaining boosted ticks backed by the catalyst item currently in the slot. */
    private int catalystTicks = 0;
    /** Cached "all output slots full" flag, synced to clients when it flips. */
    private boolean outputFull = false;
    /** Cached "crop needs a taller tank" alarm (orange glow), synced to clients. */
    private boolean heightMismatch = false;
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
     * Whether the next harvest could not place all of its items into the
     * output slots (predicted with the same math as a real harvest). While
     * full, the base lights its red "output full" glow, pauses harvests,
     * catalyst consumption and watering.
     */
    public boolean isOutputFull() {
        return outputFull;
    }

    /**
     * Whether the planted crop requires a taller tank stack than currently
     * built (stacking recipes {@code minHeight}, stage recipes {@code height}).
     * While latched the base lights its orange glow and pauses harvests,
     * catalyst consumption and watering - the crop cannot grow at all, so any
     * resource input would be wasted.
     */
    public boolean isHeightMismatch() {
        return heightMismatch;
    }

    /** Recomputes the cached height-mismatch flag and syncs it on change. */
    private void updateHeightMismatch() {
        boolean mismatch = computeHeightMismatch();
        if (mismatch != heightMismatch) {
            heightMismatch = mismatch;
            if (level != null && !level.isClientSide) {
                sendData();
            }
        }
    }

    private boolean computeHeightMismatch() {
        if (level == null) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (!(be instanceof CultivationTankBlockEntity tankBE)) {
            return false;
        }
        CultivationTankBlockEntity controller = tankBE.getControllerBE();
        if (controller == null) {
            return false;
        }
        var recipeHolder = controller.getCurrentRecipe();
        if (recipeHolder.isEmpty()) {
            return false;
        }
        if (recipeHolder.get().value() instanceof StackingCultivatingRecipe recipe) {
            return controller.getHeight() < recipe.getMinHeight();
        }
        if (recipeHolder.get().value() instanceof CultivatingRecipe recipe) {
            return recipe.getHeight() > 1 && controller.getHeight() < recipe.getHeight();
        }
        return false;
    }

    /**
     * Recomputes the cached output-full flag with hysteresis: while latched,
     * the flag only clears when even a worst-case harvest (watered + fertilizer
     * both active) would fit. This breaks the feedback loop where pausing
     * removes the watered/fertilizer bonus from the prediction, shrinks it,
     * un-pauses the machine, re-watering restores the bonus, and the alarm
     * oscillates - which kept consuming fertilizer instead of stopping.
     */
    private void updateOutputFull() {
        boolean full = isOutputSlotsFull(outputFull);
        if (full != outputFull) {
            outputFull = full;
            if (level != null && !level.isClientSide) {
                if (full) {
                    // Cut the residual watered state the moment the alarm hits
                    // so watering (and its GUI animation) stops immediately
                    // instead of lingering for the rest of its duration.
                    clearTankWatered();
                }
                sendData();
            }
        }
    }

    private void clearTankWatered() {
        if (level == null) {
            return;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (be instanceof CultivationTankBlockEntity tankBE) {
            CultivationTankBlockEntity controller = tankBE.getControllerBE();
            if (controller != null && controller.isWatered()) {
                controller.setWatered(false);
            }
        }
    }

    private boolean isOutputSlotsFull(boolean worstCase) {
        // "Full" means the next harvest would overflow: its predicted output
        // (scaled like a real harvest) cannot fully fit into the output
        // slots. Partially stacked same-item slots still accept merges, so a
        // single crop across 7 open slots + 1 empty slot is NOT full; mixed
        // crops only block when no empty or matching slot can absorb them.
        // While the alarm is latched the prediction uses the worst case
        // (watering and fertilizer assumed active) so the machine can never
        // un-pause itself merely because pausing changed those inputs.
        List<ItemStack> pending = predictNextHarvest(worstCase);
        if (pending.isEmpty()) {
            return false;
        }
        return !canInsertAll(pending);
    }

    /**
     * Estimates what the next harvest would produce, mirroring the real
     * harvest math: recipe outputs rolled once (chance included), scaled by
     * the current unified yield multiplier, minus one item reserved for
     * replanting. Empty when no harvest is currently due.
     *
     * @param worstCase when true, assumes watering is active and the catalyst
     *                  slot is consuming - the maximum yield the machine could
     *                  ever produce at this moment. Used to release the
     *                  output-full latch safely.
     */
    private List<ItemStack> predictNextHarvest(boolean worstCase) {
        List<ItemStack> pending = new ArrayList<>();
        if (level == null) {
            return pending;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (!(be instanceof CultivationTankBlockEntity tankBE) || !tankBE.isReadyForHarvest()) {
            return pending;
        }

        boolean hasCrop = tankBE.getRecipeMode() != CultivationTankBlockEntity.RecipeMode.NONE;
        boolean watered = worstCase ? hasCrop : tankBE.isWatered();
        double catalystYield = getCatalystYieldMultiplier(isCatalystBoostActive() || (worstCase && hasCatalyst()));
        double yieldMultiplier = CCConfig.CROP_YIELD.get() * catalystYield;
        if (watered) {
            yieldMultiplier *= CCConfig.WATERING_YIELD_BONUS.get();
            if (isCatalystBoostActive() || (worstCase && hasCatalyst())) {
                yieldMultiplier *= CCConfig.WATER_CATALYST_SYNERGY_BONUS.get();
            }
        }

        var recipeHolder = tankBE.getCurrentRecipe();
        if (recipeHolder.isEmpty()) {
            return pending;
        }

        if (tankBE.getRecipeMode() == CultivationTankBlockEntity.RecipeMode.STAGE_BASED
                && recipeHolder.get().value() instanceof CultivatingRecipe recipe) {
            // Same height gate as the real harvest.
            if (recipe.getHeight() > 1 && tankBE.getHeight() < recipe.getHeight()) {
                return pending;
            }
            Ingredient seedIngredient = recipe.getIngredients().get(0);
            // Deterministic worst case: chance outputs are counted as if they
            // always succeed, so the alarm never flickers between lazy ticks.
            for (ProcessingOutput output : recipe.getRollableResults()) {
                ItemStack rolled = applyYield(output.getStack().copy(), yieldMultiplier);
                if (!rolled.isEmpty()) {
                    pending.add(rolled);
                }
            }
            // Reserve one item for the automatic replant.
            for (ItemStack stack : pending) {
                if (seedIngredient.test(stack)) {
                    stack.shrink(1);
                    break;
                }
            }
            pending.removeIf(ItemStack::isEmpty);
        } else if (tankBE.getRecipeMode() == CultivationTankBlockEntity.RecipeMode.STACK_BASED
                && recipeHolder.get().value() instanceof StackingCultivatingRecipe stackRecipe) {
            int layers = Math.max(1, tankBE.getCurrentHeight());
            ItemStack base = stackRecipe.getResult().getStack().copy();
            int totalCount = (int) Math.floor(base.getCount() * layers * yieldMultiplier);
            if (totalCount > 0) {
                base.setCount(totalCount);
                pending.add(base);
            }
        }
        return pending;
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
                updateOutputFull();
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
            if (catalystTicks > 0) {
                ItemStack slotStack = itemHandler.getStackInSlot(CATALYST_SLOT);
                // Persist which catalyst type is currently granting the boost.
                // With an empty slot this is the last consumed item and must be
                // remembered, otherwise the multiplier falls back after reload.
                ResourceLocation itemId = slotStack.isEmpty()
                        ? lastConsumedCatalystId
                        : BuiltInRegistries.ITEM.getKey(slotStack.getItem());
                if (itemId != null) {
                    compound.putString("ActiveCatalyst", itemId.toString());
                }
            }
        }
        // Synced in update packets too, so the client renderer can show the
        // orange height alarm without recomputing recipe lookups client-side.
        compound.putBoolean("HeightMismatch", heightMismatch);
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
            if (compound.contains("ActiveCatalyst", CompoundTag.TAG_STRING)) {
                ResourceLocation itemId = ResourceLocation.parse(compound.getString("ActiveCatalyst"));
                activeCatalystType = BuiltInRegistries.ITEM.getOptional(itemId)
                        .map(CCCatalysts::getType)
                        .orElse(null);
            } else {
                activeCatalystType = null;
            }
        }
        // The flag is derived state: recompute on both sides instead of
        // persisting it, so old saves and new code always agree. Plain
        // evaluation (no hysteresis) is fine during load - the alarm will
        // re-latch itself if the prediction overflows.
        outputFull = isOutputSlotsFull(outputFull);
        // Height alarm arrives from the server via update packets (the recipe
        // lookup it depends on is not reliable on the client).
        heightMismatch = compound.getBoolean("HeightMismatch");
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
            // Refresh the alarm every tick while working so a full output or a
            // height mismatch is detected the moment it arises, not up to 10
            // seconds later on the next lazy tick.
            updateHeightMismatch();
            updateOutputFull();
            tickCatalyst();
            tryHarvest();
        } else if (level != null && !level.isClientSide) {
            // Not working: make sure cached alarms clear immediately when the
            // machine stops (power lost / tank removed).
            updateHeightMismatch();
        }
    }


    private void tryHarvest() {
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (!(be instanceof CultivationTankBlockEntity tankBE)) {
            return;
        }

        if (isHeightMismatch()) {
            // Orange alarm state: the crop cannot grow (tank too short), so
            // harvesting a "mature" state here would be a false positive.
            return;
        }

        if (isOutputFull()) {
            // Red alarm state: every output slot is occupied, the harvest
            // cannot place its items and is skipped entirely.
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

                // Single unified multiplier: config yield x fertilizer yield,
                // x watering bonus when watered, x synergy when watered AND
                // fertilized. Matches the display link multiplier readout.
                double yieldMultiplier = cropYield * getCatalystYieldMultiplier();
                if (tankBE.isWatered()) {
                    yieldMultiplier *= CCConfig.WATERING_YIELD_BONUS.get();
                    if (isCatalystBoostActive()) {
                        yieldMultiplier *= CCConfig.WATER_CATALYST_SYNERGY_BONUS.get();
                    }
                }

                // Roll each output exactly once (chance included), then scale.
                for (ProcessingOutput output : results) {
                    ItemStack rolled = applyYield(output.rollOutput(level.getRandom()), yieldMultiplier);
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

                // Same unified multiplier as stage-based harvests so the
                // display link readout matches actual output.
                double yieldMultiplier = cropYield * getCatalystYieldMultiplier();
                if (tankBE.isWatered()) {
                    yieldMultiplier *= CCConfig.WATERING_YIELD_BONUS.get();
                    if (isCatalystBoostActive()) {
                        yieldMultiplier *= CCConfig.WATER_CATALYST_SYNERGY_BONUS.get();
                    }
                }
                int totalCount = (int) Math.floor(result.getStack().getCount() * harvestedAmount * yieldMultiplier);
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
     * Whether the given stack is a valid catalyst. Accepts any item registered
     * in the config catalyst table, plus whatever the planted crop's recipe
     * lists in its {@code catalyst} ingredient (fallback effects), so players
     * can pre-fill the slot before planting.
     */
    public boolean isCatalyst(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (CCCatalysts.isCatalyst(stack.getItem())) {
            return true;
        }
        Ingredient ingredient = getCatalystIngredient();
        return ingredient != null && ingredient.test(stack);
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
        if (!hasCrop()) {
            // No crop: the machine is not growing anything, so the boost is
            // cancelled outright - the GUI animations must stop and the timer
            // must not resume when a new crop is planted.
            catalystTicks = 0;
            return;
        }
        if (isHeightMismatch()) {
            // Orange alarm: the crop cannot grow, so consumption is paused
            // (timer frozen, nothing consumed) until the alarm clears.
            return;
        }
        if (isOutputFull()) {
            // Output full: the machine is paused, so hold the catalyst timer
            // instead of consuming items for a machine that is not working.
            // The remaining time resumes when the alarm clears.
            return;
        }
        if (catalystTicks > 0) {
            catalystTicks--;
        }
        if (catalystTicks <= 0) {
            if (!hasCatalyst()) {
                // Slot drained: the current boost simply expires. Do NOT reset
                // the timer - the remaining time belongs to the last consumed
                // item, not to the presence of items in the slot.
                return;
            }
            // Renew in the same tick the previous catalyst expires so the boost
            // never lapses (not even for one tick) while more items remain.
            ItemStack catalystStack = itemHandler.getStackInSlot(CATALYST_SLOT);
            activeCatalystType = CCCatalysts.getType(catalystStack.getItem());
            lastConsumedCatalystId = BuiltInRegistries.ITEM.getKey(catalystStack.getItem());
            catalystStack.shrink(1);
            itemHandler.setStackInSlot(CATALYST_SLOT, catalystStack);
            catalystTicks = currentCatalystDuration();
        }
    }

    /** Duration for the catalyst currently being consumed: config table first, then recipe override, then fallback. */
    private int currentCatalystDuration() {
        ItemStack catalystStack = itemHandler.getStackInSlot(CATALYST_SLOT);
        CCCatalysts.CatalystType type = activeCatalystType != null ? activeCatalystType
            : CCCatalysts.getType(catalystStack.getItem());
        if (type != null) {
            return type.durationTicks();
        }
        return getCatalystUse();
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
     * Growth speed multiplier granted by the catalyst slot. Config-registered
     * catalysts use their table multiplier (the item being consumed defines
     * the boost); items accepted only via a recipe fall back to the built-in
     * values. No catalyst: 1.0.
     */
    public float getCatalystGrowthMultiplier() {
        if (!isCatalystBoostActive()) {
            return 1.0f;
        }
        CCCatalysts.CatalystType type = activeCatalystType;
        if (type == null) {
            ItemStack catalystStack = itemHandler.getStackInSlot(CATALYST_SLOT);
            type = CCCatalysts.getType(catalystStack.getItem());
        }
        if (type != null) {
            return (float) type.growthMultiplier();
        }
        return (float) CCCatalysts.FALLBACK.growthMultiplier();
    }

    /**
     * Harvest yield multiplier granted by the catalyst slot: the consumed
     * catalyst's configured yield multiplier while active, 1.0 otherwise.
     */
    public double getCatalystYieldMultiplier() {
        return getCatalystYieldMultiplier(isCatalystBoostActive());
    }

    /**
     * Yield multiplier for the catalyst in the slot. With {@code assumeActive}
     * the configured multiplier is returned even while the boost is paused -
     * used by worst-case output-full predictions (the machine will consume
     * again as soon as the alarm clears).
     */
    private double getCatalystYieldMultiplier(boolean assumeActive) {
        if (!assumeActive) {
            return 1.0;
        }
        CCCatalysts.CatalystType type = activeCatalystType;
        if (type == null) {
            ItemStack catalystStack = itemHandler.getStackInSlot(CATALYST_SLOT);
            type = CCCatalysts.getType(catalystStack.getItem());
        }
        if (type != null) {
            return type.yieldMultiplier();
        }
        return CCCatalysts.FALLBACK.yieldMultiplier();
    }

    /**
     * Whether the catalyst slot is actively granting a boost right now (item
     * present, boosted ticks remaining, output not full - the boost is treated
     * as paused while the output-full alarm is on).
     */
    public boolean isCatalystBoostActive() {
        // Active while paid-for ticks remain. Empty slot only means no further
        // renewal; the last consumed item's time must still count, otherwise
        // a single catalyst would be consumed with no visible effect.
        return catalystTicks > 0 && !outputFull;
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
