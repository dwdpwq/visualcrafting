package com.visualcrafting.trade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Maps an existing villager profession to a job-site block.
 *
 * Config:
 * world/visualcrafting/job_sites/<profession>.json
 * {
 *   "block": "minecraft:lectern"
 * }
 *
 * Since the arbitrary-block fix the configured block no longer needs to be a
 * vanilla POI block:
 * - if the configured block is itself a registered POI, it is matched exactly
 *   (original behavior preserved);
 * - otherwise the block is bound to the profession's vanilla POI type, and
 *   {@link com.visualcrafting.mixin.PoiTypesForStateMixin} makes
 *   PoiTypes.forState() report that type for the block at its world location,
 *   so the vanilla POI registry / villager pathfinding / profession assignment
 *   all work with the real POI system.
 */
public final class VisualCraftingJobSiteHandler {
    private VisualCraftingJobSiteHandler() {}

    /** block id -> POI type key that should be reported for that block. */
    private static volatile Map<String, ResourceKey<PoiType>> configuredPoiByBlock = Map.of();
    private static volatile boolean cacheLoaded = false;

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
            var blockOptional = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockId));
            if (blockOptional.isEmpty()) return null;

            ResourceKey<PoiType> key = resolvePoiKey(blockOptional.get(), normalize(id));
            if (key == null) {
                System.err.println("[VisualCrafting] No POI type available for job-site block "
                        + blockId + " for " + id);
                return null;
            }

            return candidate -> candidate.is(key);
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to load job-site override "
                    + file.getAbsolutePath() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Resolve the POI type key used for a configured job-site block.
     * Prefers the block's own POI type; falls back to the profession's vanilla POI type.
     */
    private static ResourceKey<PoiType> resolvePoiKey(Block block, String profId) {
        Optional<ResourceKey<PoiType>> own = findRegisteredPoiKey(block);
        if (own.isPresent()) return own.get();
        return poiKeyForProfession(profId);
    }

    /**
     * Look up the configured POI type for an arbitrary block.
     * Used by PoiTypesForStateMixin so that a configured custom block reports
     * its mapped POI type when the world scans/creates POI records.
     * Returns null when the block is not part of any job_sites config.
     */
    public static ResourceKey<PoiType> resolveConfiguredPoiKey(MinecraftServer server, Block block) {
        if (block == null) return null;
        if (!cacheLoaded) refreshCache(server);
        return configuredPoiByBlock.get(BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    /** Rebuild the block-id -> POI-type cache from world/visualcrafting/job_sites/*.json. */
    public static void refreshCache(MinecraftServer server) {
        Map<String, ResourceKey<PoiType>> map = new HashMap<>();
        try {
            File jobSiteDir = new File(new File(
                    server.getWorldPath(LevelResource.ROOT).toFile(), "visualcrafting"), "job_sites");
            File[] files = jobSiteDir.isDirectory() ? jobSiteDir.listFiles() : null;
            if (files != null) {
                for (File f : files) {
                    if (!f.isFile() || !f.getName().endsWith(".json")) continue;
                    String profId = f.getName().substring(0, f.getName().length() - 5);
                    try {
                        JsonObject json = JsonParser.parseString(
                                Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                        if (!json.has("block")) continue;
                        String blockId = json.get("block").getAsString().trim();
                        var blockOptional = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockId));
                        if (blockOptional.isEmpty()) continue;
                        ResourceKey<PoiType> key = resolvePoiKey(blockOptional.get(), profId);
                        if (key != null) map.putIfAbsent(blockId, key);
                    } catch (Exception ignore) {
                        // skip unreadable entries
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to refresh job-site POI cache: " + e.getMessage());
        }
        configuredPoiByBlock = map;
        cacheLoaded = true;
    }

    /**
     * Vanilla POI lookup that bypasses PoiTypesForStateMixin:
     * iterates registry holders directly instead of calling PoiTypes.forState(),
     * so the cache build never recurses into the mixin.
     */
    private static Optional<ResourceKey<PoiType>> findRegisteredPoiKey(Block block) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            for (Holder<PoiType> holder : BuiltInRegistries.POINT_OF_INTEREST_TYPE.holders().toList()) {
                if (holder.value().is(state)) {
                    Optional<ResourceKey<PoiType>> key = BuiltInRegistries.POINT_OF_INTEREST_TYPE
                            .getResourceKey(holder.value());
                    if (key.isPresent()) return key;
                }
            }
        }
        return Optional.empty();
    }

    /** Profession id (namespace stripped, e.g. "cleric") -> its vanilla POI type key. */
    private static ResourceKey<PoiType> poiKeyForProfession(String profId) {
        return switch (profId) {
            case "armorer" -> PoiTypes.ARMORER;
            case "butcher" -> PoiTypes.BUTCHER;
            case "cartographer" -> PoiTypes.CARTOGRAPHER;
            case "cleric" -> PoiTypes.CLERIC;
            case "farmer" -> PoiTypes.FARMER;
            case "fisherman" -> PoiTypes.FISHERMAN;
            case "fletcher" -> PoiTypes.FLETCHER;
            case "leatherworker" -> PoiTypes.LEATHERWORKER;
            case "librarian" -> PoiTypes.LIBRARIAN;
            case "mason" -> PoiTypes.MASON;
            case "shepherd" -> PoiTypes.SHEPHERD;
            case "toolsmith" -> PoiTypes.TOOLSMITH;
            case "weaponsmith" -> PoiTypes.WEAPONSMITH;
            default -> null;
        };
    }

    private static String normalize(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }
}
