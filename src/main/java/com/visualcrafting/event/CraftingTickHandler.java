package com.visualcrafting.event;

import com.visualcrafting.block.VisualCraftingBlockEntity;
import com.visualcrafting.screen.VisualCraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = "visualcrafting")
public class CraftingTickHandler {

    /** 输出槽索引（与 VisualCraftingMenu.OUTPUT_SLOT 一致）。 */
    private static final int OUTPUT_SLOT_INDEX = VisualCraftingMenu.OUTPUT_SLOT;

    /** 输入未变化时的重试间隔（tick）；输入一变就立即重新匹配配方。 */
    private static final long IDLE_RESCAN_INTERVAL = 20L;

    /** 各工作台最近一次的输入指纹与尝试时间。 */
    private static final Map<BlockPos, CraftCache> CACHE = new HashMap<>();

    private record CraftCache(int fingerprint, long lastAttemptTick) {
    }

    @SubscribeEvent
    public static void handleServerTick(ServerTickEvent.Post event) {
        long tick = event.getServer().getTickCount();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof VisualCraftingMenu vcMenu) {
                craftItem(vcMenu, tick, player);
            }
        }
    }

    /**
     * 自动合成：仅 custom-craft 模式（mode == 0）生效。
     * 输入未变化时按 IDLE_RESCAN_INTERVAL 节流，避免每 tick 全量匹配配方；
     * 配方产生的容器物品（桶、工具等）按原配方返还，不再被吞掉。
     */
    private static void craftItem(VisualCraftingMenu menu, long tick, Player player) {
        VisualCraftingBlockEntity vcbe = menu.blockEntity;
        if (vcbe == null) return;
        if (vcbe.isRemoved()) {
            CACHE.remove(vcbe.getBlockPos());
            return;
        }
        if (vcbe.getMode() != 0) return;   // custom-craft only

        Level level = vcbe.getLevel();
        if (level == null || level.isClientSide) return;

        int gridSize = menu.getGridSize();
        int slotCount = gridSize * gridSize;
        if (slotCount <= 0 || menu.slots.size() <= OUTPUT_SLOT_INDEX) return;

        Slot outSlot = menu.slots.get(OUTPUT_SLOT_INDEX);
        List<ItemStack> inputList = craftSlotsToList(menu, slotCount);

        BlockPos cacheKey = vcbe.getBlockPos();
        int fingerprint = fingerprint(inputList, outSlot.getItem());
        CraftCache cached = CACHE.get(cacheKey);
        if (cached != null && cached.fingerprint() == fingerprint
                && tick - cached.lastAttemptTick() < IDLE_RESCAN_INTERVAL) {
            return;   // 输入与输出都没变，跳过本轮配方匹配
        }
        CACHE.put(cacheKey, new CraftCache(fingerprint, tick));

        CraftingInput input = CraftingInput.of(gridSize, gridSize, inputList);
        RecipeManager recipeManager = level.getRecipeManager();
        Optional<RecipeHolder<CraftingRecipe>> optional =
                recipeManager.getRecipeFor(RecipeType.CRAFTING, input, level);
        if (optional.isEmpty()) return;

        CraftingRecipe recipe = optional.get().value();
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) return;

        ItemStack current = outSlot.getItem();
        if (!current.isEmpty()
                && (!ItemStack.isSameItemSameComponents(current, result)
                    || current.getCount() + result.getCount() > current.getMaxStackSize())) {
            return;   // 输出槽有异类物品或已堆满：等玩家取走
        }

        // 容器物品（桶/工具等）在消耗前按原配方算出剩余物，避免被吞掉
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);

        boolean[] consumed = new boolean[slotCount];
        for (int i = 0; i < slotCount; i++) {
            Slot s = menu.slots.get(i);
            if (!s.hasItem()) continue;
            consumed[i] = true;
            s.getItem().shrink(1);
            if (s.getItem().isEmpty()) {
                s.set(ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < slotCount; i++) {
            if (!consumed[i]) continue;
            ItemStack remain = remaining.get(i);
            if (remain.isEmpty()) continue;
            Slot s = menu.slots.get(i);
            if (s.hasItem()) {
                player.getInventory().placeItemBackInInventory(remain.copy());
            } else {
                s.setByPlayer(remain.copy());
            }
        }

        if (current.isEmpty()) {
            outSlot.setByPlayer(result.copy());
        } else {
            current.grow(result.getCount());
        }
        menu.broadcastChanges();
    }

    private static List<ItemStack> craftSlotsToList(VisualCraftingMenu menu, int count) {
        List<ItemStack> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(menu.slots.get(i).getItem());
        }
        return list;
    }

    /** 输入指纹：物品 + 数量 + 组件 + 输出槽状态。 */
    private static int fingerprint(List<ItemStack> inputs, ItemStack output) {
        int hash = 1;
        for (ItemStack stack : inputs) {
            hash = 31 * hash + stackHash(stack);
        }
        return 31 * hash + stackHash(output);
    }

    private static int stackHash(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int h = BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode();
        h = 31 * h + stack.getCount();
        h = 31 * h + stack.getComponents().hashCode();
        return h;
    }
}
