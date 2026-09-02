package euphy.upo.create_cultivation.content.cultivation_tank;

import com.mojang.blaze3d.vertex.PoseStack;
import euphy.upo.create_cultivation.content.recipes.CultivatingRecipe;
import euphy.upo.create_cultivation.content.recipes.StackingCultivatingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class CultivationTankRenderer implements BlockEntityRenderer<CultivationTankBlockEntity> {

    public CultivationTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CultivationTankBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (blockEntity.getCurrentRecipe().isEmpty()) {
            return;
        }

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        switch (blockEntity.getRecipeMode()) {
            case STAGE_BASED -> renderStageBased(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, blockRenderer);
            case STACK_BASED -> renderStackBased(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, blockRenderer);
        }
    }

    private void renderStageBased(CultivationTankBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockRenderDispatcher blockRenderer) {
        if (!blockEntity.isController()) {
            return;
        }

        blockEntity.getCurrentRecipe().ifPresent(recipeHolder -> {
            if (recipeHolder.value() instanceof CultivatingRecipe recipe) {
                Block cropBlock = recipe.getCropBlock();
                BlockState cropState = cropBlock.defaultBlockState();

                poseStack.pushPose();

                poseStack.translate(0.5, 0.0625, 0.5);
                float scale = 12.0f / 16.0f;
                poseStack.scale(scale, scale, scale);
                poseStack.translate(-0.5, (double) -1 / 16, -0.5);

                if (cropBlock instanceof DoublePlantBlock) {

                    float growthRatio = blockEntity.getStageGrowthRatio();

                    BlockState lowerState = cropState.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
                    blockRenderer.renderSingleBlock(lowerState, poseStack, bufferSource, packedLight, packedOverlay);

                    if (growthRatio >= 1.0f) {
                        poseStack.pushPose();
                        poseStack.translate(0, 1, 0);
                        BlockState upperState = cropState.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
                        blockRenderer.renderSingleBlock(upperState, poseStack, bufferSource, packedLight, packedOverlay);
                        poseStack.popPose();
                    }

                } else {

                    IntegerProperty ageProperty = null;
                    for (var property : cropState.getProperties()) {
                        if (property instanceof IntegerProperty i && "age".equals(i.getName())) {
                            ageProperty = i;
                            break;
                        }
                    }

                    if (ageProperty != null) {
                        float growthRatio = blockEntity.getStageGrowthRatio();
                        int maxAge = ageProperty.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
                        // Clamp against the property's real range: saves with
                        // uncapped growth progress produce a ratio above 1.0,
                        // and an out-of-range age crashes the client renderer.
                        int currentAge = Mth.clamp((int) (growthRatio * maxAge), 0, maxAge);
                        cropState = cropState.setValue(ageProperty, currentAge);
                    }

                    blockRenderer.renderSingleBlock(cropState, poseStack, bufferSource, packedLight, packedOverlay);
                }

                poseStack.popPose();
            }
        });
    }


    private void renderStackBased(CultivationTankBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockRenderDispatcher blockRenderer) {
        CultivationTankBlockEntity controller = blockEntity.getControllerBE();
        if (controller == null) {
            return;
        }

        controller.getCurrentRecipe().ifPresent(recipeHolder -> {
            if (recipeHolder.value() instanceof StackingCultivatingRecipe recipe) {
                int plantHeight = controller.getCurrentHeight();

                BlockPos controllerPos = controller.getBlockPos();
                BlockPos currentBlockPos = blockEntity.getBlockPos();
                int yOffset = currentBlockPos.getY() - controllerPos.getY();

                if (yOffset >= plantHeight) {
                    return;
                }

                // Two-part crops (e.g. rice): the base block fills every layer,
                // the optional top block replaces it above the first layer.
                boolean isTopLayer = yOffset == plantHeight - 1;
                Block blockToRender = (yOffset > 0 && recipe.getTopRender() != null)
                        ? recipe.getTopRender()
                        : recipe.getBlockToRender();
                BlockState renderState = blockToRender.defaultBlockState();

                // Multi-section crops select their section model via a
                // "location" integer property (e.g. Kaleidoscope Cookery rice:
                // DOWN/MIDDLE/UP). Each tank layer must render the matching
                // section, otherwise upper layers show floating bottom models.
                for (var property : renderState.getProperties()) {
                    if (property instanceof IntegerProperty i && "location".equals(i.getName())) {
                        int min = i.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0);
                        int max = i.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                        renderState = renderState.setValue(i, Mth.clamp(yOffset, min, max));
                        break;
                    }
                }

                poseStack.pushPose();

                poseStack.translate(0.5, 0.0625, 0.5);
                float scaleXZ = 12.0f / 16.0f;
                poseStack.scale(scaleXZ, 1.0f, scaleXZ);
                poseStack.translate(-0.5, (double) -1 /16, -0.5);


                if (blockToRender instanceof ChorusPlantBlock) {
                    boolean connectUp = yOffset < plantHeight - 1;
                    boolean connectDown = yOffset > 0;
                    renderState = renderState.setValue(ChorusPlantBlock.UP, connectUp)
                            .setValue(ChorusPlantBlock.DOWN, connectDown);
                } else if (recipe.isStageByProgress()) {
                    // Animate the "age" property: completed layers render fully
                    // grown; the topmost layer follows the progress toward the
                    // next layer (or stays full once the crop is at its limit).
                    IntegerProperty ageProperty = null;
                    for (var property : renderState.getProperties()) {
                        if (property instanceof IntegerProperty i && "age".equals(i.getName())) {
                            ageProperty = i;
                            break;
                        }
                    }
                    if (ageProperty != null) {
                        int maxAge = ageProperty.getPossibleValues().stream()
                                .mapToInt(Integer::intValue).max().orElse(0);
                        int currentAge;
                        if (controller.isHeightMismatch()) {
                            // Tank too short for this crop: growth is blocked
                            // entirely, so the layer must keep the freshly-
                            // planted look instead of falling through to the
                            // fully mature model below.
                            currentAge = 0;
                        } else if (!isTopLayer) {
                            currentAge = maxAge;
                        } else if (plantHeight < Math.min(controller.getHeight(), recipe.getMaxHeight())) {
                            float growthRatio = controller.getStageGrowthRatio();
                            currentAge = Mth.clamp((int) (growthRatio * (maxAge + 1)), 0, maxAge);
                        } else {
                            currentAge = maxAge;
                        }
                        renderState = renderState.setValue(ageProperty, currentAge);
                    }
                }


                blockRenderer.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });
    }

}