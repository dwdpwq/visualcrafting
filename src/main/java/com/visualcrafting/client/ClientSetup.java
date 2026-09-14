package com.visualcrafting.client;

import com.visualcrafting.VisualCraftingTable;
import com.visualcrafting.screen.VisualCraftingMenu;
import com.visualcrafting.screen.VisualCraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@EventBusSubscriber(modid = "visualcrafting", value = {Dist.CLIENT})
public class ClientSetup {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        try {
            Class<?> screenConstructorClass = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            MenuType<VisualCraftingMenu> menuType = VisualCraftingTable.VISUAL_CRAFTING_MENU.get();
            Method registerMethod = RegisterMenuScreensEvent.class.getMethod("register", MenuType.class, screenConstructorClass);

            Object proxy = Proxy.newProxyInstance(screenConstructorClass.getClassLoader(),
                    new Class[]{screenConstructorClass},
                    (proxyObj, method, args) -> {
                        if ("create".equals(method.getName())) {
                            return new VisualCraftingScreen(
                                    (VisualCraftingMenu) args[0],
                                    (Inventory) args[1],
                                    (Component) args[2]);
                        }
                        if (method.isDefault()) {
                            return MethodHandles.lookup()
                                    .unreflectSpecial(method, method.getDeclaringClass())
                                    .bindTo(proxyObj)
                                    .invokeWithArguments(args);
                        }
                        return null;
                    });

            registerMethod.invoke(event, menuType, proxy);
        } catch (Exception e) {
            throw new RuntimeException("[VC] Failed to register screen", e);
        }
    }
}
