package euphy.upo.create_cultivation.compat.jade;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade/WAILA provider for the Cultivation Base. Shows an "output full —
 * harvests paused" warning while all output slots are full.
 */
public enum CultivationBaseJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            CreateCultivationCraft.MODID, "cultivation_base");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (serverData.getBoolean("outputFull")) {
            tooltip.add(Component.translatable("create_cultivation.jade.output_full"));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof CultivationBaseBlockEntity baseBE) {
            data.putBoolean("outputFull", baseBE.isOutputFull());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
