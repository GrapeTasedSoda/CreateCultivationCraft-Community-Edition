package euphy.upo.create_cultivation.compat.jade;

import java.util.List;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.config.CCConfig;
import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.climate.CropClimate;
import euphy.upo.create_cultivation.content.climate.CropState;
import euphy.upo.create_cultivation.content.climate.ClimateResolver;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseCropTracker;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Climate readout for soil-planted crops (blocks carrying the
 * {@code crop_climate} data map, e.g. every vanilla {@link CropBlock}).
 *
 * <p>Priority mirrors the gameplay systems: a fresh greenhouse boost from the
 * controller's snapshot broadcast wins; positions inside a greenhouse
 * interior stay quiet (the controller covers them); otherwise the ambient
 * outdoor climate applies, evaluated client-side from the same biome
 * resolvers the server uses.
 */
public enum CropClimateJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            CreateCultivationCraft.MODID, "crop_climate");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        CropClimate climate = state.getBlockHolder().getData(CCDataMaps.CROP_CLIMATE);
        if (climate == null) {
            return;
        }
        var level = accessor.getLevel();
        var pos = accessor.getPosition();
        long now = level.getGameTime();

        GreenhouseCropTracker.Boost boost = null;
        GreenhouseCropTracker.ClientBoost clientBoost =
                GreenhouseCropTracker.getForClient(level.dimension(), pos, now);
        if (clientBoost != null) {
            boost = new GreenhouseCropTracker.Boost(clientBoost.growthMultiplier(),
                    clientBoost.yieldMultiplier(), clientBoost.stalled());
        }

        if (boost == null) {
            if (GreenhouseCropTracker.isProtectedClient(level.dimension(), pos)) {
                return; // greenhouse interior not covered by a broadcast: stay quiet
            }
            if (level.isClientSide() && CCConfig.AMBIENT_CROPS_ENABLED.get()) {
                ClimateResolver.Climate ambient = ClimateResolver.resolve(level, level.getBiome(pos));
                CropState cropState = climate.evaluate(ambient.tempC(), ambient.humidity());
                boost = boostForState(cropState,
                        CCConfig.AMBIENT_OPTIMAL_YIELD.get(), CCConfig.AMBIENT_OPTIMAL_GROWTH.get(),
                        CCConfig.AMBIENT_PARTIAL_YIELD.get(), CCConfig.AMBIENT_PARTIAL_GROWTH.get(),
                        CCConfig.AMBIENT_SURVIVAL_ONLY_YIELD.get(), CCConfig.AMBIENT_SURVIVAL_ONLY_GROWTH.get());
            }
        }

        if (boost == null) {
            return;
        }
        appendBoostLines(tooltip, boost);
    }

    private static GreenhouseCropTracker.Boost boostForState(CropState cropState, double optYield, double optGrowth,
            double partYield, double partGrowth, double surYield, double surGrowth) {
        return switch (cropState) {
            case OPTIMAL -> new GreenhouseCropTracker.Boost((float) optGrowth, optYield, false);
            case PARTIAL -> new GreenhouseCropTracker.Boost((float) partGrowth, partYield, false);
            case SURVIVAL_ONLY -> new GreenhouseCropTracker.Boost((float) surGrowth, surYield, false);
            case FAIL -> new GreenhouseCropTracker.Boost(0f, 0.0, true);
        };
    }

    /** 停滞/生存 one-liner, or the 速率/产率 multiplier pair. */
    private static void appendBoostLines(ITooltip tooltip, GreenhouseCropTracker.Boost boost) {
        if (boost.stalled()) {
            tooltip.add(Component.translatable("create_cultivation.jade.crop.stalled"));
            return;
        }
        if (boost.growthMultiplier() <= 1.0f && boost.yieldMultiplier() <= 1.0) {
            tooltip.add(Component.translatable("create_cultivation.jade.crop.survival"));
            return;
        }
        tooltip.add(Component.translatable("create_cultivation.jade.crop.growth_rate",
                String.format("%.1f", boost.growthMultiplier())));
        tooltip.add(Component.translatable("create_cultivation.jade.crop.yield_rate",
                String.format("%.1f", boost.yieldMultiplier())));
    }
}
