Visual Crafting Table 模组说明文档
版本 1.6.0 | Minecraft 1.21.1 | NeoForge

概述
Visual Crafting Table 是一个 可视化配方管理工具，让整合包作者无需手写代码即可管理 KubeJS / CraftTweaker 配方。放置一个可视化工作台，放入物品点几下按钮，配方脚本自动生成到对应目录。

核心设计理念：所见即所得——在工作台 GUI 中放入物品和产物，点击对应按钮，即可自动生成配方脚本、封禁脚本、矿物生成数据包或食物配方。

主要功能
一、Tab 1：合成配方（Crafting）—— mode 0
将物品放入 3×3 工作格和输出格，单击 Shaped（有序合成）或 Shapeless（无序合成）按钮，自动生成 KubeJS 合成配方脚本。

功能	说明
工作格尺寸	支持 4 档等级：3×3（Tier 0）、5×5（Tier 1）、7×7（Tier 2）、9×9（Tier 3）
有序/无序合成	Shaped 按钮生成有序配方，Shapeless 按钮生成无序配方
删除配方	放入物品点击 "Delete Recipe"，自动生成 event.remove() 封禁条目
按输出删除	将产物放入输出格，删除所有以该物品为产物的配方（delete_by_output）
右侧配方列表	实时显示当前工作台保存的全部合成配方，支持滚动和右键删除
操作历史	工作台内部记录最近 100 条添加/删除操作历史
生成文件（以 KubeJS 格式为例）：

kubejs/server_scripts/visualcrafting_outputs.txt —— 合成的配方脚本
kubejs/server_scripts/visualcrafting_banned.txt —— 封禁的配方条目
二、Tab 2：灌注配方（Infusing）—— mode 1
与合成配方类似，但使用单格输入 + 单格输出模型，适合制作灌注/转化类自定义配方。

功能	说明
输入方式	1 个输入物品 → 1 个输出物品
保存/删除	与 Crafting 模式相同的保存和封禁操作
按输出删除	支持 delete_infusing_by_output
生成文件：

kubejs/server_scripts/visualcrafting_infusing_outputs.txt
kubejs/server_scripts/visualcrafting_infusing_banned.txt
三、Tab 3：矿石生成（Ore Gen）—— mode 2
可视化创建矿物生成数据包（Datapack）。

功能	说明
维度选择	下拉列表选择目标维度，支持全部群系
群系选择	按维度筛选可选群系
矿物生成	生成包含 ConfiguredFeature + PlacedFeature 的完整数据包
封禁生成	禁用特定矿物的自然生成
矿脉参数	可配置矿脉数量范围、生成高度范围等
四、Tab 4：食物配方（Food）—— mode 5
为食物物品快速添加自定义属性。

属性	说明
饥饿值（Hunger）	0-20，食用后恢复的饱食度
饱和度（Saturation）	饱和度系数
食用时间（Eat Seconds）	食用所需秒数
药水效果	从已注册药水列表中选择，可配等级、时长、是否无限
返还物品	食用后留在物品栏的道具（如碗、瓶）
五、熔炉卡片（Furnace Card）
一套 3 个等级的 AE2 升级卡，插入 ME 接口后自动从网络中熔炼物品。

参数	Tier 1	Tier 2	Tier 3
熔炼速度	15 tick/个	10 tick/个	2 tick/个
AE 能耗	45,000 AE/个	85,000 AE/个	165,000 AE/个
产出倍率	30%	60%	100%
最大经验存储	255,532,000 mXP	1,098,668,000 mXP	无上限
最高经验等级	256 级	512 级	1024 级
工作方式：将 Furnace Card 放入 AE2 Interface 的升级槽，网络中有对应的合成样板（pattern）时，自动从网络中取出原料完成熔炼，产物返回网络，经验存储于卡片 NBT 中。

AE2 GUI 集成（可选依赖）：打开 ME 接口 GUI 时，会自动显示一个"提取经验"按钮，点击将卡片中存储的经验以液体 XP 形式返还到 AE 网络。

六、液体经验（Liquid XP）
属性	说明
流体 ID	visualcrafting:liquid_xp
流体标签	c:experience、forge:experience
经验桶	visualcrafting:liquid_xp_bucket
转换比率	1000 mXP = 20 mB（1 经验点 = 20 mB 流体）
当 Furnace Card 存储的经验达到上限时，溢出部分自动转换为液体经验存入 AE 网络。

七、AE2 集成（可选依赖）
AE2 声明为可选依赖（type="optional"）
MixinPlugin（VisualCraftingMixinPlugin）：仅在 AE2 加载时注入 AEBaseScreenMixin
在 ME 接口 GUI 的左侧工具栏添加"提取经验"按钮
经验流体自动识别 AE 网络中已有液体类型并合并存储
八、Mekanism 集成（可选）
仅在 Mekanism 加载时生效，用于读取 Chemical 物品的颜色信息做 GUI 渲染显示。

配置文件
路径：config/visualcrafting-common.toml

配置项	默认值	说明
default_format	KUBEJS	新放置工作台的默认输出格式（可选 KUBEJS 或 CRT）
furnace_card_enabled	1	是否启用熔炉卡片合成和 AE2 集成（0 = 禁用）
依赖关系
模组	类型	版本范围
NeoForge	必需	21.1+
Minecraft	必需	1.21.1 - 1.22
KubeJS	必需	2101+
AE2	可选	19.2+
Mekanism	可选	—
