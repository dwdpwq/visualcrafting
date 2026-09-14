package com.visualcrafting.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * 熔炉卡的纯数据/数值工具集。
 * <p>
 * 该类被 {@link FurnaceCardItem}（无 AE2）与 compat.ae2 下的实现共用，
 * <b>不得引用任何 appeng 类型</b>。
 */
public final class FurnaceCardData {

    /** 熔炉卡存储经验所用的 NBT 键（单位：milli-XP）。 */
    public static final String NBT_STORED_EXP = "vc_stored_exp";

    private static final long[] MAX_EXP_STORAGE = {0L, 255532000L, 1098668000L, Long.MAX_VALUE};
    private static final double[] ENERGY_COST = {0.0, 45000.0, 85000.0, 165000.0};
    private static final double[] PROCESS_RATIO = {0.0, 0.3, 0.6, 1.0};

    private static final int MIN_TIER = 0;
    private static final int MAX_TIER = 3;

    private FurnaceCardData() {
    }

    private static int clampTier(int tier) {
        if (tier < MIN_TIER) return MIN_TIER;
        return Math.min(tier, MAX_TIER);
    }

    public static int getSmeltSpeed(int tier) {
        return switch (clampTier(tier)) {
            case 1 -> 15;
            case 2 -> 10;
            case 3 -> 2;
            default -> 20;
        };
    }

    public static long getMaxExpStorage(int tier) {
        return MAX_EXP_STORAGE[clampTier(tier)];
    }

    public static double getEnergyCost(int tier) {
        return ENERGY_COST[clampTier(tier)];
    }

    public static double getProcessRatio(int tier) {
        return PROCESS_RATIO[clampTier(tier)];
    }

    public static float getBatchPercent(int tier) {
        return switch (clampTier(tier)) {
            case 1 -> 0.3f;
            case 2 -> 0.6f;
            case 3 -> 1.0f;
            default -> 0.3f;
        };
    }

    public static int getMaxExperienceLevels(int tier) {
        return switch (clampTier(tier)) {
            case 1 -> 256;
            case 2 -> 512;
            case 3 -> 1024;
            default -> 256;
        };
    }

    public static int getExperiencePointsForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360.0);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220.0);
    }

    public static int getMaxExperiencePoints(int tier) {
        return getExperiencePointsForLevel(getMaxExperienceLevels(tier));
    }

    public static long getStoredExpMilli(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getLong(NBT_STORED_EXP);
    }

    public static void setStoredExpMilli(ItemStack stack, long value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (value <= 0L) {
            tag.remove(NBT_STORED_EXP);
        } else {
            tag.putLong(NBT_STORED_EXP, value);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** 物品悬浮提示，两个实现类共用，避免 AE2 缺失/存在两条路径显示不一致。 */
    public static void appendTooltip(int tier, List<Component> tooltip) {
        tooltip.add(Component.translatable("item.visualcrafting.furnace_card.tooltip.speed", getSmeltSpeed(tier)));
        tooltip.add(Component.translatable("item.visualcrafting.furnace_card.tooltip.tier", tier));
    }
}
