# Create: Cultivation Craft — Community Edition

> Original mod by **[upo](https://modrinth.com/user/upo)** (MIT License) · Community Edition maintained by **GrapeTasedSoda**
>
> 原模组作者：**upo** · 社区版移植与维护：**GrapeTasedSoda**

A Create mod addon that provides a **fully automatic crop cultivation system**: toss seeds into the Cultivation Tank and the machine handles planting, growing, harvesting, replanting and storage — you just supply rotation power (or let funnels do the rest).

一个为 Create 打造的**全自动农业**附属模组：把种子丢进栽培罐，机器自己完成播种、生长、收获、补种、存储的全流程——你要做的只是接上动力（或者连收菜都交给漏斗）。

---

## English

### What's different from the original?

The original mod (v0.1.3) stopped at Create 6.0.6 and **crashes instantly** on current Create versions. This Community Edition ports it to **Create 6.0.10 + NeoForge 21.1.x** and adds a lot on top.

| | Original 0.1.3 | Community Edition 0.1.4 |
|---|---|---|
| Runtime | Create 6.0.6 only (crashes on newer) | **Create 6.0.10 + NeoForge 21.1.x** |
| Output storage | No interface | Base GUI: 8-slot output + fertilizer slot |
| Fertilizers | None | bone_meal / organic compost / letios compost / **Efficient Fertilizer**, all configurable |
| Watering bonus | None | Spout watering: growth ×2, yield ×1.5, plus synergy bonus with fertilizers |
| Machine status | None | 🔴 red / 🟠 orange dual alarm lamps + GUI border + Jade hints |
| New items | None | Efficient Fertilizer (Mechanical Mixer recipes) |
| Mod integration | DIY datapacks required | **Built-in recipes for 23 crops across 7 mods** |
| Info displays | None | Jade, 5 Display Link readouts, JEI recipe categories |
| Stability | Progress overflow crashes at high speed | Many edge-case fixes (see below) |

> Technical porting details (crash root cause, API migration list): [README_FIX.md](README_FIX.md)

### What can players experience?

#### 🌱 Fully automatic farming
- Stack the **Cultivation Tank** on a **Cultivation Base**, right-click the tank with seeds to plant;
- The machine grows, harvests into the base's storage, and **keeps one seed for replanting** — as long as storage isn't full it runs forever;
- Supports staged crops (wheat, carrots…), flowers, and **stacking crops** that grow layer by layer (sugar cane, cactus, kelp, bamboo…);
- Extract outputs with funnels/funnels — drops straight into existing logistics;
- Tanks stack upward: taller tank = taller stacking crops.

#### 🌡️ Dual alarm lamps
- 🔴 **Red — output almost full**: the machine predicts whether the *next* harvest fits; if not it pauses early (fertilizer timer and watering included), never wasting resources, and resumes automatically once space frees up;
- 🟠 **Orange — tank too short**: some crops need a minimum tank height (rice needs 2+). The lamp explains why nothing is growing — **no growth, no fertilizer consumed, watering politely refused**.

#### 🌿 Fertilizer system
- Dedicated fertilizer slot in the base GUI; multiplies growth and yield while active, auto-renews from the next item;
- Defaults: bone_meal (30s, growth ×2 / yield ×1.5), Farmer's Delight organic compost (90s, ×4 / ×2), My Nether's Delight letios compost (90s, ×2 / ×4);
- **New item: Efficient Fertilizer** (45s, growth ×3 / yield ×2), made in the Mechanical Mixer:
  - bone_meal + rotten flesh + 100mB water → 2;
  - + Farmer's Delight bark or straw + 250mB water → 4.

#### 💧 Watering bonus
- Water the tank with a Spout: growth ×2, yield ×1.5;
- Watering + fertilizer together trigger a **synergy bonus** (another ×1.5);
- Recipes can define a custom irrigant fluid — e.g. nether wart wants lava.

#### 📊 Info integration
- **Jade**: crop name, maturity and a second-accurate growth countdown on sight; short tanks get an honest "height mismatch" instead of a misleading "mature";
- **Display Link** (Create): 5 readouts — crop name, remaining time, base output list, fertilizer stock, live multipliers;
- **JEI**: two new recipe categories ("Cultivating" / "Stacking Cultivating") with irrigant and fertilizer info.

#### 📖 Built-in tutorials
Use Create's Ponder on the base or tank: full illustrated tutorials covering assembly, rotation, planting, harvesting, item retrieval, fertilizers, watering, display links and tank stacking.

#### ⚙️ In-game config
`Mods → Create: Cultivation Craft → Config`: growth rate, yield multiplier, watering bonuses, synergy, watering duration, and the whole fertilizer table (item / duration / both multipliers per entry).

### Mod compatibility

All recipes are **built in** and auto-enable when the target mod is installed (no config needed). Mod names kept in English:

| Mod | Crops | Notes |
|-----|-------|-------|
| **Farmer's Delight** | cabbage, onion, rice, tomato | organic compost doubles as a top-tier fertilizer |
| **Rustic Delight** | cotton, coffee, bell peppers (red/green/pale) | bell pepper colors roll by weight |
| **Kaleidoscope Cookery** | tomato, chili, lettuce, rice, wild rice | seeds returned on harvest; rice / wild rice need 2+ tank height |
| **Kaleidoscope Tavern** | grape, ice grape, gold grape | hanging-crop rendering, vines kept for replant |
| **Corn Delight** | corn | stacked growth, needs 2+ tank height |
| **Pineapple Delight** | pineapple | auto-replants from the stump |
| **My Nether's Delight** | powdery cane, bullet pepper, crimson colony, warped colony | letios compost doubles as a fertilizer |

### Stability fixes (vs the original)

- Stage crops no longer crash the renderer/server when progress overflows at extreme speed;
- Crops no longer get permanently stuck when a datapack reload removes their recipe (auto-cleared with a log);
- Jade no longer reports "mature" on short tanks; crop models no longer render fully mature while the height alarm is on;
- Fertilizer no longer silently consumed; alarm animations no longer stuck after the alarm clears;
- Working-state refresh and a dozen more edge cases.

### Requirements

- Minecraft **1.21.1**
- NeoForge **≥ 21.1.248**
- Create **≥ 6.0.10, < 6.1.0**
- Optional: JEI, Jade, and any mod from the compatibility table

### Credits

- **upo** — original author of Create: Cultivation Craft (MIT, see [LICENSE.txt](LICENSE.txt))
- **GrapeTasedSoda** — Community Edition port, fixes and new features

---

## 简体中文

### 这个移植版和原版有什么区别？

原模组（v0.1.3）停更于 Create 6.0.6 时代，在现行版本的 Create 下**加载即崩溃**。社区版做了完整的兼容移植，并在此之上加入了大量新功能与体验优化。

| | 原版 0.1.3 | 社区版 0.1.4 |
|---|---|---|
| 运行环境 | 仅 Create 6.0.6（新版直接崩溃） | **Create 6.0.10 + NeoForge 21.1.x** |
| 产物存储 | 无界面 | 基座 GUI：8 格产物仓 + 肥料槽 |
| 肥料 | 无 | bone_meal / organic compost / letios compost / **Efficient Fertilizer**，效果均可配置 |
| 浇水加成 | 无 | Spout 浇水：生长 ×2、产量 ×1.5，与肥料还有协同加成 |
| 机器状态提示 | 无 | 🔴 红灯 / 🟠 橙灯 双警示灯 + GUI 边框 + Jade 提示 |
| 新物品 | 无 | Efficient Fertilizer（机械搅拌合成） |
| 模组联动 | 需自行写数据包 | **内置 7 个模组 23 种作物的栽培配方** |
| 信息显示 | 无 | Jade 联动、Display Link 5 种读数、JEI 配方查询 |
| 稳定性 | 高倍率下进度溢出可崩溃 | 大量边界修复（详见下方"稳定性修复"） |

> 技术细节（崩溃根因分析、API 适配清单）见 [README_FIX.md](README_FIX.md)。

### 玩家能体验到什么？

#### 🌱 全自动种植机
- **Cultivation Tank**（栽培罐）叠在 **Cultivation Base**（栽培基座）上，对罐子右键放入种子即完成种植；
- 机器自动生长、成熟后自动收获进基座仓库，**并自动留种补种**——只要仓库不塞满，机器永远转下去；
- 支持阶段式作物（wheat、carrot……）、花卉，以及 sugar cane / cactus / kelp / bamboo 这类**逐层长高**的堆叠作物；
- 产物用漏斗 / 溜槽即可抽取，完美接入现有物流系统；
- 罐体本身可以向上堆叠：更高的罐子 = 更高的堆叠作物上限。

#### 🌡️ 一眼读懂机器状态：双警示灯
- 🔴 **红灯——仓库快满了**：机器会预测"下一次收获能不能装下"，装不下就提前暂停（连同肥料倒计时、浇水一起），绝不浪费物料；腾出空间后自动复工；
- 🟠 **橙灯——罐子太矮**：某些作物有最低罐高要求（rice 要 2 格），不满足时机器亮橙灯说明原因，**此时不生长、不消耗肥料、浇水也会被礼貌拒绝**——不会白烧你的资源。

#### 🌿 肥料系统
- 基座 GUI 里有专属肥料槽，肥料生效期间生长与产量获得倍率加成，一份消耗完自动续上下一份；
- 默认肥料表：bone_meal（30 秒，生长 ×2 / 产量 ×1.5）、Farmer's Delight 的 organic compost（90 秒，×4 / ×2）、My Nether's Delight 的 letios compost（90 秒，×2 / ×4）；
- **新物品：Efficient Fertilizer**（45 秒，生长 ×3 / 产量 ×2），用 Mechanical Mixer 生产：
  - bone_meal + rotten flesh + 100mB 水 → 2 个；
  - 再加一份 Farmer's Delight 的 tree bark 或 straw + 250mB 水 → 4 个。

#### 💧 浇水加成
- 用 Spout 给罐子浇水：生长 ×2、产量 ×1.5；
- 浇水 + 肥料同时生效触发**协同加成**（再 ×1.5）；
- 配方可以自定义灌溉流体——比如 nether wart 要用岩浆浇才给加成。

#### 📊 信息联动
- **Jade**：看向机器即可显示当前作物、是否成熟、精确到秒的剩余生长时间；罐高不足时直接告诉你"高度不满足栽培需求"，而不是误导性的"已成熟"；
- **Display Link**（Create）：作物名称、剩余生长时间、基座产物清单、肥料余量、实时倍率五种读数可选，接上显示屏就能给农场装一块状态板；
- **JEI**：新增"栽培 / 堆叠栽培"两个配方分类，点击任意作物即可查询种法（含灌溉流体与所需肥料）。

#### 📖 内置教学
对着基座或罐子使用 Create 的 Ponder，有覆盖完整流程的图文教学：组装、动力、种植、收获、取物、肥料、浇水、Display Link 与罐体堆叠。

#### ⚙️ 游戏内配置
`Mods 界面 → Create: Cultivation Craft → Config`，可调生长速率、产量倍率、浇水加成、协同加成、浇水时长，以及整张肥料表（每条肥料的物品 / 时长 / 双倍率）。

### 模组联动目录

以下配方**内置在本模组**，安装对应模组后自动启用（未安装则不加载，无需任何配置）。模组与物品名保持英文：

| 模组 | 联动作物 | 备注 |
|------|----------|------|
| **Farmer's Delight** | cabbage、onion、rice、tomato | organic compost 可作高级肥料 |
| **Rustic Delight** | cotton、coffee、bell peppers（红/绿/白三种） | bell pepper 按权重随机产出三种颜色 |
| **Kaleidoscope Cookery** | tomato、chili、lettuce、rice、wild rice | 收获自动返还种子；rice / wild rice 需 2 格以上罐高 |
| **Kaleidoscope Tavern** | grape、ice grape、gold grape | 悬挂式作物渲染，收获保留 grapevine 续种 |
| **Corn Delight** | corn | 堆叠式生长，需 2 格以上罐高 |
| **Pineapple Delight** | pineapple | 收获自动留芽补种 |
| **My Nether's Delight** | powdery cane、bullet pepper、crimson colony、warped colony | letios compost 可作高级肥料 |

### 稳定性修复（相对原版）

这些是实际游玩中会遇到的问题，社区版已全部处理：

- 阶段作物在超高速运行下进度溢出，导致**渲染崩溃或服务端崩溃**；
- 数据包重载后配方丢失，罐中作物**永久卡死**（现在会自动清除并记录日志）；
- 罐高不足时 Jade 误报"已成熟"、作物模型错误显示成熟形态；
- 肥料被无声消耗、警示解除后 GUI 动画永不停止；
- 拆除基座后罐子的工作状态延迟刷新等十余项边界问题。

### 安装需求

- Minecraft **1.21.1**
- NeoForge **≥ 21.1.248**
- Create **≥ 6.0.10, < 6.1.0**
- 可选：JEI（配方查询）、Jade（状态提示）及上方联动目录中的任意模组

### 致谢

- **upo** —— Create: Cultivation Craft 原模组作者（MIT 许可，见 [LICENSE.txt](LICENSE.txt)）
- **GrapeTasedSoda** —— 社区版移植、修复与新功能

---

## 版本 / Version

**0.1.4** (Community Edition)
