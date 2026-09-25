package com.visualcrafting.trade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Maps an existing villager profession to another registered POI/job-site block.
 *
 * Config:
 * world/visualcrafting/job_sites/<profession>.json
 * {
 *   "block": "minecraft:lectern"
 * }
 *
 * The target block must already be registered as a POI. This deliberately does
 * not fabricate POI registrations at runtime, because villager pathfinding and
 * POI occupancy rely on the real POI registry.
 */
public final class VisualCraftingJobSiteHandler {
    private VisualCraftingJobSiteHandler() {}

    public static Predicate<Holder<PoiType>> override(VillagerProfession profession) {
        String id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession) == null
                ? "" : BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).toString();
        if (id.isEmpty()) return null;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;

        File file = server.getWorldPath(LevelResource.ROOT).toFile();
        file = new File(new File(new File(file, "visualcrafting"), "job_sites"),
                normalize(id) + ".json");
        if (!file.isFile()) return null;

        try {
            JsonObject json = JsonParser.parseString(
                    Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.has("block")) return null;

            String blockId = json.get("block").getAsString().trim();
            var blockOptional = BuiltInRegistries.BLOCK.getOptional(
                    net.minecraft.resources.ResourceLocation.parse(blockId));
            if (blockOptional.isEmpty()) return null;

            Block block = blockOptional.get();
            Optional<Holder<PoiType>> poi = findPoi(block);
            if (poi.isEmpty()) {
                System.err.println("[VisualCrafting] Job-site block has no registered POI: "
                        + blockId + " for " + id);
                return null;
            }

            Holder<PoiType> target = poi.get();
            return candidate -> candidate.equals(target);
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to load job-site override "
                    + file.getAbsolutePath() + ": " + e.getMessage());
            return null;
        }
    }

    private static Optional<Holder<PoiType>> findPoi(Block block) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            Optional<Holder<PoiType>> poi = PoiTypes.forState(state);
            if (poi.isPresent()) return poi;
        }
        return Optional.empty();
    }

    private static String normalize(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }
}
