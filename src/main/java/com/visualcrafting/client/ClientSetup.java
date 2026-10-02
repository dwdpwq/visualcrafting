package com.visualcrafting.client;

import com.visualcrafting.VisualCraftingTable;
import com.visualcrafting.screen.VisualCraftingScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only GUI registrations.
 *
 * <p>NeoForge 1.21.1 exposes a typed registration API for menu screens, so
 * there is no need to reflectively resolve MenuScreens.ScreenConstructor or
 * create a runtime Proxy. Keeping this registration compile-time typed makes
 * API/mapping changes fail at build time instead of silently at runtime.</p>
 */
@EventBusSubscriber(modid = VisualCraftingTable.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                VisualCraftingTable.VISUAL_CRAFTING_MENU.get(),
                VisualCraftingScreen::new
        );
    }
}
