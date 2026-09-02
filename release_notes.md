# Create: Cultivation Craft v0.1.4

> Original mod by **upo** · Community Edition port & maintenance by **GrapeTasedSoda**
>
> 原模组作者：**upo** · 社区版移植与维护：**GrapeTasedSoda**

A Create addon for fully automatic crop farming. First public release of the Community Edition.

Create 自动化种植附属模组，社区版首次正式发布。

## New / 新增

**Crop compatibility for 6 mods / 六个模组的作物兼容**（auto-enables when the mod is installed / 安装对应模组后自动启用）
- Rustic Delight: cotton, coffee, and bell peppers — 3 pepper crops rolling 9 colors by weight / cotton、coffee、bell peppers——3 种 pepper 植株按权重随机产出 9 种颜色
- Kaleidoscope Cookery: tomato, chili, lettuce, rice, **wild rice** (new) / tomato、chili、lettuce、rice、**wild rice**（新增）
- Kaleidoscope Tavern: grape, ice grape, gold grape with hanging-crop rendering / grape、ice grape、gold grape（悬挂式渲染）
- Corn Delight / Pineapple Delight: corn, pineapple with auto-replanting / corn、pineapple（收获自动留芽补种）
- My Nether's Delight: powdery cane, bullet pepper, crimson colony, warped colony / powdery cane、bullet pepper、crimson colony、warped colony

**New item: Efficient Fertilizer / 新物品：Efficient Fertilizer**
- Fertilizer slot item: 45s, growth ×3, yield ×2 / 肥料槽可用：持续 45 秒，生长 ×3，产量 ×2
- Mechanical Mixer recipes: bone_meal + rotten flesh + 100mB water → 2; with Farmer's Delight installed: tree bark or straw + bone_meal + rotten flesh + 250mB water → 4
  （机械搅拌配方：bone_meal + rotten flesh + 100mB 水 → 2 个；装了 Farmer's Delight 时另有配方：tree bark 或 straw + bone_meal + rotten flesh + 250mB 水 → 4 个）

**Cultivation alarm system / 栽培警示系统**
- 🟠 Orange lamp — tank too short for the crop: no growth, no fertilizer consumed, watering refused; GUI border + Jade hint
  （橙灯：罐体高度不满足作物需求——不生长、不消耗肥料、拒绝浇水，GUI 橙色边框 + Jade 明确提示）
- 🔴 Red lamp — output about to overflow: pauses before wasting any harvest, breathing glow synced to the in-world lamp
  （红灯：输出槽即将溢出——预测下次收获放不下时提前暂停，呼吸灯同步到世界内发光）
- Display Link readouts: remaining time, stalled / height-mismatch warnings, live multipliers, fertilizer stock
  （Display Link 新增：剩余时间、停滞/高度不足警告、倍率读数、肥料余量）

## Fixed / 修复

- Renderer/server crashes from stage-crop progress overflow at extreme speeds
  （高倍率环境下阶段作物进度溢出导致的渲染/服务端崩溃）
- Fertilizer silently consumed on the last tick; alarm animation never stopping after the alarm cleared
  （肥料单件被无声消耗、警示解除后动画永久播放）
- Crops permanently stuck after a datapack reload removed their recipe (now auto-cleared and logged)
  （数据包重载后配方消失导致作物永久卡死——现自动清除并记录日志）
- Jade reporting "mature" on tanks that were too short
  （Jade 在矮罐上误报"已成熟"）
- Crop models rendering fully mature while the height alarm was active
  （矮罐中作物模型误渲染为成熟形态）

## Requirements / 环境要求

- Minecraft 1.21.1
- NeoForge 21.1.x
- Create 6.0.x
- Optional / 可选: JEI, Jade, and the compatible mods above / JEI、Jade 及上述兼容模组
