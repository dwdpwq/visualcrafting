package com.visualcrafting.compat.ae2;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.misc.InterfaceBlockEntity;
import com.visualcrafting.item.FurnaceCardData;
import com.visualcrafting.item.IFurnaceCard;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 从 ME 接口取出熔炉卡中已累积的经验并结算给玩家（AE2 侧）。
 * <p>
 * 只在 AE2 已加载时由 {@code com.visualcrafting.compat.AE2Compat#handleExtractExp} 反射调用。
 */
public final class Ae2ExpExtraction {

    private static final double MAX_INTERACTION_DISTANCE_SQR = 64.0 * 64.0;

    private Ae2ExpExtraction() {
    }

    public static boolean handle(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return false;
        }
        // Security: reject out-of-range positions to prevent arbitrary block access.
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_INTERACTION_DISTANCE_SQR) {
            return false;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof InterfaceBlockEntity iface)) {
            return false;
        }
        if (iface.getMainNode() == null || iface.getMainNode().getGrid() == null) {
            return false;
        }
        IUpgradeInventory upgrades = iface.getUpgrades();

        long totalExpMilli = 0L;
        int bestTier = 0;
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!(item instanceof IFurnaceCard card)) continue;
            long stored = FurnaceCardData.getStoredExpMilli(stack);
            if (stored <= 0L) continue;
            totalExpMilli += stored;
            if (card.getTier() > bestTier) {
                bestTier = card.getTier();
            }
            FurnaceCardData.setStoredExpMilli(stack, 0L);
            upgrades.setItemDirect(i, stack.copy());
            upgrades.sendChangeNotification(i);
        }

        if (totalExpMilli <= 0L) {
            return false;
        }

        int effectiveTier = bestTier > 0 ? bestTier : 1;
        int maxLevel = FurnaceCardData.getMaxExperienceLevels(effectiveTier);
        int maxPoints = FurnaceCardData.getExperiencePointsForLevel(maxLevel);

        long pointsLong = totalExpMilli / 1000L;
        int points = (int) Math.min(pointsLong, (long) maxPoints);

        if (points > 0) {
            player.giveExperiencePoints(points);
            String extra = (pointsLong > maxPoints)
                    ? " (capped at tier " + effectiveTier + " limit " + maxLevel + " levels)"
                    : "";
            player.displayClientMessage(
                    Component.literal("Gave " + points + " experience points to player" + extra), false);
        }
        return true;
    }
}
