package com.visualcrafting.network;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.misc.InterfaceBlockEntity;
import com.visualcrafting.item.FurnaceCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.MOD)
public class FurnaceExpRegistry {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                ExtractFurnaceExpPacket.TYPE,
                ExtractFurnaceExpPacket.STREAM_CODEC,
                FurnaceExpRegistry::handleExtractFurnaceExp);
    }

    private static void handleExtractFurnaceExp(ExtractFurnaceExpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                ServerPlayer player = (ServerPlayer) context.player();
                BlockPos pos = packet.pos();
                // Security: reject out-of-range positions to prevent arbitrary block access.
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0 * 64.0) {
                    return;
                }
                BlockEntity blockEntity = player.level().getBlockEntity(pos);
                if (!(blockEntity instanceof InterfaceBlockEntity iface)) {
                    return;
                }
                if (iface.getMainNode() == null || iface.getMainNode().getGrid() == null) {
                    return;
                }
                IUpgradeInventory upgrades = iface.getUpgrades();

                long totalExpMilli = 0L;
                int bestTier = 0;
                for (int i = 0; i < upgrades.size(); i++) {
                    ItemStack stack = upgrades.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    Item item = stack.getItem();
                    if (!(item instanceof FurnaceCardItem card)) continue;
                    long stored = FurnaceCardItem.getStoredExpMilli(stack);
                    if (stored <= 0L) continue;
                    totalExpMilli += stored;
                    if (card.getTier() > bestTier) {
                        bestTier = card.getTier();
                    }
                    FurnaceCardItem.setStoredExpMilli(stack, 0L);
                    upgrades.setItemDirect(i, stack.copy());
                    upgrades.sendChangeNotification(i);
                }

                if (totalExpMilli <= 0L) {
                    return;
                }

                int effectiveTier = bestTier > 0 ? bestTier : 1;
                int maxLevel = FurnaceCardItem.getMaxExperienceLevels(effectiveTier);
                int maxPoints = FurnaceCardItem.getExperiencePointsForLevel(maxLevel);

                long pointsLong = totalExpMilli / 1000L;
                int points = (int)Math.min(pointsLong, (long)maxPoints);

                if (points > 0) {
                    player.giveExperiencePoints(points);
                    String extra = (pointsLong > maxPoints)
                            ? " (capped at tier " + effectiveTier + " limit " + maxLevel + " levels)"
                            : "";
                    player.displayClientMessage(
                            Component.literal("Gave " + points + " experience points to player" + extra), false);
                }
            } catch (Exception e) {
                System.err.println("[VC] Failed to extract furnace exp: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
