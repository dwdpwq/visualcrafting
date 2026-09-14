package com.visualcrafting.compat.ae2;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import com.visualcrafting.item.FurnaceCardRegistrar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

/**
 * AE2 集成注册：把三档熔炉卡登记为 ME 接口的升级卡，并挂上服务端 tick 驱动。
 * <p>
 * 只在 AE2 已加载时由 {@code AE2Compat.registerAe2Integration()} 反射调用。
 */
public final class Ae2Registration {

    private static final String INTERFACE_TOOLTIP = "block.ae2.interface";
    private static final String MEGA_INTERFACE_TOOLTIP = "block.megacells.mega_interface";

    private Ae2Registration() {
    }

    public static void register() {
        Item tier1 = FurnaceCardRegistrar.tier1();
        Item tier2 = FurnaceCardRegistrar.tier2();
        Item tier3 = FurnaceCardRegistrar.tier3();
        if (tier1 == null || tier2 == null || tier3 == null) {
            return;
        }

        Upgrades.add(tier1, AEBlocks.INTERFACE, 1, INTERFACE_TOOLTIP);
        Upgrades.add(tier1, AEParts.INTERFACE, 1, INTERFACE_TOOLTIP);
        Upgrades.add(tier2, AEBlocks.INTERFACE, 1, INTERFACE_TOOLTIP);
        Upgrades.add(tier2, AEParts.INTERFACE, 1, INTERFACE_TOOLTIP);
        Upgrades.add(tier3, AEBlocks.INTERFACE, 1, INTERFACE_TOOLTIP);
        Upgrades.add(tier3, AEParts.INTERFACE, 1, INTERFACE_TOOLTIP);

        if (ModList.get().isLoaded("megacells")) {
            registerMegaInterface(tier1, "mega_interface");
            registerMegaInterface(tier2, "mega_interface");
            registerMegaInterface(tier3, "mega_interface");
            registerMegaInterface(tier1, "cable_mega_interface");
            registerMegaInterface(tier2, "cable_mega_interface");
            registerMegaInterface(tier3, "cable_mega_interface");
        }

        // 服务端 tick 驱动：统一由本处理器扫描 ME 接口上的熔炉卡
        NeoForge.EVENT_BUS.register(Ae2FurnaceTickHandler.class);
    }

    private static void registerMegaInterface(Item card, String path) {
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("megacells", path))
                .ifPresent(item -> Upgrades.add(card, item, 1, MEGA_INTERFACE_TOOLTIP));
    }
}
