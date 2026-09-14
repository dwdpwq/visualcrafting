package com.visualcrafting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 命名牌「彩虹」名称/描述的流动动画（纯客户端实现）。
 *
 * <p>KubeJS 脚本写入的是静态彩虹（逐字符按色相均分固定染色），只在物品生成/修改时执行一次，
 * 本身无法逐帧变色。本类在客户端每 {@link #UPDATE_INTERVAL} tick 对本地物品副本的名称/描述
 * 按相位重新着色，使颜色在字面上循环流动。</p>
 *
 * <p>只改动客户端本地副本，不会产生客户端—服务器数据不一致：服务器同步数据到达时以服务器为准，
 * 下一 tick 会重新着色。其他玩家、掉落物、物品展示框看到的是脚本写入的静态彩虹。</p>
 */
@EventBusSubscriber(modid = "visualcrafting", value = Dist.CLIENT)
public final class RainbowNameAnimator {

    /** 每多少 tick 推进一步相位；越小越顺滑，开销略增。 */
    private static final int UPDATE_INTERVAL = 2;
    /** 每步推进的色相角度：9° → 约 80 tick（4 秒）走完一圈。 */
    private static final float DEGREES_PER_STEP = 9.0F;
    /** 判定「是否为彩虹结构」时色相均分的容差（度）。 */
    private static final float HUE_TOLERANCE = 6.0F;

    private static int tickCounter;
    private static float phase;

    private RainbowNameAnimator() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (++tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        phase = (phase + DEGREES_PER_STEP) % 360.0F;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        recolorInventory(minecraft.player.getInventory());
        // 已打开的容器界面（含本模组界面、箱子等）中的物品副本
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        if (menu != null) {
            int size = menu.slots.size();
            for (int i = 0; i < size; ++i) {
                recolor(menu.slots.get(i).getItem());
            }
        }
    }

    private static void recolorInventory(Inventory inventory) {
        for (ItemStack stack : inventory.items) {
            recolor(stack);
        }
        for (ItemStack stack : inventory.armor) {
            recolor(stack);
        }
        for (ItemStack stack : inventory.offhand) {
            recolor(stack);
        }
    }

    /** 对单个物品副本的名称与描述做流动彩虹着色；非彩虹结构保持不变。 */
    private static void recolor(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            MutableComponent flowing = flow(name);
            if (flowing != null) {
                stack.set(DataComponents.CUSTOM_NAME, flowing);
            }
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            boolean changed = false;
            List<Component> lines = new ArrayList<>(lore.lines().size());
            for (Component line : lore.lines()) {
                MutableComponent flowing = flow(line);
                if (flowing == null) {
                    lines.add(line);
                } else {
                    changed = true;
                    lines.add(flowing);
                }
            }
            if (changed) {
                stack.set(DataComponents.LORE, new ItemLore(lines));
            }
        }
    }

    /**
     * 若组件是脚本生成的彩虹结构（≥2 个子组件、且各子组件色相沿 360° 均分），
     * 则返回按当前相位重新着色的新组件，否则返回 null。
     */
    private static MutableComponent flow(Component component) {
        List<Component> children = component.getSiblings();
        int count = children.size();
        if (count < 2 || !isEvenlyHuedRainbow(children)) {
            return null;
        }
        MutableComponent result = Component.empty().withStyle(component.getStyle());
        for (int i = 0; i < count; ++i) {
            Component child = children.get(i);
            float hue = 360.0F * (float) i / (float) count + phase;
            Style style = child.getStyle().withColor(TextColor.fromRgb(hslToRgb(hue)));
            result.append(Component.literal(child.getString()).withStyle(style));
        }
        return result;
    }

    /** 子组件是否逐个带色、且色相按 360°/count 均匀递增。 */
    private static boolean isEvenlyHuedRainbow(List<Component> children) {
        int count = children.size();
        TextColor first = children.get(0).getStyle().getColor();
        if (first == null) {
            return false;
        }
        float baseHue = hueOf(first.getValue());
        float step = 360.0F / (float) count;
        for (int i = 0; i < count; ++i) {
            TextColor color = children.get(i).getStyle().getColor();
            if (color == null) {
                return false;
            }
            float expected = (baseHue + step * (float) i) % 360.0F;
            float delta = Math.abs(expected - hueOf(color.getValue()));
            if (Math.min(delta, 360.0F - delta) > HUE_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /** RGB → 色相（0~360）。 */
    private static float hueOf(int rgb) {
        float r = (float) ((rgb >> 16) & 0xFF) / 255.0F;
        float g = (float) ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (float) (rgb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        if (delta < 1.0E-4F) {
            return 0.0F;
        }
        float hue;
        if (max == r) {
            hue = ((g - b) / delta) % 6.0F;
        } else if (max == g) {
            hue = (b - r) / delta + 2.0F;
        } else {
            hue = (r - g) / delta + 4.0F;
        }
        hue *= 60.0F;
        return hue < 0.0F ? hue + 360.0F : hue;
    }

    /** 与脚本侧 rainbowColor 一致的取色方式：饱和 1.0、亮度 0.5。 */
    private static int hslToRgb(float hue) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 360.0F;
        float q = 1.0F;
        float p = 0.0F;
        int r = Math.round(hueToChannel(p, q, h + 1.0F / 3.0F) * 255.0F);
        int g = Math.round(hueToChannel(p, q, h) * 255.0F);
        int b = Math.round(hueToChannel(p, q, h - 1.0F / 3.0F) * 255.0F);
        return (r << 16) | (g << 8) | b;
    }

    private static float hueToChannel(float p, float q, float t) {
        float value = t;
        if (value < 0.0F) {
            value += 1.0F;
        }
        if (value > 1.0F) {
            value -= 1.0F;
        }
        if (value < 1.0F / 6.0F) {
            return p + (q - p) * 6.0F * value;
        }
        if (value < 1.0F / 2.0F) {
            return q;
        }
        if (value < 2.0F / 3.0F) {
            return p + (q - p) * (2.0F / 3.0F - value) * 6.0F;
        }
        return p;
    }
}
