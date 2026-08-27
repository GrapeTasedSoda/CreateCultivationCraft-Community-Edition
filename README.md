# Create: Cultivation Craft — Community Edition

A Create mod addon that provides a fully automatic crop cultivation system.

**机械动力：栽培 — 社区版**

一个为机械动力（Create）模组添加的全自动作物栽培附属模组。

---

## 原版介绍 / Original Features

本模组添加了一个新的多方块机器：**栽培罐（Cultivation Tank）**。它为各类植物提供了从播种、生长到收获的全自动化解决方案，支持：

- 原版阶段式作物（小麦、胡萝卜、马铃薯等）
- 花卉
- 堆叠式植物（甘蔗、仙人掌等）
- 通过数据包兼容其他模组的作物（如农夫乐事）

---

## 社区版改动 / Community Edition Changes

原版 `create_cultivation-0.1.3` 仅兼容 Create 6.0.6，在 Create 6.0.10 下会直接崩溃。本社区版将模组移植到 **Create 6.0.10 + NeoForge 21.1.248**，并新增了多项功能。

### 崩溃修复 / Crash Fixes

原版在 Create 6.0.10 下启动时报错：
```
java.lang.NoClassDefFoundError: com/simibubi/create/foundation/fluid/FluidIngredient
```

Create 自 6.0.7 起移除了 `FluidIngredient` 类，改用 NeoForge 的 `SizedFluidIngredient`。本社区版已完成以下适配：

| 文件 | 修改内容 |
|------|----------|
| `CultivatingRecipeParams.java` | `FluidIngredient` → `SizedFluidIngredient`，`FluidIngredient.CODEC` → `CreateCodecs.SIZED_FLUID_INGREDIENT` |
| `CultivationBaseBlockEntity.java` | `output.rollOutput()` → `output.rollOutput(level.getRandom())` （适配 Create 6.0.10 新签名） |
| `CCRecipeProvider.java` | `buildRecipes` 访问修饰符 `protected` → `public` |
| `neoforge.mods.toml` | Create 依赖范围 `[6.0.6,6.1.0)` → `[6.0.10,6.1.0)` |

### 构建系统 / Build System

- Create / Ponder / Flywheel / Registrate 改为从项目内 `libs/` 目录加载本地 jar，确保与运行环境版本完全一致。
- 版本号更新：`mod_version = 0.1.4`

### 新增功能 / New Features

#### JEI 配方显示 / JEI Recipe Integration

安装 JEI 19.44.0+（NeoForge 版）时自动启用：

- 新增 **栽培（Cultivating）** 与 **堆叠栽培（Stacking Cultivation）** 两个配方分类。
- 点击种子/作物可查看栽培配方（输入种子 → 输出作物）。
- 界面显示：浇灌流体、生长时间、配方催化剂。
- 栽培基座与栽培罐注册为 JEI 配方催化剂，点击即可查看相关配方。
- 不安装 JEI 时不影响模组运行。

#### 显示连接器联动 / Display Link Integration

栽培罐注册了 Create 显示源 **"Cultivation Info / 栽培信息"**：

- 第一行：当前作物种类（如 小麦 / 甘蔗）
- 第二行：剩余生长时间
- 未种植时显示"无作物"

#### 流体灌溉系统 / Fluid Irrigation

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

### 游戏内配置 / In-Game Config

通过 `游戏内 Mods 界面 → create_cultivation → Config` 或直接编辑 `config/create_cultivation-common.toml`：

| 配置项 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `growthRateMultiplier` | 1.0 | 0.05–20 | 作物生长速率倍率 |
| `cropYieldMultiplier` | 1.0 | 0.1–64 | 作物产出数量倍率 |
| `wateringYieldBonus` | 1.0 | 1.0–10 | 浇水时额外产出加成 |
| `wateredDuration` | 2 | 1–600 | 浇水状态持续懒刻数 |

---

## 环境要求 / Requirements

- **Minecraft** 1.21.1
- **NeoForge** ≥ 21.1.248
- **Create** ≥ 6.0.10, < 6.1.0（含 Ponder 1.0.82 / Flywheel 1.0.6 / Registrate +67）
- 可选：**JEI** ≥ 19.44.0（NeoForge 版）

---

## 版本 / Version

**0.1.4** (Community Edition)

---

## 许可 / License

与原版保持一致，详见 [LICENSE.txt](LICENSE.txt)。