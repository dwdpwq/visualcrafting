package com.visualcrafting.jei;

import com.visualcrafting.screen.VisualCraftingScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 集成入口：为化学灌注 GUI 注册化学品拖放（ghost）处理器。
 * 依赖 mezz.jei（build.gradle 中 compileOnly），运行时若无 JEI 也不会影响模组本体。
 */
@JeiPlugin
public class VisualCraftingJEIPlugin implements IModPlugin {

    /** 诊断日志：确认 JEI 是否发现并加载了本插件、GUI 处理器是否注册成功。 */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("VisualCrafting");

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "jei_plugin");

    static {
        LOGGER.info("[VC-JEI] plugin class loaded, loader={}",
                VisualCraftingJEIPlugin.class.getClassLoader());
    }

    public VisualCraftingJEIPlugin() {
        LOGGER.info("[VC-JEI] plugin instantiated");
    }

    @Override
    public ResourceLocation getPluginUid() {
        LOGGER.info("[VC-JEI] getPluginUid called");
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        LOGGER.info("[VC-JEI] registerGuiHandlers called");
        registration.addGhostIngredientHandler(VisualCraftingScreen.class, new VisualCraftingGhostHandler());
        LOGGER.info("[VC-JEI] ghost handler registered for {}",
                VisualCraftingScreen.class.getName());
    }
}
