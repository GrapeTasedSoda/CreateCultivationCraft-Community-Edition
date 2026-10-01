package euphy.upo.create_cultivation.datagen;

import com.simibubi.create.AllTags;
import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.registry.CCBlockTags;
import euphy.upo.create_cultivation.registry.CCBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class CCTagProvider {

    public static class Blocks extends BlockTagsProvider {

        public Blocks(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, lookupProvider, CreateCultivationCraft.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            //Cultivation Base
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(CCBlocks.CULTIVATION_BASE.get());
            tag(BlockTags.MINEABLE_WITH_AXE).add(CCBlocks.CULTIVATION_BASE.get());
            tag(AllTags.AllBlockTags.WRENCH_PICKUP.tag).add(CCBlocks.CULTIVATION_BASE.get());

            //Cultivation Tank-
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(CCBlocks.CULTIVATION_TANK.get());
            tag(BlockTags.MINEABLE_WITH_AXE).add(CCBlocks.CULTIVATION_TANK.get());
            tag(AllTags.AllBlockTags.WRENCH_PICKUP.tag).add(CCBlocks.CULTIVATION_TANK.get());

            //Greenhouse Glass Door (parity with create's slidingDoor transformer)
            tag(BlockTags.DOORS).add(CCBlocks.GREENHOUSE_GLASS_DOOR.get());
            tag(BlockTags.WOODEN_DOORS).add(CCBlocks.GREENHOUSE_GLASS_DOOR.get()); // villager AI
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(CCBlocks.GREENHOUSE_GLASS_DOOR.get());
            tag(AllTags.AllBlockTags.NON_DOUBLE_DOOR.tag).add(CCBlocks.GREENHOUSE_GLASS_DOOR.get());

            //Greenhouse devices - detected (and exempted) by the controller scan
            tag(CCBlockTags.GREENHOUSE_DEVICE).add(
                    CCBlocks.HUMIDIFIER.get(),
                    CCBlocks.DEHUMIDIFIER.get(),
                    CCBlocks.AIR_CONDITIONER.get(),
                    CCBlocks.SPRINKLER.get());

            //Sprinkler - wrench-pickup like the other Create-adjacent machines
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(CCBlocks.SPRINKLER.get());
            tag(AllTags.AllBlockTags.WRENCH_PICKUP.tag).add(CCBlocks.SPRINKLER.get());
        }
    }


    public static class Items extends ItemTagsProvider {

        public Items(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagsProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, lookupProvider, blockTagsProvider, CreateCultivationCraft.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            // the door block and its item share the same registry id
            var doorItem = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.ITEM, CCBlocks.GREENHOUSE_GLASS_DOOR.getId());
            tag(net.minecraft.tags.ItemTags.DOORS).add(doorItem);
            tag(AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag).add(doorItem);
        }
    }
}