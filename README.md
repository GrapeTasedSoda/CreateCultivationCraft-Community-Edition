# Create: Cultivation Craft — Community Edition


---

## English

**Create: Cultivation Craft — Community Edition**

A Create mod addon that provides a fully automatic crop cultivation system.

### Original Features

This mod adds a new multi-block machine: the **Cultivation Tank**. It automates the planting, growing, and harvesting processes for a wide variety of plants, including:

- Vanilla staged crops (wheat, carrots, potatoes, etc.)
- Flowers
- Stacking plants (sugar cane, cactus, etc.)
- Data-pack compatible crops from other mods (e.g., Farmer's Delight)

### Community Edition Changes

The original `create_cultivation-0.1.3` only supported Create 6.0.6 and would crash immediately under Create 6.0.10. This Community Edition ports the mod to **Create 6.0.10 + NeoForge 21.1.248** and adds several new features.

#### Crash Fixes

The original mod crashed on startup under Create 6.0.10 with:

```
java.lang.NoClassDefFoundError: com/simibubi/create/foundation/fluid/FluidIngredient
```

Create 6.0.7+ removed the `FluidIngredient` class in favor of NeoForge's `SizedFluidIngredient`. The following files were adapted:

| File | Change |
|------|--------|
| `CultivatingRecipeParams.java` | `FluidIngredient` → `SizedFluidIngredient`, `FluidIngredient.CODEC` → `CreateCodecs.SIZED_FLUID_INGREDIENT` |
| `CultivationBaseBlockEntity.java` | `output.rollOutput()` → `output.rollOutput(level.getRandom())` (new Create 6.0.10 signature) |
| `CCRecipeProvider.java` | `buildRecipes` access modifier `protected` → `public` |
| `neoforge.mods.toml` | Create dependency range `[6.0.6,6.1.0)` → `[6.0.10,6.1.0)` |

#### Build System

- Create / Ponder / Flywheel / Registrate are loaded from local `libs/` directory jars instead of Maven coordinates, ensuring exact version parity with the runtime environment.
- Version bumped to `mod_version = 0.1.4`

#### New Features

**Cultivation Base Storage GUI**

Right-click the Cultivation Base (with any item, no need for an empty hand) to open its GUI:

- 8 output slots for harvested items plus a dedicated catalyst slot.
- Holding a Cultivation Tank and right-clicking the base places the tank on top directly instead of opening the GUI.

**Catalyst System**

- Insert catalyst items (bone meal by default) into the base GUI; the machine boosts crop growth by 3x.
- One catalyst is consumed every 30 seconds; recipes can override the item type and per-item duration via `catalyst` / `catalyst_use`.
- The catalyst bar timer is renewed on each insertion, so topping up never loses progress.
- GUI animations (catalyst arrows / water lines) run on a wall-clock timer and stay immune to server lag and time sync.

**JEI Recipe Integration**

Automatically enabled when JEI 19.44.0+ (NeoForge) is installed:

- Adds **Cultivating** and **Stacking Cultivation** recipe categories.
- Click a seed/crop to view its cultivation recipe (input seed → output crop).
- Displays: irrigant fluid, growth time, and the recipe catalyst above the arrow.
- Cultivation Base and Cultivation Tank are registered as JEI recipe catalysts.
- No JEI dependency — the mod works fine without it.

**Display Link Integration**

Both blocks register multiple Create display sources, selectable in the display link UI:

| Block | Source | Content |
|-------|--------|---------|
| Cultivation Tank | Crop Type | Current crop name (e.g., Wheat / Sugar Cane) |
| Cultivation Tank | Time Remaining | Remaining growth time |
| Cultivation Base | Base Output | Lists stored output items and counts |
| Cultivation Base | Base Catalyst Amount | e.g., "Remaining Bone Meal: 7" |
| Cultivation Base | Base Multipliers | Current yield multiplier and growth rate, live values |

Multiplier values update live with machine state (watering, catalyst, RPM).

**Fluid Irrigation System**

Cultivation recipes support an optional `irrigant` field:

```json
{
  "type": "create_cultivation:cultivating",
  "crop_block": "minecraft:nether_wart",
  "irrigant": "minecraft:lava"
}
```

- Defaults to water when omitted.
- Only matching fluid from a Spout triggers the watering bonus.
- Example: Nether Wart is configured to use lava.

**In-Game Config**

Access via `Mods screen → create_cultivation → Config` or edit `config/create_cultivation-common.toml`:

| Key | Default | Range | Description |
|-----|---------|-------|-------------|
| `growthRateMultiplier` | 1.0 | 0.05–20 | Crop growth rate multiplier |
| `cropYieldMultiplier` | 1.0 | 0.1–64 | Crop yield multiplier |
| `wateringYieldBonus` | 2.5 | 1.0–10 | Extra yield bonus when watered |
| `wateredDuration` | 2 | 1–600 | Watered status duration in ticks |
| `catalystGrowthBonus` | 3.0 | 1.0–10 | Growth multiplier while a catalyst is active |
| `waterCatalystSynergyBonus` | 1.5 | 1.0–10 | Extra multiplier for growth and yield while watered AND catalyzed |

#### Ponder

The machine ships with an in-game Ponder walkthrough covering the full loop: assembly, rotational power, planting, automatic harvesting, output extraction (funnel/chute), the catalyst system, spout watering bonuses, display link usage and tank stacking. Open it with the Create ponder on the base or tank.

#### Requirements

- **Minecraft** 1.21.1
- **NeoForge** ≥ 21.1.248
- **Create** ≥ 6.0.10, < 6.1.0 (includes Ponder 1.0.82 / Flywheel 1.0.6 / Registrate +67)
- Optional: **JEI** ≥ 19.44.0 (NeoForge)

#### Version

**0.1.4** (Community Edition)

#### License

Same as the original. See [LICENSE.txt](LICENSE.txt).

---

## 中文

**机械动力：栽培 — 社区版**

一个为机械动力（Create）模组添加的全自动作物栽培附属模组。

### 原版介绍

本模组添加了一个新的多方块机器：**栽培罐（Cultivation Tank）**。它为各类植物提供了从播种、生长到收获的全自动化解决方案，支持：

- 原版阶段式作物（小麦、胡萝卜、马铃薯等）
- 花卉
- 堆叠式植物（甘蔗、仙人掌等）
- 通过数据包兼容其他模组的作物（如农夫乐事）

### 社区版改动

原版 `create_cultivation-0.1.3` 仅兼容 Create 6.0.6，在 Create 6.0.10 下会直接崩溃。本社区版将模组移植到 **Create 6.0.10 + NeoForge 21.1.248**，并新增了多项功能。

#### 崩溃修复

原版在 Create 6.0.10 下启动时报错：

```
java.lang.NoClassDefFoundError: com/simibubi/create/foundation/fluid/FluidIngredient
```

Create 自 6.0.7 起移除了 `FluidIngredient` 类，改用 NeoForge 的 `SizedFluidIngredient`。本社区版已完成以下适配：

| 文件 | 修改内容 |
|------|----------|
| `CultivatingRecipeParams.java` | `FluidIngredient` → `SizedFluidIngredient`，`FluidIngredient.CODEC` → `CreateCodecs.SIZED_FLUID_INGREDIENT` |
| `CultivationBaseBlockEntity.java` | `output.rollOutput()` → `output.rollOutput(level.getRandom())`（适配 Create 6.0.10 新签名） |
| `CCRecipeProvider.java` | `buildRecipes` 访问修饰符 `protected` → `public` |
| `neoforge.mods.toml` | Create 依赖范围 `[6.0.6,6.1.0)` → `[6.0.10,6.1.0)` |

#### 构建系统

- Create / Ponder / Flywheel / Registrate 改为从项目内 `libs/` 目录加载本地 jar，确保与运行环境版本完全一致。
- 版本号更新：`mod_version = 0.1.4`

#### 新增功能

**栽培基座存储界面**

右键栽培基座（任意物品，无需空手）即可打开界面：

- 8 个产物槽用于存放收获物，另有 1 个专属催化剂槽。
- 手持栽培罐右键基座会直接把罐子放到基座上方，而不是打开界面。

**催化剂系统**

- 在基座界面放入催化剂物品（默认骨粉），作物生长速度提升至 3 倍。
- 每隔 30 秒消耗一份催化剂；配方可通过 `catalyst` / `catalyst_use` 字段自定义催化剂种类与单份时长。
- 催化剂计时器在每次放入时重新续期，中途补货不会损失剩余时间。
- 基座界面动画（催化剂箭头/水线）基于真实时钟驱动，不受服务器卡顿与时间同步影响。

**JEI 配方显示**

安装 JEI 19.44.0+（NeoForge 版）时自动启用：

- 新增 **栽培（Cultivating）** 与 **堆叠栽培（Stacking Cultivation）** 两个配方分类。
- 点击种子/作物可查看栽培配方（输入种子 → 输出作物）。
- 界面显示：浇灌流体、生长时间，以及箭头上方的配方催化剂。
- 栽培基座与栽培罐注册为 JEI 配方催化剂，点击即可查看相关配方。
- 不安装 JEI 时不影响模组运行。

**显示连接器联动**

两种方块都注册了多个 Create 显示源，可在显示连接器界面中切换：

| 方块 | 显示源 | 内容 |
|-------|--------|---------|
| 栽培罐 | 作物种类 | 当前作物名称（如 小麦 / 甘蔗） |
| 栽培罐 | 剩余生长时间 | 生长剩余时间 |
| 栽培基座 | 基座产物 | 列出已存储的产物种类与数量 |
| 栽培基座 | 基座催化剂数量 | 如「剩余骨粉：7个」 |
| 栽培基座 | 基座倍率 | 当前产物倍率与生产速率，实时数值 |

倍率显示会随机器状态实时更新（浇水、催化剂、转速）。

**流体灌溉系统**

栽培配方支持可选字段 `irrigant`，用于指定浇灌流体：

```json
{
  "type": "create_cultivation:cultivating",
  "crop_block": "minecraft:nether_wart",
  "irrigant": "minecraft:lava"
}
```

- 未填写时默认为水。
- 注液器（Spout）倒入的流体只有与配方匹配时才会触发浇水加成。
- 原版示例：下界疣已设置为用岩浆灌溉。

**游戏内配置**

通过 `游戏内 Mods 界面 → create_cultivation → Config` 或直接编辑 `config/create_cultivation-common.toml`：

| 配置项 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `growthRateMultiplier` | 1.0 | 0.05–20 | 作物生长速率倍率 |
| `cropYieldMultiplier` | 1.0 | 0.1–64 | 作物产出数量倍率 |
| `wateringYieldBonus` | 2.5 | 1.0–10 | 浇水时额外产出加成 |
| `wateredDuration` | 2 | 1–600 | 浇水状态持续刻数 |
| `catalystGrowthBonus` | 3.0 | 1.0–10 | 催化剂激活时的生长倍率 |
| `waterCatalystSynergyBonus` | 1.5 | 1.0–10 | 浇水与催化剂同时生效时的额外加成倍率 |

#### Ponder 教学场景

模组内置了覆盖完整玩法的 Ponder 教学场景：组装、动力供应、种植、自动收获、漏斗/溜槽取物、催化剂系统、注液器浇水加成、显示连接器用法与堆叠罐体。对着基座或栽培罐使用 Create 的 ponder 指引即可打开。

#### 环境要求

- **Minecraft** 1.21.1
- **NeoForge** ≥ 21.1.248
- **Create** ≥ 6.0.10, < 6.1.0（含 Ponder 1.0.82 / Flywheel 1.0.6 / Registrate +67）
- 可选：**JEI** ≥ 19.44.0（NeoForge 版）

#### 版本

**0.1.4** (Community Edition)

#### 许可

与原版保持一致，详见 [LICENSE.txt](LICENSE.txt)。
