package com.visualcrafting.worldgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 矿物禁用登记表（数据包侧，生成侧与写出侧共用）。
 *
 * <p>采用「数据包覆盖 placed_feature + 生成数量置 0」的禁用方案：</p>
 * <ol>
 *   <li>被禁矿物的 placed_feature 在数据包中被覆盖为 count = 0，永不生成；</li>
 *   <li>覆盖文件中的 feature 引用固定指向原版必然存在的 {@code minecraft:ore_dirt}，
 *       避免因 configured_feature 未注册导致 "Unbound values in registry" 世界加载失败；</li>
 *   <li>被禁矿物清单落盘于数据包根目录 {@code visualcrafting_disabled_ores.json}（非 data 目录，
 *       不参与数据包内容解析），MergeManager 写出 worldgen 时读取该清单强制保持数量 0，
 *       使玩家之后再次「生成/更新矿物」也不会解除禁用（不复发）。</li>
 * </ol>
 *
 * <p>禁用登记以矿物（短名，如 {@code iron_ore}）为唯一维度，不涉及群系/维度。</p>
 */
public final class OreDisableRegistry {

    /** 禁用清单文件名（数据包根目录下，与 pack.mcmeta 同级） */
    public static final String FILE_NAME = "visualcrafting_disabled_ores.json";

    /** 清单格式版本 */
    public static final int FORMAT = 1;

    /** Minecraft 1.21 / 1.21.1 的数据包格式号 */
    public static final int DATAPACK_FORMAT = 48;

    /** 本模组命名空间前缀 */
    public static final String MOD_NAMESPACE = "visualcrafting:";

    /** placed_feature / configured_feature 的短 id 前缀 */
    public static final String MINERAL_PREFIX = "visualcrafting_ore_mineral_";
    public static final String BYPRODUCT_PREFIX = "visualcrafting_ore_byproduct_";

    /** 数据包内相对 data/visualcrafting 的目录前缀 */
    public static final String PLACED_FEATURE_DIR = "worldgen/placed_feature/";
    public static final String CONFIGURED_FEATURE_DIR = "worldgen/configured_feature/";
    public static final String BIOME_MODIFIER_DIR = "neoforge/biome_modifier/";

    /** 通用禁用（remove_features）biome modifier 短 id 前缀（按矿物短名区分） */
    public static final String REMOVE_MODIFIER_PREFIX = "remove_visualcrafting_";

    /** 通用禁用 biome_modifier tag 短 id（values 汇总所有 remove_* modifier） */
    public static final String REMOVE_MODIFIER_TAG = "remove_visualcrafting_ores";

    /** remove_features 覆盖的维度群系 tag：主世界 / 下界 / 末地 */
    public static final String[] DIMENSION_BIOME_TAGS = {
            "#minecraft:is_overworld", "#minecraft:is_nether", "#minecraft:is_end"
    };

    /** remove_features 的默认生成阶段（矿石特征所在 step） */
    public static final String STEP_UNDERGROUND_ORES = "underground_ores";

    /** remove_features 对非矿石特征（植被/装饰类单方块）使用的生成阶段 */
    public static final String STEP_VEGETAL_DECORATION = "vegetal_decoration";

    /**
     * 禁用覆盖中 feature 字段的占位引用：原版必然存在的 configured_feature。
     * 配合 count = 0 保证既不生成任何方块，也不会产生未绑定的注册表引用。
     */
    public static final String PLACEHOLDER_FEATURE = "minecraft:ore_dirt";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private OreDisableRegistry() {
    }

    /**
     * 补全资源引用命名空间：裸名（不含冒号）一律视为本模组资源，补上 visualcrafting: 前缀。
     * 幂等：null / 空 / 已含冒号的原样返回。
     */
    public static String qualify(String resourceId) {
        if (resourceId == null || resourceId.isEmpty() || resourceId.indexOf(58) >= 0) {
            return resourceId;
        }

        return MOD_NAMESPACE + resourceId;
    }

    public static String mineralPlacedId(String shortName) {
        return MINERAL_PREFIX + shortName;
    }

