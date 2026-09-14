package com.visualcrafting.jei;

import com.visualcrafting.screen.ChemSlotData;
import com.visualcrafting.screen.MekanismIntegration;
import com.visualcrafting.screen.VisualCraftingMenu;
import com.visualcrafting.screen.VisualCraftingScreen;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * JEI ghost 拖放处理器：
 * 在化学灌注模式（mode == 1）下，允许玩家把 JEI 面板中的化学品直接拖放到
 * 化学槽区域（getChemSlotX/getChemSlotY 的 16x16 范围），从而完成化学品的
 * 标记选中，替代手工放置化学物品的操作。
 */
public class VisualCraftingGhostHandler implements IGhostIngredientHandler<VisualCraftingScreen> {

    /** 诊断日志：定位"化学品拖放无效"时用来判断目标是否注册、accept 是否被调用。 */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("VisualCrafting");

    @Override
    public <I> List<Target<I>> getTargetsTyped(VisualCraftingScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (doStart) {
            Object raw = ingredient == null ? null : ingredient.getIngredient();
            LOGGER.info("[VC-Ghost] targets requested: mode={}, ingredientClass={}",
                    screen.getMode(), raw == null ? "null" : raw.getClass().getName());
        }
        // 仅在化学灌注页提供化学槽作为拖放目标
        if (screen.getMode() == 1) {
            targets.add(new Target<I>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(screen.getChemSlotX(), screen.getChemSlotY(), 18, 18);
                }

                @Override
                public void accept(I ingredient) {
                    boolean chemical = MekanismIntegration.isChemicalIngredient(ingredient);
                    ChemSlotData slotData = chemical ? MekanismIntegration.buildChemSlotData(ingredient) : null;
                    LOGGER.info("[VC-Ghost] accept: ingredientClass={}, isChemical={}, slotData={}",
                            ingredient == null ? "null" : ingredient.getClass().getName(),
                            chemical, slotData == null ? "null" : "ok");
                    if (!chemical) {
                        return;
                    }
                    VisualCraftingMenu menu = (VisualCraftingMenu) screen.getMenu();
                    if (slotData != null) {
                        menu.chemSlotData = slotData;
                    }
                    CompoundTag tag = MekanismIntegration.convertChemicalToTag(ingredient);
                    LOGGER.info("[VC-Ghost] tag={}", tag);
                    ItemStack ghostItem = MekanismIntegration.createChemicalTagItem(tag);

                    // 化学拖放仅标记化学槽：不得写入合成输入槽（ghostItems[0]）
                    screen.setSelectedChemical(ghostItem);
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
        // 拖放结束无需额外处理
    }
}
