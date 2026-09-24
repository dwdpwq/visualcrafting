package com.visualcrafting.worldgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 运行时 Block 级禁用名单（世界生成拦截用）。
 * <p>线程安全模型：volatile 不可变快照 + 写时复制。世界生成可能在 worker 线程上并行执行
 * （{@link net.minecraft.world.level.chunk.WorldGenRegion#setBlockState} 会被并发调用），
 * 读路径零锁、快照不可变，保证并发安全且不会读到半写状态。</p>
 * <p>持久化位置：{@code world/visualcrafting/disabled_blocks.json}，由 {@link BlockDisableEvents}
 * 在服务端启动时加载，名单变化时由网络 handler 落盘。</p>
 */
public final class BlockDisableRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "disabled_blocks.json";

    private static volatile Set<Block> disabledBlocks = Set.of();

    /**
     * 禁止封禁的核心地形方块：这些方块由地表规则（surface rule）/流体湖/含水层等基础生成路径写入，
     * 封禁后会把世界地表、海洋、熔岩湖挖成虚空并触发大规模流体更新崩溃（卡顿 + 区块无法正常使用）。
     * 服务端与客户端共用，作为禁用操作的硬性保护。
     */
    private static final Set<Block> PROTECTED_BLOCKS = buildProtectedBlocks();

    private static Set<Block> buildProtectedBlocks() {
        Set<Block> set = new HashSet<>();
        // 地表/地下基础地形
        addAll(set, "minecraft:stone", "minecraft:deepslate", "minecraft:granite", "minecraft:diorite",
                "minecraft:andesite", "minecraft:tuff", "minecraft:bedrock", "minecraft:smooth_basalt");
        // 地表覆盖层（surface rule 写入，封禁=地表消失）
        addAll(set, "minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol",
                "minecraft:mycelium", "minecraft:mud", "minecraft:clay", "minecraft:sand", "minecraft:red_sand",
                "minecraft:sandstone", "minecraft:red_sandstone", "minecraft:gravel", "minecraft:ice",
                "minecraft:packed_ice", "minecraft:snow_block", "minecraft:snow", "minecraft:powder_snow");
        // 流体（含水层/熔岩湖/海洋，封禁=流体崩溃）
        addAll(set, "minecraft:water", "minecraft:lava", "minecraft:flowing_water", "minecraft:flowing_lava");
        // 下界/末地基础地形
        addAll(set, "minecraft:netherrack", "minecraft:soul_sand", "minecraft:soul_soil", "minecraft:basalt",
                "minecraft:blackstone", "minecraft:end_stone", "minecraft:obsidian");
        return Set.copyOf(set);
    }

    private static void addAll(Set<Block> set, String... ids) {
        for (String id : ids) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(id));
            if (block != null && block != Blocks.AIR) {
                set.add(block);
            }
        }
    }

    private BlockDisableRegistry() {
    }

    /** 核心地形方块：不允许封禁（防止地表/流体被替换成空气造成世界崩塌）。 */
    public static boolean isProtected(Block block) {
        return block != null && PROTECTED_BLOCKS.contains(block);
    }

    public static boolean isEmpty() {
        return disabledBlocks.isEmpty();
    }

    public static boolean isDisabled(Block block) {
        return block != null && disabledBlocks.contains(block);
    }

    /** @return true 表示名单实际发生变化 */
    public static boolean add(Block block) {
        if (block == null || block == Blocks.AIR) {
            return false;
        }
        if (disabledBlocks.contains(block)) {
            return false;
        }
        Set<Block> next = new HashSet<>(disabledBlocks);
        next.add(block);
        disabledBlocks = Set.copyOf(next);
        return true;
    }

    /** @return true 表示名单实际发生变化 */
    public static boolean remove(Block block) {
        if (block == null || !disabledBlocks.contains(block)) {
            return false;
        }
        Set<Block> next = new HashSet<>(disabledBlocks);
        next.remove(block);
        disabledBlocks = Set.copyOf(next);
        return true;
    }

    /** 当前名单快照（不可变），用于 S2C 全量同步。 */
    public static List<ResourceLocation> snapshotIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Block block : disabledBlocks) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key != null) {
                ids.add(key);
            }
        }
        return ids;
    }

    /** 服务端启动时加载（需在预生成模组开始前调用）。 */
    public static void load(Path worldRoot) {
        try {
            File file = worldRoot.resolve("visualcrafting").resolve(FILE_NAME).toFile();
            if (!file.exists()) {
                disabledBlocks = Set.of();
                return;
            }
            JsonElement root = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            Set<Block> loaded = new HashSet<>();
            if (root != null && root.isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray()) {
                    String id = el.getAsString();
                    ResourceLocation rl = ResourceLocation.tryParse(id);
                    if (rl == null) {
                        continue;
                    }
                    Block block = BuiltInRegistries.BLOCK.get(rl);
                    if (block != null && block != Blocks.AIR) {
                        loaded.add(block);
                    }
                }
            }
            disabledBlocks = Set.copyOf(loaded);
            System.out.println("[VisualCrafting] Loaded " + disabledBlocks.size() + " disabled blocks from " + file);
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to load disabled blocks: " + e.getMessage());
        }
    }

    /** 名单变化后落盘。 */
    public static void save(Path worldRoot) {
        try {
            File vcDir = worldRoot.resolve("visualcrafting").toFile();
            vcDir.mkdirs();
            File file = new File(vcDir, FILE_NAME);
            JsonArray arr = new JsonArray();
            for (ResourceLocation id : snapshotIds()) {
                arr.add(id.toString());
            }
            Files.writeString(file.toPath(), GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to save disabled blocks: " + e.getMessage());
        }
    }
}
