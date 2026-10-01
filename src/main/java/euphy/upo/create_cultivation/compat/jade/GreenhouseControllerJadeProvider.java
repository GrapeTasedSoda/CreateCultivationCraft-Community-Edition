package euphy.upo.create_cultivation.compat.jade;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.climate.ClimateResolver;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade provider for the greenhouse controller. Bridges the enclosure scan
 * result (and the ambient climate readout) to the client until the dedicated
 * controller GUI exists.
 */
public enum GreenhouseControllerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            CreateCultivationCraft.MODID, "greenhouse_controller");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("powered"))
            return;

        if (!data.getBoolean("powered")) {
            tooltip.add(Component.translatable("create_cultivation.jade.greenhouse.no_power"));
            return;
        }
        if (!data.getBoolean("scanned")) {
            tooltip.add(Component.translatable("create_cultivation.jade.greenhouse.not_scanned"));
        } else if (data.getBoolean("valid")) {
            tooltip.add(Component.translatable("create_cultivation.jade.greenhouse.valid",
                    data.getInt("volume"), data.getInt("devices")));
            if (data.getBoolean("tempShort") || data.getBoolean("humShort")) {
                tooltip.add(Component.translatable("create_cultivation.jade.greenhouse.short"));
            } else {
                tooltip.add(Component.translatable("create_cultivation.jade.greenhouse.reached"));
            }
        } else {
            tooltip.add(Component.translatable("create_cultivation.jade.greenhouse.invalid",
                    data.getInt("volume")));
        }

        if (data.contains("tempC")) {
            String temp = String.format("%.1f", data.getFloat("tempC"));
            String humidity = String.format("%.0f", data.getFloat("humidity"));
            tooltip.add(Component.translatable("create_cultivation.jade.greenhouse.climate", temp, humidity));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (!(be instanceof GreenhouseControllerBlockEntity controller))
            return;

        boolean powered = controller.getSpeed() != 0;
        data.putBoolean("powered", powered);

        var scan = controller.getLastScan();
        if (scan != null) {
            data.putBoolean("scanned", true);
            data.putBoolean("valid", scan.valid());
            data.putInt("volume", scan.volume());
            data.putInt("devices", scan.devices().size());
            data.putBoolean("tempShort", controller.getClimateState().isTempShort());
            data.putBoolean("humShort", controller.getClimateState().isHumShort());
        }

        Level level = controller.getLevel();
        if (level != null) {
            BlockPos pos = controller.getBlockPos();
            ClimateResolver.Climate climate = ClimateResolver.resolve(level, level.getBiome(pos));
            data.putFloat("tempC", climate.tempC());
            data.putFloat("humidity", climate.humidity());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