    public static String byproductPlacedId(String shortName) {
        return BYPRODUCT_PREFIX + shortName;
    }

    /** placed_feature 短 id → MergeManager 的 recipeId（相对 data/visualcrafting，不含 .json） */
    public static String placedRecipeId(String placedId) {
        return PLACED_FEATURE_DIR + placedId;
    }

    /** 带命名空间的完整 placed_feature id */
    public static String qualifiedPlacedId(String shortName, boolean byproduct) {
        return MOD_NAMESPACE + (byproduct ? byproductPlacedId(shortName) : mineralPlacedId(shortName));
    }

    /**
     * 构造「禁用覆盖」placed_feature：生成数量置 0。
     * placement 保留 count / in_square / biome 三个修饰符，其中 count = 0 使该特征永不生成。
     */
    public static JsonObject buildDisabledPlacedFeature() {
        JsonObject root = new JsonObject();
        root.addProperty("feature", PLACEHOLDER_FEATURE);

        JsonArray placement = new JsonArray();
        JsonObject count = new JsonObject();
        count.addProperty("type", "minecraft:count");
        count.addProperty("count", 0);
        placement.add(count);

        JsonObject inSquare = new JsonObject();
        inSquare.addProperty("type", "minecraft:in_square");
        placement.add(inSquare);

        JsonObject biome = new JsonObject();
        biome.addProperty("type", "minecraft:biome");
        placement.add(biome);

        root.add("placement", placement);
        return root;
    }

    // ------------------------------------------------------------------
    // 通用禁用（remove_features）：适用于任何被 placed_feature 生成的单方块
    // ------------------------------------------------------------------

    /** 某矿物（短名）对应的 remove_features modifier 短 id */
    public static String removeModifierId(String shortName) {
        return REMOVE_MODIFIER_PREFIX + shortName;
    }

    /** remove modifier 相对 data/visualcrafting 的 recipeId（不含 .json，供 MergeManager 路由） */
    public static String removeModifierRecipeId(String shortName) {
        return BIOME_MODIFIER_DIR + removeModifierId(shortName);
    }

    /** remove modifier tag 相对 data/visualcrafting 的 recipeId */
    public static String removeModifierTagRecipeId() {
        return "neoforge/tags/worldgen/biome_modifier/" + REMOVE_MODIFIER_TAG;
    }

    /**
     * 从 MergeManager 的 recipeId 反解矿物短名；非 remove modifier 返回 null。
     * 形如 {@code neoforge/biome_modifier/remove_visualcrafting_iron_ore} → {@code iron_ore}。
     */
    public static String extractShortNameFromRemoveModifier(String recipeId) {
        if (recipeId == null) {
            return null;
        }

        String prefix = BIOME_MODIFIER_DIR + REMOVE_MODIFIER_PREFIX;
        if (!recipeId.startsWith(prefix)) {
            return null;
        }

        String name = recipeId.substring(prefix.length());
        return name.isEmpty() ? null : name;
    }

