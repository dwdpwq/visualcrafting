# VisualCrafting 交易与职业方块编辑

本功能面向 NeoForge 1.21.1。

## 1. 自定义职业交易

GUI 继续使用：

world/visualcrafting/trades/<profession>/<index>.json

例如：

world/visualcrafting/trades/farmer/0.json

字段：

- level
- cost1 / cost1Count
- cost2 / cost2Count
- result / resultCount
- maxUses
- xp
- priceMultiplier
- clearExisting

clearExisting=true 只清空当前 level 的原有交易。

## 2. 修改其他 Mod 的村民交易

服务器加载交易表时，VisualCrafting 会保留原始 ItemListing，随后包装生成的 MerchantOffer。

因此其他 Mod 的随机交易、带组件/NBT 的产物以及根据实体动态生成的交易不会被强制转换成固定商品；只有配置指定的成本、产物或交易参数会被替换。

目录：

world/visualcrafting/trade_overrides/villager/<profession>/<level>-<index>.json

示例：

world/visualcrafting/trade_overrides/villager/engineer/3-1.json

内容：

{
  "cost1": "minecraft:emerald",
  "cost1Count": 5,
  "cost2": "minecraft:diamond",
  "cost2Count": 1,
  "result": "minecraft:iron_block",
  "resultCount": 2,
  "maxUses": 12,
  "xp": 10,
  "priceMultiplier": 0.05
}

所有字段均可省略；省略即保留原交易对应值。

注意：index 是该职业该等级交易列表在重新加载时的当前顺序。某些 Mod 如果动态改变交易顺序，建议重新确认索引。

## 3. 修改流浪商人交易

普通交易：

world/visualcrafting/trade_overrides/wandering/generic-0.json

稀有交易：

world/visualcrafting/trade_overrides/wandering/rare-0.json

格式与村民交易覆盖文件相同。

NeoForge 1.21.1 的流浪商人交易事件明确分为 generic 与 rare 两个列表，因此这里不会把两类交易混在一起。

## 4. 修改职业方块

目录：

world/visualcrafting/job_sites/<profession>.json

示例：

world/visualcrafting/job_sites/librarian.json

{
  "block": "minecraft:lectern"
}

目标方块必须已经拥有 Minecraft/其他 Mod 注册的 POI。

VisualCrafting 不会在游戏运行过程中伪造新的 POI 注册，因为村民的寻路、占用、竞争和工作行为都依赖真实 POI 注册。

因此：
- 可以把一个职业改成另一个已有 POI 的职业方块；
- 可以使用其他 Mod 已注册 POI 的方块；
- 一个普通装饰方块如果没有 POI，不能仅靠这个配置变成职业方块。

实现方式不会替换 VillagerProfession 注册表对象，而是覆盖 heldJobSite() 和 acquirableJobSite() 的谓词，从而尽量保持其他 Mod 对职业 Holder/注册表身份不变。

## 5. 生效方式

交易列表在交易事件重新构建时应用，修改配置后执行：

/reload

已有村民可能已经持有旧交易，测试新交易时建议使用新生成的村民。

职业方块修改主要影响之后的职业获取/重新检查流程。已经拥有职业且已经绑定旧工作站的村民，建议先让其失业或重新放置工作站后测试。

## 6. 设计限制

VisualCrafting 的目标是“修改原交易”，而不是把其他 Mod 的任意 Java ItemListing 强制反序列化成静态 JSON。

这是有意的：很多 Mod 的交易是在 getOffer 中根据随机数、村民实体、世界位置、标签或物品组件动态生成的。直接把它们转换成固定商品会丢失原 Mod 的行为。

因此当前索引覆盖方案保留原交易生成器，只修改最终 MerchantOffer。
