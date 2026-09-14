package com.visualcrafting.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 熔炉卡的基础物品实现（不含任何 AE2 依赖）。
 * <p>
 * AE2 已安装时，实际注册的物品由 {@code com.visualcrafting.compat.ae2.Ae2FurnaceCardItem} 创建
 * （它继承 AE2 的 UpgradeCardItem，才会被升级槽接受），本类仅作为 AE2 缺失时的实现。
 * <p>
 * <b>本类不得 import / 引用任何 appeng 类型</b>：{@code FurnaceCardRegistrar} 等类在 AE2 缺失时
 * 依然会被加载，一旦此处出现 appeng 符号就会在字节码校验阶段抛 NoClassDefFoundError。
 * <p>
 * 数值与 NBT 逻辑统一放在 {@link FurnaceCardData}，供两个实现共用；静态方法保留为兼容入口。
 */
public class FurnaceCardItem extends Item implements IFurnaceCard {

    private final int tier;

    public FurnaceCardItem(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        FurnaceCardData.appendTooltip(tier, tooltip);
    }

    // ---------------------------------------------------------------
    // 兼容入口：既有调用方（mixin / 网络包等）继续使用这些静态方法
    // ---------------------------------------------------------------

    public static long getStoredExpMilli(ItemStack stack) {
        return FurnaceCardData.getStoredExpMilli(stack);
    }

    public static void setStoredExpMilli(ItemStack stack, long value) {
        FurnaceCardData.setStoredExpMilli(stack, value);
    }

    public static int getMaxExperienceLevels(int tier) {
        return FurnaceCardData.getMaxExperienceLevels(tier);
    }

    public static int getExperiencePointsForLevel(int level) {
        return FurnaceCardData.getExperiencePointsForLevel(level);
    }
}
