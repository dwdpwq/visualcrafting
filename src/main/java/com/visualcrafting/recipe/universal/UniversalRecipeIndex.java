package com.visualcrafting.recipe.universal;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-side cache of the currently active raw recipe JSON index. */
public final class UniversalRecipeIndex {
    private static volatile Map<ResourceLocation, UniversalRecipe> recipes = Map.of();

    private UniversalRecipeIndex() {}

    public static void replace(Map<ResourceLocation, UniversalRecipe> next) {
        recipes = Collections.unmodifiableMap(new LinkedHashMap<>(next));
    }

    public static Collection<UniversalRecipe> all() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public static UniversalRecipe byId(ResourceLocation id) {
        return recipes.get(id);
    }

    public static List<UniversalRecipe> byNamespace(String namespace) {
        List<UniversalRecipe> result = new ArrayList<>();
        for (UniversalRecipe recipe : recipes.values()) {
            if (recipe.id().getNamespace().equals(namespace)) {
                result.add(recipe);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static int size() {
        return recipes.size();
    }

    public static void clear() {
        recipes = Map.of();
    }
}
