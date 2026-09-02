package euphy.upo.create_cultivation.compat.jade;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import euphy.upo.create_cultivation.content.recipes.CultivatingRecipe;
import euphy.upo.create_cultivation.content.recipes.StackingCultivatingRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CultivationTankJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(CreateCultivationCraft.MODID, "cultivation_tank");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains("isGrowing")) {
            return;
        }

        if (accessor.getBlockEntity() instanceof CultivationTankBlockEntity tankBE) {
            CultivationTankBlockEntity controllerBE = tankBE.getControllerBE();
            // The controller BE can be momentarily absent (chunk edge, unload
            // in progress) - guard instead of letting Jade throw.
            if (controllerBE != null) {
                controllerBE.getCurrentRecipe().ifPresent(recipeHolder -> {
                    ItemStack seedStack = recipeHolder.value().getIngredients().get(0).getItems()[0];
                    tooltip.add(Component.translatable("create_cultivation.jade.crop", seedStack.getDisplayName()));
                });
            }
        }

        if (serverData.getBoolean("isMature")) {
            tooltip.add(Component.translatable("create_cultivation.jade.mature"));
        } else if (serverData.getBoolean("heightMismatch")) {
            // Height alarm takes precedence over the growth readout: the crop
            // cannot grow in this tank, so a remaining-time number would lie.
            tooltip.add(Component.translatable("create_cultivation.jade.height_mismatch"));
        } else {
            String recipeMode = serverData.getString("recipeMode");
            if ("STAGE_BASED".equals(recipeMode)) {
                float pointsPerGameTick = serverData.getFloat("pointsPerGameTick");
                if (pointsPerGameTick <= 1.0e-5f) {
                    tooltip.add(Component.translatable("create_cultivation.jade.stalled"));
                } else {
                    // Deterministic estimate: remaining progress ÷ per-tick
                    // rate, computed from the latest server snapshot. No
                    // client-side extrapolation, so the number is stable and
                    // only refines downward as Jade re-syncs snapshots.
                    float remainingPoints = serverData.getFloat("remainingPoints");
                    float remainingSeconds = Math.max(0, remainingPoints / (pointsPerGameTick * 20.0f));
                    String formattedTime = String.format("%.1f", remainingSeconds);
                    tooltip.add(Component.translatable("create_cultivation.jade.time_remaining", formattedTime));
                }
            } else if ("STACK_BASED".equals(recipeMode)) {
                int currentHeight = serverData.getInt("currentHeight");
                int maxHeight = serverData.getInt("maxHeight");
                tooltip.add(Component.translatable("create_cultivation.jade.growth", currentHeight, maxHeight));

                float pointsPerGameTick = serverData.getFloat("pointsPerGameTick");
                if (pointsPerGameTick > 1.0e-5f) {
                    // Same deterministic estimate as stage mode: remaining
                    // points (all unfinished layers) ÷ per-tick rate.
                    float remainingPoints = serverData.getFloat("remainingPoints");
                    float remainingSeconds = Math.max(0, remainingPoints / (pointsPerGameTick * 20.0f));
                    String formattedTime = String.format("%.1f", remainingSeconds);
                    tooltip.add(Component.translatable("create_cultivation.jade.time_remaining", formattedTime));
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (!(be instanceof CultivationTankBlockEntity tankBE)) {
            return;
        }

        CultivationTankBlockEntity controller = tankBE.getControllerBE();
        if (controller != null && controller.getCurrentRecipe().isPresent()) {
            boolean mismatch = baseHeightMismatch(controller);
            data.putBoolean("isGrowing", true);
            data.putBoolean("heightMismatch", mismatch);
            // With a too-short tank the crop cannot grow, so the stored
            // "mature" flag is a false positive - report it as not mature.
            data.putBoolean("isMature", !mismatch && controller.isMature());
            data.putString("recipeMode", controller.getRecipeMode().name());

            if (!controller.isMature() && !mismatch) {
                if (controller.getRecipeMode() == CultivationTankBlockEntity.RecipeMode.STAGE_BASED) {
                    // Smooth, tick-accurate remaining points (batched growth
                    // plus the in-flight fraction) divided by the true
                    // per-game-tick rate (speed, config, catalyst, watering).
                    // The client only divides this snapshot: remaining ÷ rate
                    // gives a deterministic remaining time that stays stable
                    // between syncs and adapts when the rate changes.
                    data.putFloat("remainingPoints", controller.getSmoothRemainingPoints());
                    data.putFloat("pointsPerGameTick", controller.getGrowthPointsPerGameTick());
                } else if (controller.getRecipeMode() == CultivationTankBlockEntity.RecipeMode.STACK_BASED) {
                    data.putInt("currentHeight", controller.getCurrentHeight());
                    data.putFloat("remainingPoints", controller.getSmoothRemainingPoints());
                    data.putFloat("pointsPerGameTick", controller.getGrowthPointsPerGameTick());
                    controller.getCurrentRecipe().ifPresent(recipeHolder -> {
                        if (recipeHolder.value() instanceof StackingCultivatingRecipe recipe) {
                            data.putInt("maxHeight", recipe.getMaxHeight());
                        }
                    });
                }
            }
        }
    }

    /** Height requirement of the recipe vs the built tank stack (server side). */
    private boolean baseHeightMismatch(CultivationTankBlockEntity controller) {
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

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}