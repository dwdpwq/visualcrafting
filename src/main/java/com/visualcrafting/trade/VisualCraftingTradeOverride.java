package com.visualcrafting.trade;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.util.RandomSource;

import java.util.Optional;

/**
 * Keeps an original ItemListing intact and transforms the generated MerchantOffer.
 * This is important for modded trades whose ItemListing creates random/NBT/component-rich
 * results dynamically.
 */
public final class VisualCraftingTradeOverride implements VillagerTrades.ItemListing {
    private final VillagerTrades.ItemListing original;
    private final VisualCraftingTradeHandler.OverrideDefinition definition;

    public VisualCraftingTradeOverride(VillagerTrades.ItemListing original,
                                        VisualCraftingTradeHandler.OverrideDefinition definition) {
        this.original = original;
        this.definition = definition;
    }

    @Override
    public MerchantOffer getOffer(Entity trader, RandomSource random) {
        MerchantOffer offer = original.getOffer(trader, random);
        if (offer == null) return null;

        ItemStack costA = definition.cost1().isEmpty()
                ? offer.getBaseCostA()
                : definition.cost1().copy();
        ItemStack costB = definition.cost2().isEmpty()
                ? offer.getCostB()
                : definition.cost2().copy();
        ItemStack result = definition.result().isEmpty()
                ? offer.getResult().copy()
                : definition.result().copy();

        if (costA.isEmpty() || result.isEmpty()) return offer;

        ItemCost itemCostA = new ItemCost(costA.getItem(), Math.max(1, costA.getCount()));
        Optional<ItemCost> itemCostB = costB.isEmpty()
                ? Optional.empty()
                : Optional.of(new ItemCost(costB.getItem(), Math.max(1, costB.getCount())));

        int maxUses = definition.maxUses() > 0 ? definition.maxUses() : offer.getMaxUses();
        int xp = definition.xp() >= 0 ? definition.xp() : offer.getXp();
        float multiplier = definition.priceMultiplier() >= 0.0F
                ? definition.priceMultiplier()
                : offer.getPriceMultiplier();

        return new MerchantOffer(
                itemCostA,
                itemCostB,
                result,
                offer.getUses(),
                maxUses,
                xp,
                multiplier,
                offer.getDemand()
        );
    }
}
