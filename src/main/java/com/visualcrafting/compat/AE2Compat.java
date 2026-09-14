package com.visualcrafting.compat;

import com.visualcrafting.item.FurnaceCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AE2 可选前置的统一反射门面。
 * <p>
 * <b>本类不得 import / 引用任何 appeng 类型</b>：它会被 {@link com.visualcrafting.item.FurnaceCardRegistrar}
 * 与 {@link com.visualcrafting.network.FurnaceExpRegistry} 等"AE2 缺失时仍会被加载"的类引用，
 * 一旦这里出现 appeng 符号，未安装 AE2 时就会在类加载/字节码校验阶段抛 NoClassDefFoundError。
 * <p>
 * 真正的 AE2 逻辑全部位于 {@code com.visualcrafting.compat.ae2} 包，只有确认 AE2 已加载时
 * 才通过 {@link Class#forName(String)} 加载。
 */
public final class AE2Compat {

    public static final String AE2_MOD_ID = "ae2";

    private static final String AE2_PKG = "com.visualcrafting.compat.ae2.";

    private static final Logger LOGGER = LoggerFactory.getLogger(AE2Compat.class);

    private AE2Compat() {
    }

    /**
     * AE2 是否在本实例中成功加载。
     * <p>
     * 本方法可能在 FML 早期（@EventBusSubscriber 注解扫描阶段，静态初始化块内）被调用，
     * 此时 {@link net.neoforged.fml.ModList} 可能尚未构建完成，因此先试 ModList、
     * 再退回加载期的 {@link net.neoforged.fml.loading.LoadingModList}。
     * 任何异常都按"未加载"处理，保证不会因探测本身导致崩溃。
     */
    public static boolean isAe2Loaded() {
        try {
            var modList = net.neoforged.fml.ModList.get();
            if (modList != null) {
                return modList.isLoaded(AE2_MOD_ID);
            }
        } catch (Throwable ignored) {
            // 早期阶段 ModList 尚不可用，退回 LoadingModList
        }
        try {
            var loadingModList = net.neoforged.fml.loading.LoadingModList.get();
            return loadingModList != null && loadingModList.getModFileById(AE2_MOD_ID) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 创建熔炉卡物品：AE2 存在时创建继承 AE2 UpgradeCardItem 的实现（否则升级槽不认），
     * 否则退回普通物品实现。
     */
    public static Item createFurnaceCard(Item.Properties properties, int tier) {
        if (isAe2Loaded()) {
            try {
                Class<?> cls = Class.forName(AE2_PKG + "Ae2FurnaceCardItem");
                return (Item) cls.getMethod("create", Item.Properties.class, int.class)
                        .invoke(null, properties, tier);
            } catch (Throwable t) {
                LOGGER.error("[VC] Failed to build AE2 furnace card (tier {}); falling back to plain item", tier, t);
            }
        }
        return new FurnaceCardItem(properties, tier);
    }

    /** 注册 AE2 集成（升级卡关联 + 服务端 tick 驱动），仅在 AE2 已加载时调用。 */
    public static void registerAe2Integration() {
        if (invokeNoArg("Ae2Registration", "register")) {
            LOGGER.info("[VC] AE2 integration registered: furnace cards bound to ME Interface upgrades");
        }
    }

    /**
     * 处理"从 ME 接口取出熔炉卡经验"的网络包。
     *
     * @return 是否成功完成一次处理（AE2 缺失或异常时返回 false）
     */
    public static boolean handleExtractExp(ServerPlayer player, BlockPos pos) {
        if (!isAe2Loaded()) {
            return false;
        }
        try {
            Class<?> cls = Class.forName(AE2_PKG + "Ae2ExpExtraction");
            Object result = cls.getMethod("handle", ServerPlayer.class, BlockPos.class)
                    .invoke(null, player, pos);
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {
            LOGGER.warn("[VC] AE2 exp extraction failed", t);
            return false;
        }
    }

    private static boolean invokeNoArg(String simpleName, String methodName) {
        try {
            Class.forName(AE2_PKG + simpleName).getMethod(methodName).invoke(null);
            return true;
        } catch (Throwable t) {
            LOGGER.error("[VC] AE2 integration {}.{}() failed", simpleName, methodName, t);
            return false;
        }
    }
}
