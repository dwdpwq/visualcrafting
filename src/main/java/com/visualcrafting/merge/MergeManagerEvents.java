package com.visualcrafting.merge;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 将 {@link MergeManager} 挂载到模组生命周期：
 * <ul>
 *   <li>服务端启动完成后自动执行一次玩家配方暂存合并；</li>
 *   <li>游戏内执行 {@code /reload}（数据包重载）时也自动合并一次。</li>
 * </ul>
 */
@EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.GAME)
public class MergeManagerEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        new MergeManager().mergeAll();
    }

    /**
     * /reload 命令会触发数据包资源重载，这里注册一个重载监听器，
     * 在重载完成时执行配方合并。
     */
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Object>() {
            @Override
            protected Object prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                return null;
            }

            @Override
            protected void apply(Object prepared, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                new MergeManager().mergeAll();
            }
        });
    }
}
