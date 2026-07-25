package com.visualcrafting.event;

import com.visualcrafting.block.VisualCraftingBlockEntity;
import com.visualcrafting.screen.VisualCraftingMenu;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = "visualcrafting")
public class CraftingTickHandler {

    @SubscribeEvent
    public static void handleServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof VisualCraftingMenu vcMenu) {
                craftItem(vcMenu);
            }
        }
    }

    /**
     * Auto-craft on server tick.
     * Matches benchmark: single parameter (VisualCraftingMenu),
     * accesses blockEntity directly rather than looking up from world.
     * Only operates in custom-craft mode (mode == 0).
     */
    private static void craftItem(VisualCraftingMenu menu) {
        VisualCraftingBlockEntity vcbe = menu.blockEntity;
        if (vcbe == null) return;
        if (vcbe.getMode() != 0) return;   // custom-craft only

        Level level = vcbe.getLevel();
        if (level == null || level.isClientSide) return;

        int tier = menu.getTier();
        int gridSize = menu.getGridSize();

        // Collect items from active grid slots
        List<ItemStack> inputList = craftSlotsToList(menu, gridSize * gridSize);

        CraftingInput input = CraftingInput.of(gridSize, gridSize, inputList);
        RecipeManager recipeManager = level.getRecipeManager();
        Optional<RecipeHolder<CraftingRecipe>> optional =
                recipeManager.getRecipeFor(RecipeType.CRAFTING, input, level);

        ItemStack result = optional.isPresent()
                ? optional.get().value().assemble(input, level.registryAccess())
                : ItemStack.EMPTY;

        if (result.isEmpty()) return;

        // Output slot is index 81
        Slot outSlot = menu.slots.get(81);
        ItemStack current = outSlot.getItem();

        if (current.isEmpty()) {
            // Consume one from each input slot, then set output
            int maxSlots = gridSize * gridSize;
            for (int i = 0; i < maxSlots; i++) {
                Slot s = menu.slots.get(i);
                if (s.hasItem()) {
                    s.getItem().shrink(1);
                    if (s.getItem().isEmpty()) {
                        s.set(ItemStack.EMPTY);
                    }
                }
            }
            outSlot.setByPlayer(result.copy());
            menu.broadcastChanges();
        } else if (ItemStack.isSameItemSameComponents(current, result)
                   && current.getCount() + result.getCount() <= current.getMaxStackSize()) {
            // Merge with existing output
            int maxSlots = gridSize * gridSize;
            for (int i = 0; i < maxSlots; i++) {
                Slot s = menu.slots.get(i);
                if (s.hasItem()) {
                    s.getItem().shrink(1);
                    if (s.getItem().isEmpty()) {
                        s.set(ItemStack.EMPTY);
                    }
                }
            }
            current.grow(result.getCount());
            menu.broadcastChanges();
        }
    }

    private static List<ItemStack> craftSlotsToList(VisualCraftingMenu menu, int count) {
        List<ItemStack> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(menu.slots.get(i).getItem());
        }
        return list;
    }
}
