package euphy.upo.create_cultivation.compat.jade;

import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseBlock;
import euphy.upo.create_cultivation.content.cultivation_base.CultivationBaseBlockEntity;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlock;
import euphy.upo.create_cultivation.content.greenhouse.GreenhouseControllerBlockEntity;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlock;
import euphy.upo.create_cultivation.content.cultivation_tank.CultivationTankBlockEntity;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {

        registration.registerBlockDataProvider(CultivationTankJadeProvider.INSTANCE, CultivationTankBlockEntity.class);
        registration.registerBlockDataProvider(CultivationBaseJadeProvider.INSTANCE, CultivationBaseBlockEntity.class);
        registration.registerBlockDataProvider(GreenhouseControllerJadeProvider.INSTANCE, GreenhouseControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {

        registration.registerBlockComponent(CultivationTankJadeProvider.INSTANCE, CultivationTankBlock.class);
        registration.registerBlockComponent(CultivationBaseJadeProvider.INSTANCE, CultivationBaseBlock.class);
        registration.registerBlockComponent(GreenhouseControllerJadeProvider.INSTANCE, GreenhouseControllerBlock.class);
        // ONE registration at the top of the block hierarchy: Jade runs every
        // provider matched along the superclass chain, so also listing
        // CropBlock/BushBlock made CropBlock-family crops render twice while
        // Block-level crops (sugar cane, cactus, ...) matched nothing. The
        // provider itself filters non-crop blocks via the crop_climate lookup.
        registration.registerBlockComponent(CropClimateJadeProvider.INSTANCE, Block.class);
    }
}