    /**
     * 构造 {@code neoforge:remove_features} biome modifier：把指定 placed_feature 从
     * 三维度群系 tag 的对应生成阶段整体移除。非侵入、可叠加，与 add_features 互不竞争文件加载顺序。
     */
    public static JsonObject buildRemoveFeaturesModifier(List<String> featureIds, String step) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "neoforge:remove_features");

        JsonArray biomes = new JsonArray();
        for (String tag : DIMENSION_BIOME_TAGS) {
            biomes.add(tag);
        }
        root.add("biomes", biomes);

        JsonArray features = new JsonArray();
        if (featureIds != null) {
            for (String featureId : featureIds) {
                if (featureId != null && !featureId.isEmpty()) {
                    features.add(featureId);
                }
            }
        }
        root.add("features", features);

        root.addProperty("step", step == null || step.isEmpty() ? STEP_UNDERGROUND_ORES : step);
        return root;
    }

    /** biome_modifier tag：values 引用所有已登记的 remove modifier id（replace=false 可叠加） */
    public static JsonObject buildRemoveModifierTag(Collection<String> modifierIds) {
        JsonObject root = new JsonObject();
        root.addProperty("replace", Boolean.valueOf(false));
        JsonArray values = new JsonArray();
        if (modifierIds != null) {
            for (String id : modifierIds) {
                if (id != null && !id.isEmpty()) {
                    values.add(id.contains(":") ? id : MOD_NAMESPACE + id);
                }
            }
        }
        root.add("values", values);
        return root;
    }

    /**
     * 就地强制 placed_feature 的生成数量为 0：
     * 把 placement 中所有 {@code minecraft:count} 修饰符的 count 置 0（无 count 条目则插入到最前），
     * 同时补全 feature 字段的命名空间。返回是否发生了修改。
     */
    public static boolean forceZeroCount(JsonObject placedFeature) {
        if (placedFeature == null) {
            return false;
        }

        boolean changed = false;
        JsonArray placement;
        if (placedFeature.has("placement") && placedFeature.get("placement").isJsonArray()) {
            placement = placedFeature.getAsJsonArray("placement");
        } else {
            placement = new JsonArray();
            placedFeature.add("placement", placement);
            changed = true;
        }

        boolean hasCount = false;
        for (int i = 0; i < placement.size(); i++) {
            JsonElement element = placement.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            JsonObject modifier = element.getAsJsonObject();
            JsonElement type = modifier.get("type");
            if (type != null && type.isJsonPrimitive() && "minecraft:count".equals(type.getAsString())) {
                hasCount = true;
                JsonElement current = modifier.get("count");
                if (current == null || !current.isJsonPrimitive() || current.getAsInt() != 0) {
                    modifier.addProperty("count", 0);
                    changed = true;
                }
            }
        }

        if (!hasCount) {
            JsonObject count = new JsonObject();
            count.addProperty("type", "minecraft:count");
            count.addProperty("count", 0);
            JsonArray rebuilt = new JsonArray();
            rebuilt.add(count);
            for (int i = 0; i < placement.size(); i++) {
                rebuilt.add(placement.get(i));
            }

            placedFeature.add("placement", rebuilt);
            changed = true;
        }

        JsonElement feature = placedFeature.get("feature");
        if (feature != null && feature.isJsonPrimitive()) {
            String featureId = feature.getAsString();
            String qualified = qualify(featureId);
            if (qualified != null && !qualified.equals(featureId)) {
                placedFeature.addProperty("feature", qualified);
                changed = true;
            }
        }

        return changed;
    }

    /** 生效校验：placement 中至少存在一个 minecraft:count 修饰符，且所有 count 均为 0 */
    public static boolean verifyZeroCount(JsonObject placedFeature) {
        if (placedFeature == null || !placedFeature.has("placement") || !placedFeature.get("placement").isJsonArray()) {
            return false;
        }

        JsonArray placement = placedFeature.getAsJsonArray("placement");
        boolean hasCount = false;
        for (int i = 0; i < placement.size(); i++) {
            JsonElement element = placement.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            JsonObject modifier = element.getAsJsonObject();
            JsonElement type = modifier.get("type");
            if (type == null || !type.isJsonPrimitive() || !"minecraft:count".equals(type.getAsString())) {
                continue;
            }

            hasCount = true;
            JsonElement count = modifier.get("count");
            if (count == null || !count.isJsonPrimitive()) {
                return false;
            }

            try {
                if (count.getAsInt() != 0) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return hasCount;
    }

    /** 校验 placed_feature 的 feature 引用是否自带命名空间（防空引用） */
    public static boolean hasQualifiedFeature(JsonObject placedFeature) {
        if (placedFeature == null) {
            return false;
        }

        JsonElement feature = placedFeature.get("feature");
        if (feature == null || !feature.isJsonPrimitive()) {
            return false;
        }

        String featureId = feature.getAsString();
        return featureId != null && featureId.indexOf(58) > 0;
    }

    // ------------------------------------------------------------------
    // 禁用清单读写
    // ------------------------------------------------------------------

    private static JsonObject readRegistry(File datapackRoot) {
        if (datapackRoot == null) {
            return new JsonObject();
        }

        File file = new File(datapackRoot, FILE_NAME);
        if (!file.exists()) {
            return new JsonObject();
        }

        try {
            JsonElement element = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (Exception e) {
            // 清单损坏：备份后重建，避免禁用信息静默丢失
            try {
                Files.copy(file.toPath(), new File(datapackRoot, FILE_NAME + ".bak").toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
                // 备份失败不影响后续重建
            }
        }

        return new JsonObject();
    }

    /** 读取被禁矿物清单：短名 → 条目 JSON */
    public static Map<String, JsonObject> loadEntries(File datapackRoot) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        JsonObject registry = readRegistry(datapackRoot);
        JsonElement ores = registry.get("ores");
        if (ores == null || !ores.isJsonObject()) {
            return result;
        }

        for (Map.Entry<String, JsonElement> entry : ores.getAsJsonObject().entrySet()) {
            if (entry.getValue() != null && entry.getValue().isJsonObject()) {
                result.put(entry.getKey(), entry.getValue().getAsJsonObject());
            } else {
                result.put(entry.getKey(), new JsonObject());
            }
        }

        return result;
    }

    /** 被禁矿物短名集合 */
    public static Set<String> disabledShortNames(File datapackRoot) {
        return new LinkedHashSet<>(loadEntries(datapackRoot).keySet());
    }

    /** 该矿物（短名）是否已被禁用 */
    public static boolean isDisabled(File datapackRoot, String shortName) {
        if (shortName == null || shortName.isEmpty()) {
            return false;
        }

        return loadEntries(datapackRoot).containsKey(shortName);
    }

    /**
     * 判断某个 worldgen 条目是否命中禁用清单。
     * 入参可为 MergeManager 的 recipeId（{@code worldgen/placed_feature/xxx}）或 placed_feature 短 id。
     */
    public static boolean isDisabledPlacedFeature(File datapackRoot, String placedIdOrRecipeId) {
        if (placedIdOrRecipeId == null || placedIdOrRecipeId.isEmpty()) {
            return false;
        }

        String placedId = placedIdOrRecipeId;
        int slash = placedId.lastIndexOf('/');
        if (slash >= 0) {
            placedId = placedId.substring(slash + 1);
        }

        if (placedId.endsWith(".json")) {
            placedId = placedId.substring(0, placedId.length() - 5);
        }

        for (String shortName : disabledShortNames(datapackRoot)) {
            if (placedId.equals(mineralPlacedId(shortName)) || placedId.equals(byproductPlacedId(shortName))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 登记一条矿物禁用记录并落盘（写入数据包根目录清单文件）。
     * 已有记录会被更新（保留其他矿物记录），返回是否写入成功。
     * placedFeatures 可为空：不记录反查到的 placed_feature id（仅本模组 count=0 覆盖场景）。
     */
    public static boolean mark(File datapackRoot, String shortName, String blockId, List<String> placedFeatures) {
        if (datapackRoot == null || shortName == null || shortName.isEmpty()) {
            return false;
        }

        JsonObject registry = readRegistry(datapackRoot);
        JsonObject ores = registry.get("ores") != null && registry.get("ores").isJsonObject()
                ? registry.getAsJsonObject("ores")
                : new JsonObject();

        JsonObject entry = new JsonObject();
        entry.addProperty("block", blockId == null ? "" : blockId);
        entry.addProperty("mineral", qualifiedPlacedId(shortName, false));
        entry.addProperty("byproduct", qualifiedPlacedId(shortName, true));
        if (placedFeatures != null && !placedFeatures.isEmpty()) {
            JsonArray placed = new JsonArray();
            for (String id : placedFeatures) {
                placed.add(id);
            }
            entry.add("placedFeatures", placed);
        }
        entry.addProperty("disabledAt", System.currentTimeMillis());
        ores.add(shortName, entry);

        registry.addProperty("format", FORMAT);
        registry.addProperty("updatedAt", System.currentTimeMillis());
        registry.addProperty("mode", "placed_feature_count_zero");
        registry.add("ores", ores);

        try {
            Files.createDirectories(datapackRoot.toPath());
            Files.writeString(new File(datapackRoot, FILE_NAME).toPath(), GSON.toJson(registry), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 兼容旧调用：不携带 placed feature 反查结果 */
    public static boolean mark(File datapackRoot, String shortName, String blockId) {
        return mark(datapackRoot, shortName, blockId, (List<String>) null);
    }

    /**
     * 读取某被禁矿物（短名）反查记录到的 placed_feature id 列表（完整命名空间）。
     * 供 MergeManager 写出 remove_features modifier 时强制保持 features 一致（防复发）。
     */
    public static List<String> loadPlacedFeatures(File datapackRoot, String shortName) {
        List<String> result = new ArrayList<>();
        if (datapackRoot == null || shortName == null) {
            return result;
        }

        JsonObject registry = readRegistry(datapackRoot);
        JsonElement ores = registry.get("ores");
        if (ores == null || !ores.isJsonObject()) {
            return result;
        }

        JsonElement entry = ores.getAsJsonObject().get(shortName);
        if (entry == null || !entry.isJsonObject()) {
            return result;
        }

        JsonElement placed = entry.getAsJsonObject().get("placedFeatures");
        if (placed != null && placed.isJsonArray()) {
            for (JsonElement element : placed.getAsJsonArray()) {
                if (element != null && element.isJsonPrimitive()) {
                    result.add(element.getAsString());
                }
            }
        }
        return result;
    }

    /** 移除一条矿物禁用记录（保留以备后续「启用矿物生成」使用） */
    public static boolean clear(File datapackRoot, String shortName) {
        if (datapackRoot == null || shortName == null) {
            return false;
        }

        JsonObject registry = readRegistry(datapackRoot);
        if (registry.get("ores") == null || !registry.get("ores").isJsonObject()) {
            return false;
        }

        JsonObject ores = registry.getAsJsonObject("ores");
        if (ores.has(shortName)) {
            ores.remove(shortName);
            registry.addProperty("updatedAt", System.currentTimeMillis());
            try {
                Files.writeString(new File(datapackRoot, FILE_NAME).toPath(), GSON.toJson(registry), StandardCharsets.UTF_8);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /**
     * 确保数据包根目录存在 pack.mcmeta 且 pack_format 与当前 Minecraft 版本一致
     * （1.21 / 1.21.1 = {@link #DATAPACK_FORMAT}）；格式号不符时修正，避免数据包被判为不兼容。
     * 返回 true 表示本次发生了写入（新建或修正）。
     */
    public static boolean ensurePackMcmeta(File datapackRoot) {
        if (datapackRoot == null) {
            return false;
        }

        File mcmeta = new File(datapackRoot, "pack.mcmeta");
        boolean needWrite = true;
        if (mcmeta.exists()) {
            needWrite = false;
            try {
                JsonElement element = JsonParser.parseString(Files.readString(mcmeta.toPath(), StandardCharsets.UTF_8));
                JsonObject pack = element != null && element.isJsonObject() && element.getAsJsonObject().get("pack") != null
                        && element.getAsJsonObject().get("pack").isJsonObject()
                        ? element.getAsJsonObject().getAsJsonObject("pack")
                        : null;
                JsonElement format = pack == null ? null : pack.get("pack_format");
                if (format == null || !format.isJsonPrimitive() || format.getAsInt() != DATAPACK_FORMAT) {
                    needWrite = true;
                }
            } catch (Exception e) {
                needWrite = true;
            }
        }

        if (!needWrite) {
            return false;
        }

        try {
            Files.createDirectories(datapackRoot.toPath());
            String content = "{\n  \"pack\": {\n    \"pack_format\": " + DATAPACK_FORMAT
                    + ",\n    \"description\": \"VisualCrafting Ore Generation\"\n  }\n}";
            Files.writeString(mcmeta.toPath(), content, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
