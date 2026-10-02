package com.visualcrafting.recipe.universal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads recipe JSON directly from the active data-pack/resource stack.
 *
 * This intentionally scans resources rather than trying to cast Recipe
 * implementations. Custom RecipeSerializers therefore remain discoverable
 * even when VisualCrafting has never heard of their Java classes.
 */
public final class UniversalRecipeScanner {
    private UniversalRecipeScanner() {}

    public static Map<ResourceLocation, UniversalRecipe> scan(ResourceManager resourceManager) {
        Map<ResourceLocation, UniversalRecipe> result = new LinkedHashMap<>();

        List<Map.Entry<ResourceLocation, Resource>> resources =
                resourceManager.listResources("recipes", path -> path.getPath().endsWith(".json"))
                        .entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                        .toList();

        for (Map.Entry<ResourceLocation, Resource> entry : resources) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();
            if (!path.startsWith("recipes/") || !path.endsWith(".json")) {
                continue;
            }

            String recipePath = path.substring("recipes/".length(), path.length() - ".json".length());
            ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                    fileId.getNamespace(), recipePath);

            try (Reader reader = entry.getValue().openAsReader()) {
                var parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    continue;
                }
                JsonObject json = parsed.getAsJsonObject();
                result.put(recipeId, new UniversalRecipe(recipeId, fileId, json));
            } catch (Exception ignored) {
                // A malformed/non-object recipe should not prevent other mods'
                // recipes from being indexed.
            }
        }

        return result;
    }
}
