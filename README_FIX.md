# Create: Cultivation Craft — Create 6.0.10 兼容修复

## 崩溃根因

启动游戏时 `create_cultivation-0.1.3.jar` 抛出：

```
java.lang.NoClassDefFoundError: com/simibubi/create/foundation/fluid/FluidIngredient
```

原因：

- Create 模组自 **6.0.7** 起把旧的 `com.simibubi.create.foundation.fluid.FluidIngredient`
  重命名为 `FluidIngredientOld`，并标记 `@Deprecated(since = "6.0.7", forRemoval = true)`、
  `@ScheduledForRemoval(inVersion = "1.21.1+ Port")`。
- 也就是说 Create 6.0.7+ 的 jar 里**不再存在** `FluidIngredient` 这个类，
  真正替代它的是 NeoForge 的 `net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient`。
- `create_cultivation-0.1.3` 的 `neoforge.mods.toml` 声明依赖 `create [6.0.6,6.1.0)`，
  但代码实际只兼容到 6.0.6；版本范围“虚标”导致它能在 6.0.10 下被允许加载，随后在
  RegisterEvent 阶段实例化 `CultivatingRecipeParams` 时崩溃。

## 所做的修改

1. `CultivatingRecipeParams.java`
   - 删除 `import com.simibubi.create.foundation.fluid.FluidIngredient;`
   - 改用 `com.simibubi.create.foundation.codec.CreateCodecs` 与
     `net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient`
   - `FluidIngredient.CODEC` → `CreateCodecs.SIZED_FLUID_INGREDIENT`
   - `List<Either<FluidIngredient, Ingredient>>` → `List<Either<SizedFluidIngredient, Ingredient>>`

2. `CultivationBaseBlockEntity.java`
   - `output.rollOutput()` → `output.rollOutput(level.getRandom())`
     （Create 6.0.10 中 `ProcessingOutput.rollOutput` 需要传入 `RandomSource`）

3. `CCRecipeProvider.java`
   - `protected void buildRecipes(...)` → `public void buildRecipes(...)`
     （Create 6.0.10 中基类 `buildRecipes` 为 `public`）

4. `build.gradle`
   - Create/Ponder/Flywheel/Registrate 改为从项目内 `libs/` 目录加载本地 jar
     （与运行环境完全一致的版本），不再依赖 Maven 坐标匹配。
   - 仅保留 Jade 作为 dependency。

5. `gradle.properties`
   - `neo_version = 21.1.248`
   - `mod_version = 0.1.4`
   - create/ponder/flywheel/registrate 版本号更新为与运行环境一致。

6. `src/main/templates/META-INF/neoforge.mods.toml`
   - Create 依赖范围 `[6.0.6,6.1.0)` → `[6.0.10,6.1.0)`

## 编译验证

在 `CreateCultivationCraft-1.21-dev/CreateCultivationCraft-1.21-dev` 下：

```
gradle.bat compileJava --no-daemon --no-configuration-cache
gradle.bat jar      --no-daemon --no-configuration-cache
```

使用本机 `D:\gradle-9.0.0-bin\gradle-9.0.0\bin\gradle.bat` 构建，
`compileJava` 通过（仅剩无害的废弃 API 警告），并成功产出
`build/libs/create_cultivation-0.1.4.jar`。

## 使用方法

把 `create_cultivation-0.1.4.jar` 放入
`F:\MinecraftPacks\versions\create\mods\`，删除旧的 `create_cultivation-0.1.3.jar`
即可。该 jar 声明依赖：

- NeoForge `>= 21.1.248`
- Minecraft `1.21.1`
- Create `>= 6.0.10, < 6.1.0`

仍需要 Create 6.0.10（已包含 Ponder 1.0.82 / Flywheel 1.0.6 / Registrate +67，随 Create 打包装入）。

## 游戏内配置界面

本模组复用了 NeoForge 自带的配置界面（与 Create 同一个入口）：

`游戏内 Mods（模组）界面 → 选择 create_cultivation → Config（配置）`

配置项（`config/create_cultivation-common.toml`，改动即时生效）：

| 配置项 | 默认 | 范围 | 说明 |
| --- | --- | --- | --- |
| `growthRateMultiplier` | 1.0 | 0.05–20 | 作物生长速率倍率 |
| `cropYieldMultiplier` | 1.0 | 0.1–64 | 每次收获的作物产出数量倍率 |
| `wateringYieldBonus` | 1.0 | 1.0–10 | 被注液器浇水时，额外叠加到产出倍率上的加成 |
| `wateredDuration` | 2 | 1–600 | 浇水状态持续的懒刻数（约每 0.5 秒一懒刻） |

说明：`cropYieldMultiplier` 会同时作用于阶段式与堆叠式收获；
`growthRateMultiplier` 会同时加速普通作物与堆叠作物的生长进度。

## JEI 配方显示支持

本模组内置了一个 JEI 插件（`compat/jei`），在安装了 **JEI 19.44.0+（NeoForge 版）** 时生效：

- 新增两个 JEI 配方分类：**栽培（Cultivating）** 与 **堆叠栽培（Stacking Cultivation）**。
- 在 JEI 中点击种子 / 作物时，可查看对应的栽培配方（输入种子 → 输出作物）。
- 界面采用与机械动力本体一致的排版：**左侧种子输入、中间动力元件（栽培罐 + 栽培基座）、右侧作物产物堆叠**，高度紧凑。
- 界面左侧会显示 **浇灌流体**（未指定时默认水，例如下界疣配方为岩浆）。
- 界面顶部会显示 **生长时间**。
- **栽培基座** 与 **栽培罐** 会作为“配方催化剂”注册：在 JEI 里点击这两个方块即可看到本模组相关的栽培配方。

该支持是**可选**的：不安装 JEI 时本模组照常运行，JEI 相关类不会被加载。

## 显示连接器（Display Link）联动

栽培罐现已注册了一个 Create 显示源 **“Cultivation Info / 栽培信息”**：

用显示连接器（Display Link）对准栽培罐，即可在上面显示：

- 第一行：当前**作物种类**（如 小麦 / 甘蔗）。
- 第二行：**剩余生长时间**（基于基础生长时间估算）。

未种植时显示“无作物”。该源在显示连接器的配置界面中可选。

## 流体灌溉（针对其他作物模组扩展）

栽培配方现在支持可选字段 **`irrigant`**，用于指定“只有用该流体浇灌才能提升产量”：

```json
{
  "type": "create_cultivation:cultivating",
  "crop_block": "minecraft:nether_wart",
  "irrigant": "minecraft:lava",
  "...": "..."
}
```

- 字段为单个流体 ID（如 `minecraft:lava` / `minecraft:water`），未填写时默认水。
- 注液器（Spout）倒入的流体只有与该字段匹配时才会计入“浇水”，从而获得产出加成。
- JEI 中会显示该配方所接受的浇灌流体。

原版示例：**下界疣**（`nether_wart.json`）已设置为用岩浆灌溉。其他作物模组可通过自定义数据包覆盖/新增配方来指定各自需要的流体。

## 构建产物

- 成品：`create_cultivation-0.1.4.jar`

## 说明

- 部分代码（`CreateCodecs.SIZED_FLUID_INGREDIENT`、`ProcessingOutput.CODEC`、
  `EventBusSubscriber.bus` 等）在 Create 6.0.10 中仍标记为“即将移除”，
  但当前仍存在且可用，仅产生编译警告，不影响运行。
