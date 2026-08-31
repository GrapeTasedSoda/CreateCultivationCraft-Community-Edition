package euphy.upo.create_cultivation.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import euphy.upo.create_cultivation.registry.CCRecipes;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CCCompatRecipeGenerator implements DataProvider {
    private final PackOutput packOutput;

    public CCCompatRecipeGenerator(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path outputFolder = this.packOutput.getOutputFolder();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        generateFarmersDelightRecipes((id, json) -> {
            Path recipePath = outputFolder.resolve("data/" + id.getNamespace() + "/recipe/compat/" + id.getPath() + ".json");
            futures.add(DataProvider.saveStable(cache, json, recipePath));
        });

        generateKaleidoscopeCookeryRecipes((id, json) -> {
            Path recipePath = outputFolder.resolve("data/" + id.getNamespace() + "/recipe/compat/" + id.getPath() + ".json");
            futures.add(DataProvider.saveStable(cache, json, recipePath));
        });

        generateRusticDelightRecipes((id, json) -> {
            Path recipePath = outputFolder.resolve("data/" + id.getNamespace() + "/recipe/compat/" + id.getPath() + ".json");
            futures.add(DataProvider.saveStable(cache, json, recipePath));
        });

        generateKaleidoscopeTavernRecipes((id, json) -> {
            Path recipePath = outputFolder.resolve("data/" + id.getNamespace() + "/recipe/compat/" + id.getPath() + ".json");
            futures.add(DataProvider.saveStable(cache, json, recipePath));
        });

        generateCornDelightRecipes((id, json) -> {
            Path recipePath = outputFolder.resolve("data/" + id.getNamespace() + "/recipe/compat/" + id.getPath() + ".json");
            futures.add(DataProvider.saveStable(cache, json, recipePath));
        });

        generatePineappleDelightRecipes((id, json) -> {
            Path recipePath = outputFolder.resolve("data/" + id.getNamespace() + "/recipe/compat/" + id.getPath() + ".json");
            futures.add(DataProvider.saveStable(cache, json, recipePath));
        });

        generateMyNethersDelightRecipes((id, json) -> {
            Path recipePath = outputFolder.resolve("data/" + id.getNamespace() + "/recipe/compat/" + id.getPath() + ".json");
            futures.add(DataProvider.saveStable(cache, json, recipePath));
        });

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void generateKaleidoscopeCookeryRecipes(RecipeAcceptor acceptor) {
        //万花筒烹饪：番茄（耕地成ren：1番茄+1种子，右键后回到成熟前的阶段）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "kaleidoscopecookery/tomato"),
                createCultivatingRecipe(
                        "kaleidoscope_cookery",
                        "kaleidoscope_cookery:tomato_seed",
                        new String[]{"kaleidoscope_cookery:tomato,2", "kaleidoscope_cookery:tomato_seed,1", "kaleidoscope_cookery:tomato_seed,1,0.5"},
                        12000,
                        "kaleidoscope_cookery:tomato_crop"
                )
        );
        //万花筒烹饪：辣椒（耕地成ren：1红辣椒+1种子+20%青椒；右键收获后回到第五阶段自补种）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "kaleidoscopecookery/chili"),
                createCultivatingRecipe(
                        "kaleidoscope_cookery",
                        "kaleidoscope_cookery:chili_seed",
                        new String[]{"kaleidoscope_cookery:red_chili,1", "kaleidoscope_cookery:green_chili,1,0.2", "kaleidoscope_cookery:chili_seed,1"},
                        12000,
                        "kaleidoscope_cookery:chili_crop"
                )
        );
        //万花筒烹饪：生菜（耕地成ren：1生菜+1种子+10%毛虫）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "kaleidoscopecookery/lettuce"),
                createCultivatingRecipe(
                        "kaleidoscope_cookery",
                        "kaleidoscope_cookery:lettuce_seed",
                        new String[]{"kaleidoscope_cookery:lettuce,1", "kaleidoscope_cookery:lettuce_seed,1", "kaleidoscope_cookery:lettuce_seed,1,0.5", "kaleidoscope_cookery:caterpillar,1,0.1"},
                        12000,
                        "kaleidoscope_cookery:lettuce_crop"
                )
        );
        //万花筒烹饪：水稻（堆叠作物，需要至少2格高的栽培罐；耕地成ren：2–4稻穗）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "kaleidoscopecookery/rice"),
                createStackingRecipe(
                        "kaleidoscope_cookery",
                        "kaleidoscope_cookery:rice",
                        "kaleidoscope_cookery:rice_panicle",
                        2,
                        9000,
                        "kaleidoscope_cookery:rice_crop",
                        null,
                        true,
                        3,
                        2
                )
        );
    }

    private void generateFarmersDelightRecipes(RecipeAcceptor acceptor) {
        //农夫乐事：卷心菜
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "farmersdelight/cabbage"),
                createCultivatingRecipe(
                        "farmersdelight",
                        "farmersdelight:cabbage_seeds",
                        new String[]{"farmersdelight:cabbage,1", "farmersdelight:cabbage_seeds,1", "farmersdelight:cabbage_seeds,1,0.5"},
                        18000,
                        "farmersdelight:cabbages"
                )
        );
        //农夫乐事：番茄
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "farmersdelight/tomato"),
                createCultivatingRecipe(
                        "farmersdelight",
                        "farmersdelight:tomato_seeds",
                        new String[]{"farmersdelight:tomato,2", "farmersdelight:tomato,1,0.5", "farmersdelight:tomato_seeds,1"},
                        18000,
                        "farmersdelight:tomatoes"
                )
        );
        //农夫乐事：洋葱
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "farmersdelight/onion"),
                createCultivatingRecipe(
                        "farmersdelight",
                        "farmersdelight:onion",
                        new String[]{"farmersdelight:onion,3", "farmersdelight:onion,1,0.5"},
                        18000,
                        "farmersdelight:onions"
                )
        );
        //农夫乐事：稻米（堆叠作物，需要至少 2 格高的栽培罐；底层稻株+顶层稻穗）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "farmersdelight/rice"),
                createStackingRecipe(
                        "farmersdelight",
                        "farmersdelight:rice",
                        "farmersdelight:rice_panicle",
                        2,
                        9000,
                        "farmersdelight:rice",
                        "farmersdelight:rice_panicles",
                        true,
                        2,
                        2
                )
        );
        /*
        //自然环境：车轮花
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/cartwheel"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:cartwheel",
                        new String[]{"environmental:cartwheel,2", "environmental:cartwheel,1,0.5"},
                        140,
                        "environmental:cartwheel"
                )
        );
        //自然环境：蓝铃花
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/bluebell"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:bluebell",
                        new String[]{"environmental:bluebell,2", "environmental:bluebell,1,0.5"},
                        140,
                        "environmental:bluebell"
                )
        );
        //自然环境：紫罗兰
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/violet"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:violet",
                        new String[]{"environmental:violet,2", "environmental:violet,1,0.5"},
                        140,
                        "environmental:violet"
                )
        );
        //自然环境：石竹
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/dianthus"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:dianthus",
                        new String[]{"environmental:dianthus,2", "environmental:dianthus,1,0.5"},
                        140,
                        "environmental:dianthus"
                )
        );
        //自然环境：莲花
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/red_lotus_flower"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:red_lotus_flower",
                        new String[]{"environmental:red_lotus_flower,2", "environmental:red_lotus_flower,1,0.5"},
                        140,
                        "environmental:red_lotus_flower"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/white_lotus_flower"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:white_lotus_flower",
                        new String[]{"environmental:white_lotus_flower,2", "environmental:white_lotus_flower,1,0.5"},
                        140,
                        "environmental:white_lotus_flower"
                )
        );
        //自然环境：木槿花
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/yellow_hibiscus"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:yellow_hibiscus",
                        new String[]{"environmental:yellow_hibiscus,2", "environmental:yellow_hibiscus,1,0.5"},
                        140,
                        "environmental:yellow_hibiscus"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/orange_hibiscus"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:orange_hibiscus",
                        new String[]{"environmental:orange_hibiscus,2", "environmental:orange_hibiscus,1,0.5"},
                        140,
                        "environmental:orange_hibiscus"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/red_hibiscus"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:red_hibiscus",
                        new String[]{"environmental:red_hibiscus,2", "environmental:red_hibiscus,1,0.5"},
                        140,
                        "environmental:red_hibiscus"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/pink_hibiscus"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:pink_hibiscus",
                        new String[]{"environmental:pink_hibiscus,2", "environmental:pink_hibiscus,1,0.5"},
                        140,
                        "environmental:pink_hibiscus"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/purple_hibiscus"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:purple_hibiscus",
                        new String[]{"environmental:purple_hibiscus,2", "environmental:purple_hibiscus,1,0.5"},
                        140,
                        "environmental:purple_hibiscus"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/purple_hibiscus"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:purple_hibiscus",
                        new String[]{"environmental:purple_hibiscus,2", "environmental:purple_hibiscus,1,0.5"},
                        140,
                        "environmental:purple_hibiscus"
                )
        );
        //自然环境：鹤望兰
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/bird_of_paradise"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:bird_of_paradise",
                        new String[]{"environmental:bird_of_paradise,2", "environmental:bird_of_paradise,1,0.5"},
                        140,
                        "environmental:bird_of_paradise"
                )
        );
        //自然环境：翠雀花
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/pink_delphinium"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:pink_delphinium",
                        new String[]{"environmental:pink_delphinium,2", "environmental:pink_delphinium,1,0.5"},
                        140,
                        "environmental:pink_delphinium"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/blue_delphinium"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:blue_delphinium",
                        new String[]{"environmental:blue_delphinium,2", "environmental:blue_delphinium,1,0.5"},
                        140,
                        "environmental:blue_delphinium"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/purple_delphinium"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:purple_delphinium",
                        new String[]{"environmental:purple_delphinium,2", "environmental:purple_delphinium,1,0.5"},
                        140,
                        "environmental:purple_delphinium"
                )
        );
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "environmental/white_delphinium"),
                createCultivatingRecipe(
                        "environmental",
                        "environmental:white_delphinium",
                        new String[]{"environmental:white_delphinium,2", "environmental:white_delphinium,1,0.5"},
                        140,
                        "environmental:white_delphinium"
                )
        );
         */





    }

    private void generateRusticDelightRecipes(RecipeAcceptor acceptor) {
        //乡村乐事：棉花（耕地成熟：1-3棉铃+1-3种子，取均值）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "rusticdelight/cotton"),
                createCultivatingRecipe(
                        "rusticdelight",
                        "rusticdelight:cotton_seeds",
                        new String[]{"rusticdelight:cotton_boll,2", "rusticdelight:cotton_boll,1,0.5", "rusticdelight:cotton_seeds,1", "rusticdelight:cotton_seeds,1,0.5"},
                        12000,
                        "rusticdelight:cotton"
                )
        );
        //乡村乐事：咖啡（种子即咖啡豆；耕地成熟：1+1-4豆，取均值3.5）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "rusticdelight/coffee"),
                createCultivatingRecipe(
                        "rusticdelight",
                        "rusticdelight:coffee_beans",
                        new String[]{"rusticdelight:coffee_beans,3", "rusticdelight:coffee_beans,1,0.5"},
                        12000,
                        "rusticdelight:coffee"
                )
        );
        //乡村乐事：甜椒（随机类别：主池权重红6绿1黄1 + 两个15%额外池，简化为必给红椒+低概率绿黄）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "rusticdelight/bell_peppers"),
                createCultivatingRecipe(
                        "rusticdelight",
                        "rusticdelight:bell_pepper_seeds",
                        new String[]{"rusticdelight:bell_pepper_red,1", "rusticdelight:bell_pepper_green,1,0.25", "rusticdelight:bell_pepper_yellow,1,0.25", "rusticdelight:bell_pepper_seeds,1", "rusticdelight:bell_pepper_seeds,1,0.5"},
                        12000,
                        "rusticdelight:bell_peppers"
                )
        );
        //乡村乐事：浅色甜椒（随机类别：橙/白/粉均等 + 额外池，合计期望1.35）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "rusticdelight/pale_bell_peppers"),
                createCultivatingRecipe(
                        "rusticdelight",
                        "rusticdelight:pale_bell_pepper_seeds",
                        new String[]{"rusticdelight:bell_pepper_orange,1,0.45", "rusticdelight:bell_pepper_white,1,0.45", "rusticdelight:bell_pepper_pink,1,0.45", "rusticdelight:pale_bell_pepper_seeds,1", "rusticdelight:pale_bell_pepper_seeds,1,0.5"},
                        12000,
                        "rusticdelight:pale_bell_peppers"
                )
        );
        //乡村乐事：深色甜椒（随机类别：蓝/紫/黑均等 + 额外池，合计期望1.35）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "rusticdelight/dark_bell_peppers"),
                createCultivatingRecipe(
                        "rusticdelight",
                        "rusticdelight:dark_bell_pepper_seeds",
                        new String[]{"rusticdelight:bell_pepper_blue,1,0.45", "rusticdelight:bell_pepper_purple,1,0.45", "rusticdelight:bell_pepper_black,1,0.45", "rusticdelight:dark_bell_pepper_seeds,1", "rusticdelight:dark_bell_pepper_seeds,1,0.5"},
                        12000,
                        "rusticdelight:dark_bell_peppers"
                )
        );
    }

    private void generateKaleidoscopeTavernRecipes(RecipeAcceptor acceptor) {
        //万花筒酒馆：葡萄（剪刀收3串+30%额外1-2青提；种子=葡萄藤，对应种藤架上）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "kaleidoscopetavern/grape"),
                createCultivatingRecipe(
                        "kaleidoscope_tavern",
                        "kaleidoscope_tavern:grapevine",
                        new String[]{"kaleidoscope_tavern:grape,3", "kaleidoscope_tavern:green_grape,1,0.45", "kaleidoscope_tavern:grapevine,1"},
                        12000,
                        "kaleidoscope_tavern:grape_crop"
                )
        );
        //万花筒酒馆：冰葡萄（品种由藤架下方方块决定，罐内抽象为种子自持）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "kaleidoscopetavern/ice_grape"),
                createCultivatingRecipe(
                        "kaleidoscope_tavern",
                        "kaleidoscope_tavern:ice_grape",
                        new String[]{"kaleidoscope_tavern:ice_grape,3", "kaleidoscope_tavern:green_grape,1,0.45", "kaleidoscope_tavern:ice_grape,1"},
                        12000,
                        "kaleidoscope_tavern:ice_grape_crop"
                )
        );
        //万花筒酒馆：金葡萄（同上）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "kaleidoscopetavern/gold_grape"),
                createCultivatingRecipe(
                        "kaleidoscope_tavern",
                        "kaleidoscope_tavern:gold_grape",
                        new String[]{"kaleidoscope_tavern:gold_grape,3", "kaleidoscope_tavern:green_grape,1,0.45", "kaleidoscope_tavern:gold_grape,1"},
                        12000,
                        "kaleidoscope_tavern:gold_grape_crop"
                )
        );
    }

    private void generateCornDelightRecipes(RecipeAcceptor acceptor) {
        //玉米乐事：玉米（FD式双层作物，堆叠栽培；真正的种子是玉米粒，产出只有玉米）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "corndelight/corn"),
                createStackingRecipe(
                        "corn_delight",
                        "corn_delight:corn_seeds",
                        "corn_delight:corn,1",
                        1,
                        12000,
                        "corn_delight:corn_crop",
                        null,
                        true,
                        2,
                        2
                )
        );
    }

    private void generatePineappleDelightRecipes(RecipeAcceptor acceptor) {
        //菠萝乐事：菠萝（耕地成塾：1菠萝，收成后不掉种子；种子是菠萝芽 pineapple_crop）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "pineappledelight/pineapple"),
                createCultivatingRecipe(
                        "pineapple_delight",
                        "pineapple_delight:pineapple_crop",
                        new String[]{"pineapple_delight:pineapple,2", "pineapple_delight:pineapple,1,0.5"},
                        12000,
                        "pineapple_delight:pineapple_crop"
                )
        );
    }

    private void generateMyNethersDelightRecipes(RecipeAcceptor acceptor) {
        //我的下界乐事：粉蔗（powder cannon 为种苗；耕地成塾：成年时掉1个powder_cannon苗+点嬉后2-3子弹椒）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "mynethersdelight/powdery_cane"),
                createCultivatingRecipe(
                        "mynethersdelight",
                        "mynethersdelight:powder_cannon",
                        new String[]{"mynethersdelight:powder_cannon,1", "mynethersdelight:bullet_pepper,2,0.5", "mynethersdelight:bullet_pepper,1,0.5"},
                        12000,
                        "mynethersdelight:powdery_cane"
                )
        );
        //我的下界乐事：子弹椒（ bullet_pepper 既是作物又是果实；耕地点嬉后掉1个）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "mynethersdelight/bullet_pepper"),
                createCultivatingRecipe(
                        "mynethersdelight",
                        "mynethersdelight:bullet_pepper",
                        new String[]{"mynethersdelight:bullet_pepper,2", "mynethersdelight:bullet_pepper,1,0.5"},
                        12000,
                        "mynethersdelight:bullet_pepper"
                )
        );
        //我的下界乐事：绯红菌（colony = 种苗+作物；耕地成塾：2-5菌，均值3）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "mynethersdelight/crimson_colony"),
                createCultivatingRecipe(
                        "mynethersdelight",
                        "mynethersdelight:crimson_fungus_colony",
                        new String[]{"minecraft:crimson_fungus,3", "minecraft:crimson_fungus,1,0.5", "mynethersdelight:crimson_fungus_colony,1"},
                        12000,
                        "mynethersdelight:crimson_fungus_colony"
                )
        );
        //我的下界乐事：诡异菌（同绯红）
        acceptor.accept(
                ResourceLocation.fromNamespaceAndPath("create_cultivation", "mynethersdelight/warped_colony"),
                createCultivatingRecipe(
                        "mynethersdelight",
                        "mynethersdelight:warped_fungus_colony",
                        new String[]{"minecraft:warped_fungus,3", "minecraft:warped_fungus,1,0.5", "mynethersdelight:warped_fungus_colony,1"},
                        12000,
                        "mynethersdelight:warped_fungus_colony"
                )
        );
    }

    private JsonObject createCultivatingRecipe(String modIdCondition, String ingredientId, String[] results, int duration, String cropBlockId) {
        JsonObject json = new JsonObject();
        JsonArray conditions = new JsonArray();
        JsonObject modLoaded = new JsonObject();
        modLoaded.addProperty("type", "neoforge:mod_loaded");
        modLoaded.addProperty("modid", modIdCondition);
        conditions.add(modLoaded);
        json.add("neoforge:conditions", conditions);

        json.addProperty("type", CCRecipes.CULTIVATING.getId().toString());

        JsonArray ingredientsArray = new JsonArray();
        JsonObject ingredientJson = new JsonObject();
        ingredientJson.addProperty("item", ingredientId);
        ingredientsArray.add(ingredientJson);
        json.add("ingredients", ingredientsArray);

        JsonArray resultsArray = new JsonArray();
        for (String resultStr : results) {
            String[] parts = resultStr.split(",");
            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("id", parts[0]);
            resultJson.addProperty("count", Integer.parseInt(parts[1]));
            if (parts.length > 2) {
                resultJson.addProperty("chance", Float.parseFloat(parts[2]));
            }
            resultsArray.add(resultJson);
        }
        json.add("results", resultsArray);

        json.addProperty("processingDuration", duration);
        json.addProperty("crop_block", cropBlockId);

        return json;
    }

    /**
     * Builds a stacking cultivation recipe with the mod-loaded condition.
     * {@code minHeight} gates growth on the tank stack height (1 = no gate).
     * Stacking recipes use a single {@code result} output that scales with
     * the harvested layers.
     */
    private JsonObject createStackingRecipe(String modIdCondition, String ingredientId, String resultId, int resultCount,
                                            int duration, String blockToRenderId, String topRenderId, boolean stageByProgress,
                                            int maxHeight, int minHeight) {
        JsonObject json = new JsonObject();
        JsonArray conditions = new JsonArray();
        JsonObject modLoaded = new JsonObject();
        modLoaded.addProperty("type", "neoforge:mod_loaded");
        modLoaded.addProperty("modid", modIdCondition);
        conditions.add(modLoaded);
        json.add("neoforge:conditions", conditions);

        json.addProperty("type", CCRecipes.STACKING_CULTIVATING.getId().toString());

        JsonObject ingredientJson = new JsonObject();
        ingredientJson.addProperty("item", ingredientId);
        json.add("ingredient", ingredientJson);

        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("id", resultId);
        resultJson.addProperty("count", resultCount);
        json.add("result", resultJson);

        json.addProperty("processingDuration", duration);
        json.addProperty("block_to_render", blockToRenderId);
        if (topRenderId != null && !topRenderId.isBlank()) {
            json.addProperty("top_render", topRenderId);
        }
        if (stageByProgress) {
            json.addProperty("stage_by_progress", true);
        }
        json.addProperty("maxHeight", maxHeight);
        if (minHeight > 1) {
            json.addProperty("min_height", minHeight);
        }
        return json;
    }

    @Override
    public String getName() {
        return "Create Cultivation Compat Recipes";
    }

    @FunctionalInterface
    protected interface RecipeAcceptor {
        void accept(ResourceLocation id, JsonObject recipe);
    }
}