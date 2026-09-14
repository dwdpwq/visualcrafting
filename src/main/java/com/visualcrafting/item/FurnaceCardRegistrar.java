package com.visualcrafting.item;

import com.visualcrafting.Config;
import com.visualcrafting.VisualCraftingTable;
import com.visualcrafting.compat.AE2Compat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 熔炉卡的注册与分类页登记。
 * <p>
 * 本类在 AE2 缺失时同样会被加载，因此 <b>不得 import 任何 appeng 类型</b>：
 * AE2 相关注册全部经 {@link AE2Compat} 反射转发到 {@code compat.ae2} 包。
 */
public class FurnaceCardRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger("VisualCrafting");

    public static DeferredItem<Item> FURNACE_CARD_TIER1;
    public static DeferredItem<Item> FURNACE_CARD_TIER2;
    public static DeferredItem<Item> FURNACE_CARD_TIER3;

    private FurnaceCardRegistrar() {
    }

    public static Item tier1() {
        return FURNACE_CARD_TIER1 == null ? null : FURNACE_CARD_TIER1.get();
    }

    public static Item tier2() {
        return FURNACE_CARD_TIER2 == null ? null : FURNACE_CARD_TIER2.get();
    }

    public static Item tier3() {
        return FURNACE_CARD_TIER3 == null ? null : FURNACE_CARD_TIER3.get();
    }

    @EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.GAME)
    public static class CreativeTabHandler {
        private static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("functional_blocks"));

        @SubscribeEvent
        static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (!Config.isFurnaceCardEnabled()) return;
            if (event.getTabKey().equals(FUNCTIONAL_BLOCKS) && FURNACE_CARD_TIER1 != null) {
                event.accept(FURNACE_CARD_TIER1.get());
                event.accept(FURNACE_CARD_TIER2.get());
                event.accept(FURNACE_CARD_TIER3.get());
            }
        }
    }

    @EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.MOD)
    public static class RegistrationHandler {
        @SubscribeEvent
        static void onCommonSetup(FMLCommonSetupEvent event) {
            if (!Config.isFurnaceCardEnabled()) return;
            // AE2 为可选前置：未安装时不做任何 AE2 相关注册
            if (!AE2Compat.isAe2Loaded()) return;
            if (FURNACE_CARD_TIER1 == null || FURNACE_CARD_TIER2 == null || FURNACE_CARD_TIER3 == null) return;
            event.enqueueWork(AE2Compat::registerAe2Integration);
        }

        static {
            // AE2 存在时才注册熔炉卡物品：物品实现类需要继承 AE2 的 UpgradeCardItem，
            // 缺失 AE2 时注册会在类加载阶段抛 NoClassDefFoundError。
            if (AE2Compat.isAe2Loaded()) {
                FURNACE_CARD_TIER1 = VisualCraftingTable.ITEMS.register("furnace_card_tier1",
                        () -> AE2Compat.createFurnaceCard(new Item.Properties().stacksTo(1), 1));
                FURNACE_CARD_TIER2 = VisualCraftingTable.ITEMS.register("furnace_card_tier2",
                        () -> AE2Compat.createFurnaceCard(new Item.Properties().stacksTo(1), 2));
                FURNACE_CARD_TIER3 = VisualCraftingTable.ITEMS.register("furnace_card_tier3",
                        () -> AE2Compat.createFurnaceCard(new Item.Properties().stacksTo(1), 3));
                LOGGER.info("[VC] AE2 detected, furnace card items queued for registration");
            } else {
                LOGGER.info("[VC] AE2 not installed, furnace card items skipped (optional dependency)");
            }
        }
    }
}
