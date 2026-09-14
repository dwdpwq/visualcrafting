package com.visualcrafting.item;

/**
 * 熔炉卡的公共契约。
 * <p>
 * AE2 为可选前置：AE2 存在时实际物品是 {@code com.visualcrafting.compat.ae2.Ae2FurnaceCardItem}
 * （必须继承 AE2 的 UpgradeCardItem 才会被升级槽接受），AE2 缺失时使用
 * {@link FurnaceCardItem}（仅继承原版 Item）。
 * <p>
 * 因此所有"是否是熔炉卡""等级是多少"的判断都必须基于本接口，而不是具体实现类。
 * 本接口及其 default 实现不得引用任何 appeng 类型。
 */
public interface IFurnaceCard {

    int getTier();

    default int getSmeltSpeed() {
        return FurnaceCardData.getSmeltSpeed(getTier());
    }

    default long getMaxExpStorage() {
        return FurnaceCardData.getMaxExpStorage(getTier());
    }

    default double getEnergyCost() {
        return FurnaceCardData.getEnergyCost(getTier());
    }

    default double getProcessRatio() {
        return FurnaceCardData.getProcessRatio(getTier());
    }

    default float getBatchPercent() {
        return FurnaceCardData.getBatchPercent(getTier());
    }

    default int getMaxExperiencePoints() {
        return FurnaceCardData.getMaxExperiencePoints(getTier());
    }
}
