package com.visualcrafting.recipe.universal;

import com.visualcrafting.VisualCraftingTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Hooks the universal recipe index into the normal server datapack reload.
 */
@EventBusSubscriber(modid = VisualCraftingTable.MOD_ID)
public final class UniversalRecipeEvents {
    private UniversalRecipeEvents() {}

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new UniversalRecipeReloadListener());
    }
}
