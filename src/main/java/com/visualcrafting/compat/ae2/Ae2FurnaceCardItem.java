package com.visualcrafting.compat.ae2;

import appeng.items.materials.UpgradeCardItem;
import com.visualcrafting.item.FurnaceCardData;
import com.visualcrafting.item.IFurnaceCard;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * AE2 存在时使用的熔炉卡实现。
 * <p>
 * 必须继承 {@link UpgradeCardItem}，否则 AE2 的升级槽（Upgrades.isUpgradeCardItem）
 * 不会把它识别为升级卡。
 * <p>
 * 本类位于 compat 包，只有 AE2 已加载时才会被 {@code AE2Compat} 通过反射加载。
 */
public class Ae2FurnaceCardItem extends UpgradeCardItem implements IFurnaceCard {

    private final int tier;

    public Ae2FurnaceCardItem(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    /**
     * 供无 AE2 依赖的 {@code com.visualcrafting.compat.AE2Compat} 反射调用。
     * 返回类型声明为 {@link Item}，避免调用方在编译期绑定本类。
     */
    public static Item create(Item.Properties properties, int tier) {
        return new Ae2FurnaceCardItem(properties, tier);
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
}
