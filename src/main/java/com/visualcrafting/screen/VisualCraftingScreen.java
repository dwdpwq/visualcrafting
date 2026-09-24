package com.visualcrafting.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.visualcrafting.block.VisualCraftingBlockEntity;
import com.visualcrafting.merge.MergeManager;
import com.visualcrafting.network.DimensionBiomesData;
import com.visualcrafting.network.DisableBlockPacket;
import com.visualcrafting.network.ModMessages;
import com.visualcrafting.network.RequestDisabledBlocksPacket;
import com.visualcrafting.worldgen.BlockDisableRegistry;
import com.visualcrafting.worldgen.OreDisableRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

public class VisualCraftingScreen extends AbstractContainerScreen<VisualCraftingMenu> {
    static final int BG_COLOR = -1072689136;
    static final int CRAFTING_SLOT_BORDER_COLOR = -1;
    static final String[] TIER_LABELS = new String[]{"gui.visualcrafting.tier.basic", "gui.visualcrafting.tier.advanced", "gui.visualcrafting.tier.elite", "gui.visualcrafting.tier.ultimate"};
    static final int CHEM_SLOT_X = 80;
    static final int CHEM_SLOT_Y = 50;
    static final int AMOUNT_SLOT_Y_GAP = 22;
    static final int INFUSE_RECIPES_OFFSET_X = 183;
    static final int INFUSE_RECIPES_OFFSET_Y = 13;
    static final ResourceLocation TEX_T0 = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/crafting_table.png");
    static final ResourceLocation TEX_T1 = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/crafting_table_tier1.png");
    static final ResourceLocation TEX_T2 = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/crafting_table_tier2.png");
    static final ResourceLocation TEX_T3 = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/crafting_table_tier3.png");
    static final ResourceLocation TAB_CRAFT = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/tab_crafting.png");
    static final ResourceLocation TAB_INFUSE = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/tab_infusing.png");
    static final ResourceLocation TAB_VILLAGER = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/tab_crafting.png");
    static final ResourceLocation TAB_FOOD = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/tab_food.png");
    static final ResourceLocation TAB_ORE = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/tab_ore.png");
    static final ResourceLocation ICO_DEL = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/recipe_delete.png");
    static final ResourceLocation ICO_SHAPE = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/recipe_shaped.png");
    static final ResourceLocation ICO_SHLESS = ResourceLocation.fromNamespaceAndPath("visualcrafting", "textures/gui/recipe_shapeless.png");
    static final int TEXTURE_SIZE = 256;
    static final ItemStack ICON_CRAFT = new ItemStack(Items.CRAFTING_TABLE);
    static final ItemStack ICON_INFUSE = new ItemStack(Items.ENCHANTING_TABLE);
    static final ItemStack ICON_VILLAGER = new ItemStack(Items.EMERALD);
    static final ItemStack ICON_FOOD = new ItemStack(Items.COOKED_BEEF);
    static final ItemStack ICON_NAME = new ItemStack(Items.NAME_TAG);
    static final ItemStack ICON_CREATE = new ItemStack(Items.TRIAL_KEY);
    static final ItemStack ICON_ENHANCE = new ItemStack(Items.OMINOUS_TRIAL_KEY);
    static final int WORLD_HEIGHT = 319;
    static final Field SLOT_X;
    static final Field SLOT_Y;

    static {
        Field slotX = null;
        Field slotY = null;
        try {
            slotX = Slot.class.getDeclaredField("x");
            slotY = Slot.class.getDeclaredField("y");
            slotX.setAccessible(true);
            slotY.setAccessible(true);
        }

        catch (Exception e) {
            logWarn("Failed to access Slot.x / Slot.y fields via reflection", e);
        }

        SLOT_X = slotX;
        SLOT_Y = slotY;
    }
    int tier = 0;
    int mode = 0;
    int format = 0;
    int scrollOffset = 0;
    int btnOffsetX = 6;
    int btnOffsetY = 0;
    int gridOffsetX = -8;
    int gridOffsetY = -8;
    int gridSlotOffsetX = 16;
    int gridSlotOffsetY = 13;
    int infAmountLabelOffsetX = 24;
    int infAmountLabelOffsetY = 0;
    int infButtonsOffsetX = 6;
    int infButtonsOffsetY = 0;
    int infChemSlotOffsetX = 2;
    int infChemSlotOffsetY = 4;
    int infEditBoxOffsetX = 185;
    int infEditBoxOffsetY = 69;
    int infInputSlotOffsetX = -1;
    int infInputSlotOffsetY = -1;
    int infInputSlotSlotOffsetX = 3;
    int infInputSlotSlotOffsetY = -5;
    int infInvLineOffsetX = -1;
    int infInvLineOffsetY = -1;
    int infInvSlotSlotOffsetX = 3;
    int infInvSlotSlotOffsetY = -7;
    int infOutSlotLineOffsetX = -1;
    int infOutSlotLineOffsetY = -1;
    int infOutSlotSlotOffsetX = 1;
    int infOutSlotSlotOffsetY = -2;
    int inputLabelX = 22;
    int inputLabelY = 3;
    int outputLabelX = 22;
    int outputLabelY = 3;
    int chemLabelX = 20;
    int chemLabelY = 5;
    int chemLineX = 0;
    int chemLineY = 0;
    int invLabelOffsetX = -179;
    int invLabelOffsetY = -4;
    int invLineOffsetX = -1;
    int invLineOffsetY = -1;
    int invSlotSlotOffsetX = 9;
    int invSlotSlotOffsetY = 21;
    boolean saveNbtOnCraft = false;
    int outSlotLineOffsetX = -1;
    int outSlotLineOffsetY = -1;
    int outSlotSlotOffsetX = 16;
    int outSlotSlotOffsetY = 11;
    int recipesOffsetX = 0;
    int recipesOffsetY = 2;
    int tierOffsetY = 0;
    List<VisualCraftingBlockEntity.SavedRecipe> recipes = new ArrayList<VisualCraftingBlockEntity.SavedRecipe>();
    List<VisualCraftingBlockEntity.InfusingRecipe> infusingRecipes = new ArrayList<VisualCraftingBlockEntity.InfusingRecipe>();
    List<Button> tierButtons = new ArrayList<Button>();
    List<Button> funcButtons = new ArrayList<Button>();
    Button formatToggle;
    ItemStack markedContainer = ItemStack.EMPTY;
    List<ResourceLocation> mode2Dimensions = new ArrayList<ResourceLocation>();
    Map<String, List<ResourceLocation>> mode2BiomesByDim = new LinkedHashMap<String, List<ResourceLocation>>();
    boolean mode2DataPending = true;
    List<ResourceLocation> mode2Biomes = new ArrayList<ResourceLocation>();
    int mode2DimIdx = 0;
    int mode2BiomeIdx = 0;
    Set<Integer> mode2BiomeSelectedIndices = new LinkedHashSet<Integer>();
    DropdownWidget mode2BiomeDropdown;
    int mode2Pct = 50;
    int mode2MinY = -63;
    int mode2MaxY = 319;
    int mode2MineralPct = 50;
    int mode2ByproductPct = 50;
    int mode2MineralCountMin = 1;
    int mode2MineralCountMax = 3;
    int mode2ByproductCountMin = 1;
    int mode2ByproductCountMax = 2;
    boolean mode2WidgetsInited = false;
    EditBox mode2MinYEdit = null;
    EditBox mode2MaxYEdit = null;
    Button mode2BtnMineralGen = null;
    Button mode2BtnBanGen = null;
    /** 被禁矿物短名缓存（带节流，避免每帧读盘）；禁用动作后置空强制重读 */
    private Set<String> mode2DisabledOresCache = null;
    private long mode2DisabledOresCacheAt = 0L;
    Button mode2BtnConfig = null;
    EditBox mode2MineralCountMinEdit = null;
    EditBox mode2MineralCountMaxEdit = null;
    EditBox mode2ByproductCountMinEdit = null;
    EditBox mode2ByproductCountMaxEdit = null;
    DropdownWidget mode2Dropdown;
    int mode5Hunger = 4;
    float mode5Saturation = 0.3f;
    private Set<Integer> mode5PotionSelectedIdx = new TreeSet<Integer>();
    int mode5PotionLevel = 1;
    List<String> mode5PotionNames = new ArrayList<String>();
    List<String> mode5PotionIds = new ArrayList<String>();
    DropdownWidget mode5PotionDropdown;
    EditBox mode5HungerEdit;
    EditBox mode5SaturationEdit;
    EditBox mode5PotionLevelEdit;
    EditBox mode5DurationEdit;
    boolean mode5DurationInfinite = false;
    /** 药水效果时长，单位：秒（内部生成脚本时换算 tick = 秒 * 20） */
    int mode5Duration = 30;
    int mode5EatSeconds = 0;
    EditBox mode5EatSecondsEdit;
    float mode5PotionProbability = 0.0f;
    EditBox mode5ProbabilityEdit;
    int mode2OffsetX = 8;
    ItemStack mode5LastSlot0Item = ItemStack.EMPTY;
    Button mode5BtnSave;
    Button mode5BtnDelete;
    Button mode5BtnConfig;
    Button mode5BtnRefresh;
        Checkbox mode5InfiniteCheckbox;
    Button mode5BtnAddEffect;
    /** mode5 暂存效果预览（渲染层，不修改真实物品；退出界面自动清除）: {name, level, duration} */
    private final List<String[]> mode5PreviewEffects = new ArrayList<String[]>();
    // ===================== Mode 6: 命名牌标签页（名称 / 描述 + RGB 拾色器） =====================
    String mode6NameInput = "";
    static final int MODE6_LORE_VISIBLE = 4;
    static final int MODE6_LORE_MAX = 25;
    final java.util.List<String> mode6LoreLines = new java.util.ArrayList<>(java.util.Arrays.asList("", "", "", ""));
    final int[] mode6LoreColors = new int[MODE6_LORE_MAX];
    final boolean[] mode6LoreRainbow = new boolean[MODE6_LORE_MAX];
    int mode6NameColor = -1;
    boolean mode6NameRainbow = false;
    boolean mode6Glow = false;
    final boolean[] mode6NameFonts = new boolean[4];
    final boolean[][] mode6LoreFonts = new boolean[MODE6_LORE_MAX][4];
    int mode6LoreScroll = 0;
    boolean mode6LoreRefreshing = false;
    int mode6FontTarget = -1;
    EditBox mode6NameEdit;
    final EditBox[] mode6LoreEdits = new EditBox[MODE6_LORE_VISIBLE];
    final Button[] mode6FontButtons = new Button[4];
    Button mode6BtnSave;
    Button mode6BtnDelete;
    Button mode6BtnConfig;
    Button mode6BtnGlow;
    EditBox mode6PickerHexEdit;
    boolean mode6PickerOpen = false;
    int mode6PickerTarget = -2;
    int mode6PickerColor = 0xFFFFFF;
    static final int[] NAME_PALETTE = new int[]{
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
            0x880000, 0xCC0000, 0xFF3333, 0xFF7777, 0xFFBBBB, 0x880044, 0xCC0055, 0xFF0077,
            0xFF44AA, 0xFF88CC, 0x884400, 0xCC6600, 0xFF8800, 0xFFAA44, 0xFFCC88, 0x888800,
            0xCCCC00, 0xEEEE33, 0xFFFF66, 0x88AA00, 0xAACC33, 0xCCEE66, 0x008800, 0x00CC00
    };
    // ===================== Mode 7: 创建物品标签页（类型/子类型 + KubeJS ItemBuilder 脚本） =====================
    static final String[] MODE7_TYPES = new String[]{"gui.visualcrafting.mode7.type.armor", "gui.visualcrafting.mode7.type.tool", "gui.visualcrafting.mode7.type.trinket"};
    static final String[][] MODE7_SUBTYPES = new String[][]{
            {"gui.visualcrafting.mode7.subtype.helmet", "gui.visualcrafting.mode7.subtype.chestplate", "gui.visualcrafting.mode7.subtype.leggings", "gui.visualcrafting.mode7.subtype.boots"},
            {"gui.visualcrafting.mode7.subtype.sword", "gui.visualcrafting.mode7.subtype.pickaxe", "gui.visualcrafting.mode7.subtype.axe", "gui.visualcrafting.mode7.subtype.shovel", "gui.visualcrafting.mode7.subtype.hoe"},
            {"gui.visualcrafting.mode7.subtype.ring", "gui.visualcrafting.mode7.subtype.necklace", "gui.visualcrafting.mode7.subtype.charm", "gui.visualcrafting.mode7.subtype.bracelet"}
    };
    static final String[] MODE7_ARMOR_PROTECTION = new String[]{"2", "6", "5", "2"};
    static final String[] MODE7_ARMOR_DURABILITY = new String[]{"165", "240", "225", "195"};
    static final String[] MODE7_TOOL_DAMAGE = new String[]{"3", "2", "6", "1.5", "0"};
    static final String[] MODE7_TOOL_SPEED = new String[]{"1.6", "1.2", "1.0", "1.0", "1.0"};
    static final String[] MODE7_TRINKET_TAGS = new String[]{"curios:ring", "curios:necklace", "curios:charm", "curios:bracelet"};
    int mode7TypeIdx = 0;
    int mode7SubtypeIdx = 0;
    DropdownWidget mode7TypeDropdown;
    DropdownWidget mode7SubtypeDropdown;
    EditBox mode7NameEdit;
    EditBox mode7RegIdEdit;
    Button mode7BtnGenerate;
    Button mode7BtnConfig;
    Button mode7BtnTexture;
    String mode7TexturePath = null;
    // ===================== Mode 8: 物品增强标签页（属性/附魔/耐久 + 自动类型检测 + 滚动） =====================
    static final String[][] MODE8_ATTRIBUTES = new String[][]{
            {"Attack Damage", "minecraft:generic.attack_damage"},
            {"Attack Speed", "minecraft:generic.attack_speed"},
            {"Armor", "minecraft:generic.armor"},
            {"Armor Toughness", "minecraft:generic.armor_toughness"},
            {"Max Health", "minecraft:generic.max_health"},
            {"Movement Speed", "minecraft:generic.movement_speed"},
            {"Knockback Resistance", "minecraft:generic.knockback_resistance"},
            {"Luck", "minecraft:generic.luck"}
    };
    static final String[] MODE8_ATTR_OPS = new String[]{"gui.visualcrafting.mode8.op.add", "gui.visualcrafting.mode8.op.mult_base", "gui.visualcrafting.mode8.op.mult_total"};
    static final String[] MODE8_SLOTS = new String[]{"any", "mainhand", "offhand", "head", "chest", "legs", "feet"};
    static final String[][] MODE8_ENCHANTS = new String[][]{
            {"Sharpness", "minecraft:sharpness"},
            {"Smite", "minecraft:smite"},
            {"Bane of Arthropods", "minecraft:bane_of_arthropods"},
            {"Knockback", "minecraft:knockback"},
            {"Fire Aspect", "minecraft:fire_aspect"},
            {"Looting", "minecraft:looting"},
            {"Sweeping Edge", "minecraft:sweeping_edge"},
            {"Efficiency", "minecraft:efficiency"},
            {"Silk Touch", "minecraft:silk_touch"},
            {"Unbreaking", "minecraft:unbreaking"},
            {"Fortune", "minecraft:fortune"},
            {"Protection", "minecraft:protection"},
            {"Fire Protection", "minecraft:fire_protection"},
            {"Blast Protection", "minecraft:blast_protection"},
            {"Projectile Protection", "minecraft:projectile_protection"},
            {"Feather Falling", "minecraft:feather_falling"},
            {"Respiration", "minecraft:respiration"},
            {"Aqua Affinity", "minecraft:aqua_affinity"},
            {"Thorns", "minecraft:thorns"},
            {"Depth Strider", "minecraft:depth_strider"},
            {"Swift Sneak", "minecraft:swift_sneak"},
            {"Soul Speed", "minecraft:soul_speed"},
            {"Mending", "minecraft:mending"},
            {"Power", "minecraft:power"},
            {"Punch", "minecraft:punch"},
            {"Flame", "minecraft:flame"},
            {"Infinity", "minecraft:infinity"},
            {"Luck of the Sea", "minecraft:luck_of_the_sea"},
            {"Lure", "minecraft:lure"},
            {"Multishot", "minecraft:multishot"},
            {"Piercing", "minecraft:piercing"},
            {"Quick Charge", "minecraft:quick_charge"},
            {"Channeling", "minecraft:channeling"},
            {"Riptide", "minecraft:riptide"},
            {"Loyalty", "minecraft:loyalty"},
            {"Impaling", "minecraft:impaling"},
            {"Curse of Vanishing", "minecraft:vanishing_curse"},
            {"Curse of Binding", "minecraft:binding_curse"}
    };
    static final int MODE8_SCROLL_TOP = 33;
    static final int MODE8_ROW_H = 16;
    int mode8ScrollOffset = 0;
    private final List<String> mode8AttributeIds = new ArrayList<>();
    private final List<String> mode8EnchantmentIds = new ArrayList<>();
    private static final String[] MODE8_TOOL_TIER_IDS = new String[]{"wood", "stone", "iron", "gold", "diamond", "netherite"};
    private static final String MODE8_TOOL_TIER_PREFIX = "__tool_tier__:";
    private static final String[] MODE8_TOOL_TIER_LABELS = new String[]{"工具等级：木质（等级 0）", "工具等级：石质（等级 1）", "工具等级：铁质（等级 2）", "工具等级：金质（等级 0）", "工具等级：钻石（等级 3）", "工具等级：下界合金（等级 4）"};
    private String mode8LastItemId = "";
    private int mode8LastRegistrySignature = 0;
    private static final String[] MODE8_ATTR_LABELS_CN = new String[]{"攻击伤害", "攻击速度", "护甲值", "护甲韧性", "最大生命", "移动速度", "击退抗性", "幸运"};
    private static final String[] MODE8_SLOT_LABELS_CN = new String[]{"任意", "主手", "副手", "头盔", "胸甲", "护腿", "靴子"};
    private static final String[] MODE8_ENCHANT_LABELS_CN = new String[]{"锋利", "亡灵杀手", "节肢杀手", "击退", "火焰附加", "抢夺", "横扫之刃", "效率", "精准采集", "耐久", "时运", "保护", "火焰保护", "爆炸保护", "弹射物保护", "摔落保护", "水下呼吸", "水下速掘", "荆棘", "深海探索者", "迅捷潜行", "灵魂疾行", "经验修补", "力量", "冲击", "火矢", "无限", "海之眷顾", "饵钓", "多重射击", "穿透", "快速装填", "引雷", "激流", "忠诚", "穿刺", "消失诅咒", "绑定诅咒"};
    int mode8DetectedType = 0;
    DropdownWidget mode8AttrDropdown;
    DropdownWidget mode8OpDropdown;
    DropdownWidget mode8SlotDropdown;
    DropdownWidget mode8EnchantDropdown;
    EditBox mode8AttrValueEdit;
    EditBox mode8EnchantLevelEdit;
    EditBox mode8DurabilityEdit;
    Button mode8BtnGenerate;
    Button mode8BtnConfig;
    Button mode8BtnChange;
    boolean mode5AlwaysEdible;
    ItemStack selectedChemical = ItemStack.EMPTY;
    private final ItemStack[] ghostItems = new ItemStack[9];
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("VisualCrafting");

    public static void logWarn(String message, Throwable cause) {
        LOGGER.warn(message, cause);
    }

    private static void logDebug(String message) {
    }

    public int getGuiTop() {
        return this.topPos;
    }

    public int getGuiLeft() {
        return this.leftPos;
    }

    public int getGuiWidth() {
        return this.imageWidth;
    }

    public void setGuiWidth(int width) {
        this.imageWidth = width;
    }

    public int getGuiHeight() {
        return this.imageHeight;
    }

    public void setGuiHeight(int height) {
        this.imageHeight = height;
    }

    public void setGuiLeft(int left) {
        this.leftPos = left;
    }

    public void setGuiTop(int top) {
        this.topPos = top;
    }

    public Font getGuiFont() {
        return this.font;
    }

    public Minecraft getScreenMinecraft() {
        return this.minecraft;
    }

    public VisualCraftingMenu getScreenMenu() {
        return this.menu;
    }

    public <T extends AbstractWidget> T addScreenWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    public int getChemSlotX() {
        return this.leftPos + 80 + this.infChemSlotOffsetX;
    }

    public int getChemSlotY() {
        return this.topPos + 50 + this.infChemSlotOffsetY;
    }

    public int[] getChemSlotArea() {
        return new int[]{this.getChemSlotX(), this.getChemSlotY(), 18, 18};
    }

    int getInfuseRecipesX() {
        return this.leftPos + 183 + this.recipesOffsetX;
    }

    int getInfuseRecipesY() {
        return this.topPos + 13 + this.recipesOffsetY;
    }

    public void setSelectedChemical(ItemStack itemStack) {
        this.selectedChemical = itemStack;
    }

    public ItemStack getSelectedChemical() {
        return this.selectedChemical;
    }

    public void setGhostItem(int slotIndex, ItemStack itemStack) {
        if (slotIndex >= 0 && slotIndex < 9) {
            this.ghostItems[slotIndex] = itemStack;
        }
    }

    public ItemStack getGhostItem(int slotIndex) {
        return slotIndex >= 0 && slotIndex < 9 && this.ghostItems[slotIndex] != null ? this.ghostItems[slotIndex] : ItemStack.EMPTY;
    }

    public int getAmountSlotX() {
        return this.leftPos + 82;
    }

    public int getAmountSlotY() {
        return this.topPos + 77 - 20;
    }

    private CompoundTag encodePattern() {
        CompoundTag compoundTag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (int i = 0; i < 9; ++i) {
            CompoundTag compoundTag2 = new CompoundTag();
            compoundTag2.putInt("slot", i);
            CompoundTag compoundTag3 = this.menu.getChemGhost(i);
            if (compoundTag3 != null && !compoundTag3.isEmpty()) {
                compoundTag2.putString("type", "chemical");
                compoundTag2.put("data", compoundTag3.copy());
            } else if (this.ghostItems[i] != null && !this.ghostItems[i].isEmpty()) {
                compoundTag2.putString("type", "item");
                compoundTag2.put("data", this.ghostItems[i].save(this.minecraft.level.registryAccess()));
            } else {
                compoundTag2.putString("type", "empty");
            }

            listTag.add(compoundTag2);
        }

        compoundTag.put("inputs", listTag);
        return compoundTag;
    }

    private void decodePattern(CompoundTag compoundTag) {
        if (compoundTag != null) {
            ListTag listTag = compoundTag.getList("inputs", 10);
            for (int i = 0; i < listTag.size() && i < 9; ++i) {
                CompoundTag compoundTag2 = listTag.getCompound(i);
                String type = compoundTag2.getString("type");
                if ("chemical".equals(type) && compoundTag2.contains("data")) {
                    this.menu.setChemGhost(i, compoundTag2.getCompound("data"));
                    try {
                        this.ghostItems[i] = MekanismIntegration.createChemicalTagItem(compoundTag2.getCompound("data"));
                    }

                    catch (Throwable e) {
                        VisualCraftingScreen.logWarn("Mekanism not available, skipping chemical ghost item", e);
                        this.ghostItems[i] = ItemStack.EMPTY;
                    }

                    continue;
                }

                this.ghostItems[i] = "item".equals(type) && compoundTag2.contains("data") ? ItemStack.parse(this.minecraft.level.registryAccess(), compoundTag2.getCompound("data")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            }

        }
    }

    static void setSlotX(Slot slot, int x) {
        if (SLOT_X != null) {
            try {
                SLOT_X.setInt(slot, x);
            }

            catch (IllegalAccessException e) {
                VisualCraftingScreen.logWarn("Failed to reflectively set Slot.x", e);
            }

        }
    }

    static void setSlotY(Slot slot, int y) {
        if (SLOT_Y != null) {
            try {
                SLOT_Y.setInt(slot, y);
            }

            catch (IllegalAccessException e) {
                VisualCraftingScreen.logWarn("Failed to reflectively set Slot.y", e);
            }

        }
    }

    public VisualCraftingScreen(VisualCraftingMenu visualCraftingMenu, Inventory inventory, Component component) {
        super(visualCraftingMenu, inventory, component);
        int gridSize = this.getGridSize();
        this.imageWidth = 260 + (gridSize - 3) * 18;
        this.imageHeight = gridSize * 18 + 161;
    }

    protected void init() {
        super.init();
        this.titleLabelY -= 2;
        this.inventoryLabelY += 10;
        this.inventoryLabelX += this.invLabelOffsetX;
        this.inventoryLabelY += this.invLabelOffsetY;
        this.loadOffsets();
        this.readBEState();
        this.updateGuiSize();
        this.menu.setCurrentMode(this.mode);
        this.menu.updateSlotPositions(this.tier);
        this.rebuildWidgets();
    }

    private void readBEState() {
        BlockEntity blockEntity;
        if (this.minecraft != null && this.minecraft.level != null && (blockEntity = this.minecraft.level.getBlockEntity(this.menu.blockPos)) instanceof VisualCraftingBlockEntity) {
            VisualCraftingBlockEntity visualCraftingBlockEntity = (VisualCraftingBlockEntity)blockEntity;
            this.mode = VisualCraftingBlockEntity.normalizeMode(visualCraftingBlockEntity.getMode());
            this.tier = visualCraftingBlockEntity.getTier();
            this.format = visualCraftingBlockEntity.getFormat();
            this.recipes = new ArrayList<VisualCraftingBlockEntity.SavedRecipe>(visualCraftingBlockEntity.getRecipes());
            this.infusingRecipes = new ArrayList<VisualCraftingBlockEntity.InfusingRecipe>(visualCraftingBlockEntity.getInfusingRecipes());
        }
    }

    int getGridSize() {
        return switch (this.tier) {
            case 1 -> 5;
            case 2 -> 7;
            case 3 -> 9;
            default -> 3;
        };
    }

    protected void rebuildWidgets() {
        this.clearWidgets();
        this.tierButtons.clear();
        this.funcButtons.clear();
        this.menu.setCurrentMode(this.mode);
        this.menu.updateSlotPositions(this.tier);
        if (this.mode == 0) {
            this.initCraftingWidgets();
        } else if (this.mode == 5) {
            this.initMode5Widgets();
                } else if (this.mode == 1 || this.mode == 2) {
            this.initInfusingWidgets();
        } else if (this.mode == 6) {
            this.initNameWidgets();
        } else if (this.mode == 7) {
            this.initMode7Widgets();
        } else if (this.mode == 8) {
            this.initMode8Widgets();
        } else {
            // 兜底：非法模式（例如旧存档里被写成 4 的值）统一回退到合成界面
            this.mode = VisualCraftingBlockEntity.MODE_CRAFTING;
            this.menu.setCurrentMode(this.mode);
            this.menu.updateSlotPositions(this.tier);
            this.initCraftingWidgets();
        }

        // 统一槽位布局必须最后执行：在 initModeXWidgets 覆盖坐标之后再应用最终布局，避免被 init 覆盖
        this.layoutCurrentModeSlots();

        // 打开 GUI 时请求一次运行时 Block 级禁用名单，保证缓存与按钮状态一致
        PacketDistributor.sendToServer(new RequestDisabledBlocksPacket(), new CustomPacketPayload[0]);
    }

    private void onShapedCraft(Button button) {
        this.doCraft(true);
    }

    private void onShapelessCraft(Button button) {
        this.doCraft(false);
    }

    private void doCraft(boolean shaped) {
        Slot slot = this.menu.slots.get(81);
        if (!slot.getItem().isEmpty()) {
            int gridSize = this.getGridSize();
            int maxSlots = gridSize * gridSize;
            ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>();
            boolean hasAnyItem = false;
            for (int i = 0; i < maxSlots; ++i) {
                ItemStack itemStack = this.menu.craftSlots.getItem(i);
                arrayList.add(itemStack.copy());
                if (itemStack.isEmpty()) continue;
                hasAnyItem = true;
            }

            if (hasAnyItem) {
                PacketDistributor.sendToServer(new ModMessages.AddRecipePacket(this.menu.blockPos, shaped, slot.getItem().copy(), arrayList, this.saveNbtOnCraft), new CustomPacketPayload[0]);
            }

        }
    }

    private void onDelCraft(Button button) {
        ItemStack itemStack = this.menu.slots.get(81).getItem();
        if (!itemStack.isEmpty()) {
            PacketDistributor.sendToServer(new ModMessages.DeleteByOutputPacket(this.menu.blockPos, itemStack.copy()), new CustomPacketPayload[0]);
        }
    }

    private void onFmtCraft(Button button) {
        File file = new File("config/visualcrafting");
        if (!file.exists()) {
            file.mkdirs();
        }

        Util.getPlatform().openFile(file);
    }

    private void onFormatToggle(Button button) {
        this.format = 1 - this.format;
        PacketDistributor.sendToServer(new ModMessages.FormatUpdatePacket(this.menu.blockPos, this.format), new CustomPacketPayload[0]);
        button.setMessage(Component.literal(this.format == 0 ? "KubeJS" : "CRT"));
        this.tierButtons.forEach(b -> {
            b.visible = this.format != 0;
        });
    }

    private void onTier(int tierValue) {
        this.tier = tierValue;
        PacketDistributor.sendToServer(new ModMessages.TierUpdatePacket(this.menu.blockPos, tierValue), new CustomPacketPayload[0]);
        this.menu.updateSlotPositions(tierValue);
        this.updateGuiSize();
        this.rebuildWidgets();
    }

    private void onInfuseAdd(Button button) {
        ItemStack itemStack = this.menu.slots.get(81).getItem();
        ItemStack itemStack2 = this.menu.craftSlots.getItem(0);
        ItemStack itemStack3 = this.selectedChemical.copy();
        VisualCraftingMenu visualCraftingMenu = this.menu;
        if (itemStack3.isEmpty() && visualCraftingMenu.chemSlotData == null) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.chem_empty").getString());
        } else if (itemStack2.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.input_empty").getString());
        } else if (itemStack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.output_empty").getString());
        } else {
            if (itemStack3.isEmpty() && visualCraftingMenu.chemSlotData != null) {
                try {
                    CompoundTag synthTag = new CompoundTag();
                    synthTag.putString("chemicalId", visualCraftingMenu.chemSlotData.chemicalId);
                    synthTag.putLong("amount", visualCraftingMenu.chemAmount);
                    itemStack3 = MekanismIntegration.createChemicalTagItem(synthTag);
                } catch (Throwable e) {
                    VisualCraftingScreen.logWarn("Failed to synthesize chemical item from ghost slot data", e);
                }

            }

            int chemAmount = visualCraftingMenu.chemAmount;
            if (chemAmount < 0) {
                chemAmount = 10;
            } else if (chemAmount > 9000) {
                chemAmount = 9000;
            }

            ChemSlotData chemSlotData = visualCraftingMenu.chemSlotData;
            if (chemSlotData == null) {
                try {
                    CompoundTag compoundTag = MekanismIntegration.getChemicalTagFromItem(itemStack3);
                    if (compoundTag != null) {
                        chemSlotData = MekanismIntegration.buildChemSlotDataFromTag(compoundTag);
                    }

                }

                catch (Throwable e) {
                    VisualCraftingScreen.logWarn("Mekanism not available, skipping chemical data extraction", e);
                }

            }

            if (chemSlotData == null) {
                this.showStatus(Component.translatable("gui.visualcrafting.status.chem_invalid").getString());
            } else {
                PacketDistributor.sendToServer(new ModMessages.AddInfusingRecipePacket(visualCraftingMenu.blockPos, itemStack3.copy(), itemStack2.copy(), itemStack.copy(), chemAmount), new CustomPacketPayload[0]);
                this.showStatus(Component.translatable("gui.visualcrafting.status.infusing_created").getString());
            }

        }
    }

    private void onInfuseDelete(Button button) {
        ItemStack itemStack = this.menu.slots.get(81).getItem();
        if (!itemStack.isEmpty()) {
            PacketDistributor.sendToServer(new ModMessages.DeleteInfusingByOutputPacket(this.menu.blockPos, itemStack.copy()), new CustomPacketPayload[0]);
        }
    }

    private void onInfuseConfig(Button button) {
        File file = new File("kubejs/server_scripts");
        if (!file.exists()) {
            file.mkdirs();
        }

        Util.getPlatform().openFile(file);
    }

    public void reverseParseOreGenFiles() {
        File file = new File(this.getDatapackDataDir(), "worldgen/configured_feature/visualcrafting_ore_mineral.json");
        if (!file.exists()) {
            return;
        }

        int primaryCountMin = 0;
        int primaryCountMax = 0;
        int byproductCountMin = 0;
        int byproductCountMax = 0;
        int primaryHeightMin = 0;
        int primaryHeightMax = 0;
        int clusterSize = 0;
        int byproductChance = 0;
        String oreBlock = "";
        String byproductBlock = "";
        String biomeTag = "";
        try {
            Gson gson = new Gson();
            String mineralItemId = null;
            String byproductItemId = null;
            int mineralVeinSize = -1;
            int mineralVeinsPerChunk = -1;
            int mineralMinY = -1;
            int mineralMaxY = -1;
            int byproductVeinSize = -63;
            int byproductMinY = 319;
            String biomeId = null;
            ArrayList<String> parsedBiomes = new ArrayList<String>();
            int byproductVeinsPerChunk = -1;
            int byproductMaxY = -1;
            // Parse mineral configured feature
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                JsonObject configuredJson = gson.fromJson(reader, JsonObject.class);
                JsonArray targets = configuredJson.getAsJsonObject("config").getAsJsonArray("targets");
                if (targets != null && targets.size() > 0) {
                    JsonObject stateObj = targets.get(0).getAsJsonObject().getAsJsonObject("state");
                    if (stateObj != null && stateObj.has("Name")) {
                        oreBlock = stateObj.get("Name").getAsString();
                    }

                }

            }

            // Parse byproduct configured feature
            File byproductConfigFile = new File(this.getDatapackDataDir(), "worldgen/configured_feature/visualcrafting_ore_byproduct.json");
            if (byproductConfigFile.exists()) {
                try (FileReader reader = new FileReader(byproductConfigFile, StandardCharsets.UTF_8)) {
                    JsonObject configuredJson = gson.fromJson(reader, JsonObject.class);
                    JsonArray targets = configuredJson.getAsJsonObject("config").getAsJsonArray("targets");
                    if (targets != null && targets.size() > 0) {
                        JsonObject stateObj = targets.get(0).getAsJsonObject().getAsJsonObject("state");
                        if (stateObj != null && stateObj.has("Name")) {
                            byproductBlock = stateObj.get("Name").getAsString();
                        }

                    }

                }

            }

            // Parse mineral placed feature
            File mineralPlacedFile = new File(this.getDatapackDataDir(), "worldgen/placed_feature/visualcrafting_ore_mineral.json");
            if (mineralPlacedFile.exists()) {
                try (FileReader reader = new FileReader(mineralPlacedFile, StandardCharsets.UTF_8)) {
                    JsonObject placedJson = gson.fromJson(reader, JsonObject.class);
                    for (JsonElement elem : placedJson.getAsJsonArray("placement")) {
                        JsonObject placementObj = elem.getAsJsonObject();
                        String type = placementObj.get("type").getAsString();
                        if ("minecraft:count".equals(type)) {
                            JsonObject valueObj = placementObj.getAsJsonObject("count").getAsJsonObject("value");
                            primaryCountMin = valueObj.get("min_inclusive").getAsInt();
                            primaryCountMax = valueObj.get("max_inclusive").getAsInt();
                        } else if ("minecraft:height_range".equals(type)) {
                            JsonObject heightObj = placementObj.getAsJsonObject("height");
                            primaryHeightMin = heightObj.getAsJsonObject("min_inclusive").get("absolute").getAsInt();
                            primaryHeightMax = heightObj.getAsJsonObject("max_inclusive").get("absolute").getAsInt();
                        }

                    }

                }

            }

            // Parse byproduct placed feature
            File byproductPlacedFile = new File(this.getDatapackDataDir(), "worldgen/placed_feature/visualcrafting_ore_byproduct.json");
            if (byproductPlacedFile.exists()) {
                try (FileReader reader = new FileReader(byproductPlacedFile, StandardCharsets.UTF_8)) {
                    JsonObject placedJson = gson.fromJson(reader, JsonObject.class);
                    for (JsonElement elem : placedJson.getAsJsonArray("placement")) {
                        JsonObject placementObj = elem.getAsJsonObject();
                        String type = placementObj.get("type").getAsString();
                        if ("minecraft:count".equals(type)) {
                            JsonObject valueObj = placementObj.getAsJsonObject("count").getAsJsonObject("value");
                            byproductCountMin = valueObj.get("min_inclusive").getAsInt();
                            byproductCountMax = valueObj.get("max_inclusive").getAsInt();
                        }

                    }

                }

            }

            // Parse biome modifier
            File biomeModifierFile = new File(this.getDatapackDataDir(), "neoforge/biome_modifier/add_visualcrafting_ore.json");
            if (biomeModifierFile.exists()) {
                try (FileReader reader = new FileReader(biomeModifierFile, StandardCharsets.UTF_8)) {
                    JsonObject biomeJson = gson.fromJson(reader, JsonObject.class);
                    if (biomeJson.has("biomes")) {
                        JsonElement biomesElem = biomeJson.get("biomes");
                        if (biomesElem.isJsonPrimitive()) {
                            biomeTag = biomesElem.getAsString();
                            parsedBiomes.add(biomeTag);
                        } else if (biomesElem.isJsonArray() && biomesElem.getAsJsonArray().size() > 0) {
                            JsonArray biomeArr = biomesElem.getAsJsonArray();
                            for (JsonElement elem : biomeArr) {
                                parsedBiomes.add(elem.getAsString());
                            }

                            biomeTag = biomeArr.get(0).getAsString();
                        }

                    }

                }

            }

            // Parse mineral cluster size from placed feature
            if (mineralPlacedFile.exists()) {
                try (FileReader reader = new FileReader(mineralPlacedFile, StandardCharsets.UTF_8)) {
                    JsonObject placedJson = gson.fromJson(reader, JsonObject.class);
                    clusterSize = placedJson.getAsJsonObject("config").getAsJsonObject("size").getAsInt();
                    this.mode2MineralPct = Math.clamp((int)((double)clusterSize / 0.15), 0, 100);
                } catch (Exception e) {
                    logWarn("Failed to parse mineral cluster size from placed feature", null);
                }

            }

            // Parse byproduct cluster size from placed feature
            if (byproductPlacedFile.exists()) {
                try (FileReader reader = new FileReader(byproductPlacedFile, StandardCharsets.UTF_8)) {
                    JsonObject placedJson = gson.fromJson(reader, JsonObject.class);
                    byproductChance = placedJson.getAsJsonObject("config").getAsJsonObject("size").getAsInt();
                    this.mode2ByproductPct = Math.clamp((int)((double)byproductChance / 0.08), 0, 100);
                } catch (Exception e) {
                    logWarn("Failed to parse byproduct cluster size from placed feature", null);
                }

            }

            // Apply parsed values
            if (mineralItemId != null && this.minecraft != null) {
                try {
                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(mineralItemId));
                    if (item != null) {
                        this.menu.slots.get(0).set(new ItemStack(item, 1));
                    }

                } catch (Exception e) {
                    logWarn("Failed to load mode2 mineral item", e);
                }

            }

            if (byproductItemId != null && this.minecraft != null) {
                try {
                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(byproductItemId));
                    if (item != null) {
                        this.menu.slots.get(1).set(new ItemStack(item, 1));
                    }

                } catch (Exception e) {
                    logWarn("Failed to load mode2 byproduct item", e);
                }

            }

            if (primaryCountMin > 0) {
                this.mode2MineralCountMin = Math.clamp(primaryCountMin, 1, 256);
                if (this.mode2MineralCountMinEdit != null) {
                    this.mode2MineralCountMinEdit.setValue(String.valueOf(primaryCountMin));
                }

            }

            if (primaryCountMax > 0) {
                this.mode2MineralCountMax = Math.clamp(primaryCountMax, 1, 256);
                if (this.mode2MineralCountMaxEdit != null) {
                    this.mode2MineralCountMaxEdit.setValue(String.valueOf(primaryCountMax));
                }

            }

            if (byproductCountMin > 0) {
                this.mode2ByproductCountMin = Math.clamp(byproductCountMin, 1, 256);
                if (this.mode2ByproductCountMinEdit != null) {
                    this.mode2ByproductCountMinEdit.setValue(String.valueOf(byproductCountMin));
                }

            }

            if (byproductCountMax > 0) {
                this.mode2ByproductCountMax = Math.clamp(byproductCountMax, 1, 256);
                if (this.mode2ByproductCountMaxEdit != null) {
                    this.mode2ByproductCountMaxEdit.setValue(String.valueOf(byproductCountMax));
                }

            }

            this.mode2MinY = primaryHeightMin;
            this.mode2MaxY = primaryHeightMax;
            if (this.mode2MinYEdit != null) {
                this.mode2MinYEdit.setValue(String.valueOf(primaryHeightMin));
            }

            if (this.mode2MaxYEdit != null) {
                this.mode2MaxYEdit.setValue(String.valueOf(primaryHeightMax));
            }

            // 群系选择恢复：biomes 数组与当前分类完整集合一致 → All；否则取首个群系（修复原 biomeId 恒为 null 导致永不恢复的缺陷）
            boolean fullBiomeList = !parsedBiomes.isEmpty() && !this.mode2Biomes.isEmpty();
            if (fullBiomeList) {
                for (ResourceLocation biome : this.mode2Biomes) {
                    if (!parsedBiomes.contains(biome.toString())) {
                        fullBiomeList = false;
                        break;
                    }
                }
            }

            if (fullBiomeList) {
                this.mode2BiomeSelectedIndices.clear();
                this.mode2BiomeSelectedIndices.add(0);
            } else {
                ResourceLocation biomeLoc = ResourceLocation.tryParse(biomeTag);
                if (!biomeTag.isEmpty() && biomeLoc != null) {
                    int idx = this.mode2Biomes.indexOf(biomeLoc);
                    if (idx >= 0) {
                        this.mode2BiomeIdx = idx + 1;
                        this.mode2BiomeSelectedIndices.add(idx + 1);
                    }

                }
            }

        } catch (Exception e) {
            logWarn("Failed to reverse-parse ore gen files", null);
        }
    }

    public void loadMode2Data() {
        DimensionBiomesData dimensionBiomesData = this.loadDimBiomesFromFile();
        if (dimensionBiomesData != null) {
            // 文件缓存命中时回填内存缓存，后续打开 GUI 直接命中内存，避免重复读文件/发请求
            ModMessages.setCachedDimBiomesData(dimensionBiomesData);
            this.applyDimBiomesData(dimensionBiomesData);
            return;
        }

        Object dimCache = ModMessages.getCachedDimBiomesData();
        if (dimCache instanceof DimensionBiomesData) {
            this.applyDimBiomesData((DimensionBiomesData)dimCache);
            return;
        }

        if (!this.mode2DataPending) {
            this.syncBiomesFromDim();
            this.updateMode2ButtonLabels();
            return;
        }

        this.mode2Dimensions.clear();
        this.mode2BiomesByDim.clear();
        this.mode2Biomes.clear();
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new ModMessages.RequestDimBiomesPacket(), new CustomPacketPayload[0]);
        }
    }

    public DimensionBiomesData loadDimBiomesFromFile() {
        try {
            if (this.minecraft == null) {
                return null;
            }

            File file = new File(this.minecraft.gameDirectory, "visualcrafting/dim_biomes_cache.json");
            if (!file.exists()) {
                return null;
            }

            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            DimensionBiomesData data = DimensionBiomesData.fromJson(json);

            // 缓存必须带来源实例标记且与当前实例一致才可信；
            // 否则视为跨实例/旧版残留，忽略并重新向服务端请求
            String launchedVersion = this.minecraft.getLaunchedVersion();
            if (data.sourceVersion == null || data.sourceVersion.isEmpty() || !data.sourceVersion.equals(launchedVersion)) {
                logWarn("Dim/biome cache source '" + data.sourceVersion + "' != current '" + launchedVersion + "', ignoring stale cache", null);
                return null;
            }

            return data;
        }

        catch (Exception e) {
            return null;
        }
    }

    public void applyDimBiomesData(DimensionBiomesData dimensionBiomesData) {
        if (dimensionBiomesData == null) {
            return;
        }

        this.mode2Dimensions.clear();
        for (String iterator : dimensionBiomesData.dimIds) {
            this.mode2Dimensions.add(ResourceLocation.parse(iterator));
        }

        if (this.mode2Dimensions.isEmpty()) {
            this.mode2Dimensions.add(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
            this.mode2Dimensions.add(ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether"));
            this.mode2Dimensions.add(ResourceLocation.fromNamespaceAndPath("minecraft", "the_end"));
        }

        this.mode2BiomesByDim.clear();
        for (Map.Entry<String, List<String>> entry : dimensionBiomesData.biomesByDim.entrySet()) {
            ArrayList<ResourceLocation> arrayList = new ArrayList<ResourceLocation>();
            List<String> list = entry.getValue();
            for (String biomeId : list) {
                arrayList.add(ResourceLocation.parse(biomeId));
            }

            this.mode2BiomesByDim.put(entry.getKey(), arrayList);
        }

        if (!dimensionBiomesData.allBiomes.isEmpty()) {
            ArrayList<ResourceLocation> arrayList = new ArrayList<ResourceLocation>();
            for (String biomeId : dimensionBiomesData.allBiomes) {
                arrayList.add(ResourceLocation.parse(biomeId));
            }

            this.mode2BiomesByDim.put("__all__", arrayList);
            this.mode2DimIdx = 0;
        }

        this.syncBiomesFromDim();
        if (this.mode2DimIdx > this.mode2Dimensions.size()) {
            this.mode2DimIdx = 0;
        }

        if (this.mode2Biomes.isEmpty()) {
            this.mode2BiomeIdx = 0;
        } else if (this.mode2BiomeIdx <= 0 || this.mode2BiomeIdx > this.mode2Biomes.size()) {
            this.mode2BiomeIdx = 1;
        }

        if (this.mode2BiomeSelectedIndices.isEmpty()) {
            this.mode2BiomeSelectedIndices.add(this.mode2BiomeIdx);
        }

        this.updateMode2ButtonLabels();
        this.mode2DataPending = false;
    }

    String getCurrentDimCategory() {
        return this.mode2DimIdx > 0 && this.mode2DimIdx <= this.mode2Dimensions.size() ? this.mode2Dimensions.get(this.mode2DimIdx - 1).toString() : "__all__";
    }

    public void syncBiomesFromDim() {
        String dimCategory = this.getCurrentDimCategory();
        List<ResourceLocation> list = this.mode2BiomesByDim.get(dimCategory);
        if (list == null) {
            list = this.mode2BiomesByDim.get("__all__");
        }

        this.mode2Biomes = list != null ? list : new ArrayList();
    }

    public void updateMode2ButtonLabels() {
        String displayName;
        String translationKey;
        ArrayList<String> arrayList;
        if (this.mode2Dropdown != null) {
            arrayList = new ArrayList<String>();
            if (this.mode2DataPending) {
                arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.loading"));
            } else {
                if (this.mode2BiomesByDim.containsKey("__all__")) {
                    arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.all_dimensions"));
                }

                for (ResourceLocation dimension : this.mode2Dimensions) {
                    translationKey = "generator." + dimension.getNamespace() + "." + dimension.getPath();
                    displayName = Language.getInstance().getOrDefault(translationKey);
                    if (displayName.equals(translationKey)) {
                        displayName = dimension.getPath();
                    }

                    arrayList.add(displayName);
                }
            }

            this.mode2Dropdown.setOptions(arrayList, this.mode2DimIdx);
        }

        if (this.mode2BiomeDropdown != null) {
            arrayList = new ArrayList();
            if (this.mode2DataPending) {
                arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.loading"));
            } else {
                if (!this.mode2Biomes.isEmpty()) {
                    arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.all_biomes"));
                }

                for (ResourceLocation resourceLocation : this.mode2Biomes) {
                    translationKey = "biome." + resourceLocation.getNamespace() + "." + resourceLocation.getPath();
                    displayName = Language.getInstance().getOrDefault(translationKey);
                    if (displayName.equals(translationKey)) {
                        displayName = resourceLocation.getPath();
                    }

                    arrayList.add(displayName);
                }
            }

            this.mode2BiomeDropdown.setMultiselect(true);
            this.mode2BiomeDropdown.setOptions(arrayList, 0);
            Set<Integer> validBiomeSel = new LinkedHashSet<Integer>();
            int allBiomeIdx = this.mode2Biomes.size();
            for (int n : this.mode2BiomeSelectedIndices) {
                if (n >= 0 && n <= allBiomeIdx) {
                    validBiomeSel.add(n);
                }
            }

            this.mode2BiomeDropdown.setSelectedIndices(validBiomeSel);
        }

        if (this.mode2BtnMineralGen != null) {
            String itemBlockId = this.getItemBlockId(this.menu.slots.get(0).getItem());
            String oreName = !itemBlockId.isEmpty() && itemBlockId.contains(":") ? itemBlockId.substring(itemBlockId.indexOf(58) + 1) : "";
            File file = new File(this.getDatapackDataDir(), "worldgen/configured_feature/visualcrafting_ore_mineral_" + oreName + ".json");
            if (!oreName.isEmpty() && file.exists()) {
                this.mode2BtnMineralGen.setMessage(Component.translatable("gui.visualcrafting.label.update_mineral"));
            } else {
                this.mode2BtnMineralGen.setMessage(Component.translatable("gui.visualcrafting.mode2.mineral_gen"));
            }

        }

        if (this.mode2BtnBanGen != null) {
            String itemBlockId = this.getItemBlockId(this.menu.slots.get(0).getItem());
            String oreName = !itemBlockId.isEmpty() && itemBlockId.contains(":") ? itemBlockId.substring(itemBlockId.indexOf(58) + 1) : "";
            boolean disabledStatic = !oreName.isEmpty() && this.getDisabledOresCached().contains(oreName);
            // 运行时通道命中（世界生成拦截名单）
            boolean disabledRuntime = false;
            Block targetBlock = this.resolveBlockFromItemId(itemBlockId);
            if (targetBlock != null && targetBlock != Blocks.AIR) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(targetBlock);
                disabledRuntime = blockId != null && ModMessages.getCachedDisabledBlocks().contains(blockId);
            }
            boolean disabled = disabledStatic || disabledRuntime;
            this.mode2BtnBanGen.setMessage(Component.translatable(disabled
                    ? "gui.visualcrafting.mode2.ban_gen.done"
                    : "gui.visualcrafting.mode2.ban_gen"));
        }
    }

    /** 读取被禁矿物短名集合（2 秒节流缓存，避免每帧读盘） */
    private Set<String> getDisabledOresCached() {
        long now = System.currentTimeMillis();
        if (this.mode2DisabledOresCache == null || now - this.mode2DisabledOresCacheAt > 2000L) {
            this.mode2DisabledOresCache = OreDisableRegistry.disabledShortNames(this.getDatapackDir());
            this.mode2DisabledOresCacheAt = now;
        }

        return this.mode2DisabledOresCache;
    }

    private void selectDimBiome(int dimIdx, int biomeIdx) {
        if (dimIdx >= 0 && dimIdx < this.mode2Dimensions.size() && biomeIdx >= 0 && biomeIdx < this.mode2Biomes.size()) {
            this.mode2DimIdx = dimIdx;
            this.syncBiomesFromDim();
            this.mode2BiomeIdx = biomeIdx;
            if (this.mode2Dropdown != null) {
                this.mode2Dropdown.collapse();
                int index = dimIdx * this.mode2Biomes.size() + biomeIdx;
                this.mode2Dropdown.setSelected(index);
            }

            this.saveMode2Config();
        }
    }

    /**
     * 群系列表首位的「All」选项（索引 0，表示当前维度分类下的全部群系）：
     * 点击 All 时仅保留 All；点击具体群系时若已选 All 则自动移除 All，保留具体群系。
     */
    private void onMode2BiomeAllClicked(int clickedIdx) {
        int allIdx = 0;
        if (clickedIdx == allIdx) {
            this.mode2BiomeSelectedIndices.clear();
            this.mode2BiomeSelectedIndices.add(allIdx);
        } else {
            this.mode2BiomeSelectedIndices = new LinkedHashSet<Integer>(this.mode2BiomeDropdown.getSelectedIndices());
            if (this.mode2BiomeSelectedIndices.remove(allIdx) && this.mode2BiomeSelectedIndices.isEmpty()) {
                this.mode2BiomeSelectedIndices.add(clickedIdx);
            }
        }

        this.mode2BiomeDropdown.setSelectedIndices(this.mode2BiomeSelectedIndices);
        this.saveMode2Config();
    }

    private void onMode2MineralGen(Button button) {
        String itemBlockId = this.getItemBlockId(this.menu.slots.get(0).getItem());
        if (itemBlockId.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_slot_empty").getString());
        } else {
            this.buildAndWriteOreGenFiles(itemBlockId);
            String oreName = itemBlockId.contains(":") ? itemBlockId.substring(itemBlockId.indexOf(58) + 1) : itemBlockId;
            if (OreDisableRegistry.isDisabled(this.getDatapackDir(), oreName)) {
                // 已禁用矿物的再生成：MergeManager 合并时仍会强制数量 0，禁用不解除
                this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_disabled_note", oreName).getString()
                        + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());
            } else {
                this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_created").getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());
            }

            this.updateMode2ButtonLabels();
        }
    }

    private void onMode2BanGen(Button button) {
        String itemBlockId = this.getItemBlockId(this.menu.slots.get(0).getItem());
        if (itemBlockId.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_slot_empty").getString());
        } else {
            // 已在运行时 Block 级禁用名单 → 对称解封；否则走禁用流程（静态数据包通道 + 运行时兜底）
            Block targetBlock = this.resolveBlockFromItemId(itemBlockId);
            ResourceLocation blockId = targetBlock != null ? BuiltInRegistries.BLOCK.getKey(targetBlock) : null;
            if (blockId != null && ModMessages.getCachedDisabledBlocks().contains(blockId)) {
                this.sendRuntimeBlockDisable(targetBlock, false);
            } else {
                this.disableOreGeneration();
            }
            this.updateMode2ButtonLabels();
            this.updateMode2ButtonStates();
        }
    }

    /**
     * 运行时 Block 级禁用/解封：发送 DisableBlockPacket，由服务端 BlockDisableRegistry
     * 维护名单并广播全量（收到广播即视为服务端已采纳，回执与广播合一）。
     * 服务端采纳后，WorldGenRegionBlockDisableMixin 将命中方块的生成写入替换为空气。
     */
    private void sendRuntimeBlockDisable(Block targetBlock, boolean disable) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(targetBlock);
        if (blockId == null) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_disable_not_block").getString());
            return;
        }
        if (disable && BlockDisableRegistry.isProtected(targetBlock)) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.block_disable_protected", blockId.toString()).getString());
            return;
        }
        PacketDistributor.sendToServer(new DisableBlockPacket(this.menu.blockPos, blockId, disable), new CustomPacketPayload[0]);
        this.showStatus(Component.translatable(disable
                ? "gui.visualcrafting.status.block_disable_runtime"
                : "gui.visualcrafting.status.block_unban_runtime", blockId.toString()).getString());
    }

    /** 客户端收到运行时禁用名单全量后回调：刷新模式2 按钮显示。 */
    public void applyDisabledBlocks() {
        this.updateMode2ButtonLabels();
        this.updateMode2ButtonStates();
    }

    private void onMode2Config(Button button) {
        this.saveMode2Config();
        Path path = this.getDatapackDataDir().toPath().resolve("worldgen");
        try {
            Files.createDirectories(path, new FileAttribute[0]);
        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Failed to create datapack data directories", null);
        }

        Util.getPlatform().openFile(path.toFile());
        String itemBlockId = this.getItemBlockId(this.menu.slots.get(0).getItem());
        if (!itemBlockId.isEmpty()) {
            this.buildAndWriteOreGenFiles(itemBlockId);
            this.showStatus(Component.translatable("gui.visualcrafting.status.config_saved").getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());
            this.updateMode2ButtonLabels();
        } else {
            this.showStatus(Component.translatable("gui.visualcrafting.status.config_saved_no_ore").getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());
        }
    }

    public void updateMode2ButtonStates() {
        if (this.mode2BtnMineralGen != null) {
            ItemStack itemStack = this.menu.slots.get(0).getItem();
            boolean hasItem = !itemStack.isEmpty();
            this.mode2BtnMineralGen.active = hasItem;
            this.mode2BtnBanGen.active = hasItem;
        }
    }

    String getItemBlockId(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return "";
        }

        try {
            return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        }

        catch (Exception e) {
            return "";
        }
    }

    public File getDatapackDir() {
        File file = null;
        try {
            if (this.minecraft != null) {
                if (this.minecraft.getSingleplayerServer() != null) {
                    file = this.minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toFile();
                    file = new File(file, "datapacks/visualcrafting");
                } else if (this.minecraft.getCurrentServer() != null) {
                    file = this.minecraft.gameDirectory.toPath().resolve("datapacks/visualcrafting").toFile();
                    this.showStatus(Component.translatable("gui.visualcrafting.status.singleplayer_note").getString());
                } else {
                    file = this.minecraft.gameDirectory.toPath().resolve("datapacks/visualcrafting").toFile();
                }

            }

        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Failed to get world path for datapack dir, falling back to game directory", null);
        }

        if (file == null) {
            file = this.minecraft.gameDirectory.toPath().resolve("datapacks/visualcrafting").toFile();
        }

        return file;
    }

    public File getDatapackDataDir() {
        return new File(this.getDatapackDir(), "data/visualcrafting");
    }

    /** 当前玩家 UUID（客户端本地玩家），单机/局域网模式可用 */

    private String currentPlayerId() {
        if (this.minecraft != null && this.minecraft.player != null) {
            return this.minecraft.player.getUUID().toString();
        }

        return "local";
    }

    /** 单机/局域网世界根目录下的 visualcrafting/pending 暂存目录 */

    private File getPendingDir() {
        if (this.minecraft != null && this.minecraft.getSingleplayerServer() != null) {
            return this.minecraft.getSingleplayerServer()
                    .getWorldPath(LevelResource.ROOT).resolve("visualcrafting").resolve("pending").toFile();
        }

        return new File(this.getDatapackDir().getParentFile().getParentFile(),
                "visualcrafting/pending");
    }

    /**
     * 将 GUI 生成的内容写入 pending 暂存目录，由 MergeManager 在启动/reload 时按类型合并输出。
     * 文件名规则：{玩家UUID}_{类型}_{recipeId脱敏}.json
     */
    private void writePending(String type, String recipeId, JsonObject content) {
        try {
            File pendingDir = this.getPendingDir();
            pendingDir.mkdirs();
            String safeRecipe = recipeId.replaceAll("[^A-Za-z0-9_/.-]", "_").replace('/', '_').replace('.', '_');
            String fileName = this.currentPlayerId() + "_" + type + "_" + safeRecipe + ".json";
            JsonObject root = new JsonObject();
            root.addProperty("player", this.currentPlayerId());
            root.addProperty("type", type);
            root.addProperty("recipeId", recipeId);
            root.addProperty("timestamp", System.currentTimeMillis());
            root.add("content", content);
            Files.writeString(new File(pendingDir, fileName).toPath(),
                    new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Failed to write pending for " + recipeId, e);
        }
    }

    public void ensureDatapackExists() {
        File file = this.getDatapackDir();
        file.mkdirs();
        // pack.mcmeta 的 pack_format 需与当前 Minecraft 版本一致（1.21/1.21.1 = 48）；
        // 已存在但格式号不符（如历史遗留的 57）时一并修正，避免数据包被判为不兼容。
        OreDisableRegistry.ensurePackMcmeta(file);

        File f_vt = new File(file, "data/visualcrafting/villager_trade");
        if (!f_vt.exists()) {
            f_vt.mkdirs();
        }
    }

    public void buildAndWriteOreGenFiles(String mineralItemId) {
        if (this.mode2BiomeDropdown != null) {
            this.mode2BiomeSelectedIndices = new LinkedHashSet<Integer>(this.mode2BiomeDropdown.getSelectedIndices());
        }

        String byproductItemId = this.getItemBlockId(this.menu.slots.get(1).getItem());
        List<String> list = this.gatherSelectedBiomeIds();
        String mineralName = mineralItemId.contains(":") ? mineralItemId.substring(mineralItemId.indexOf(58) + 1) : mineralItemId;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            int veinSize = Math.max(1, (int)((double)this.mode2MineralPct * 0.15));
            String mineralFeatureId = "visualcrafting:visualcrafting_ore_mineral_" + mineralName;
            JsonObject mineralConfigured = this.buildConfiguredFeature(gson, mineralFeatureId, mineralItemId, veinSize);
            this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/configured_feature/visualcrafting_ore_mineral_" + mineralName, mineralConfigured);
            JsonObject byproductConfigured = null;
            String byproductFeatureId = "";
            if (!byproductItemId.isEmpty()) {
                int byproductVein = Math.max(1, (int)((double)this.mode2ByproductPct * 0.08));
                byproductFeatureId = "visualcrafting:visualcrafting_ore_byproduct_" + mineralName;
                byproductConfigured = this.buildConfiguredFeature(gson, (String)byproductFeatureId, byproductItemId, byproductVein);
                this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/configured_feature/visualcrafting_ore_byproduct_" + mineralName, byproductConfigured);
            }

            JsonObject mineralPlaced = this.buildPlacedFeature(gson, mineralFeatureId, this.mode2MineralCountMin, this.mode2MineralCountMax, this.mode2MinY, this.mode2MaxY, "visualcrafting_ore_mineral_" + mineralName);
            this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/placed_feature/visualcrafting_ore_mineral_" + mineralName, mineralPlaced);
            if (!byproductItemId.isEmpty()) {
                JsonObject byproductPlaced = this.buildPlacedFeature(gson, (String)byproductFeatureId, this.mode2ByproductCountMin, this.mode2ByproductCountMax, this.mode2MinY, this.mode2MaxY, "visualcrafting_ore_byproduct_" + mineralName);
                this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/placed_feature/visualcrafting_ore_byproduct_" + mineralName, byproductPlaced);
            }

            JsonObject biomeModifier = this.buildBiomeModifier(gson, list, mineralFeatureId, (String)byproductFeatureId);
            this.writePending(MergeManager.TYPE_WORLDGEN, "neoforge/biome_modifier/add_visualcrafting_ore", biomeModifier);
            JsonObject biomeModifierTag = this.buildBiomeModifierTag();
            this.writePending(MergeManager.TYPE_WORLDGEN, "neoforge/tags/worldgen/biome_modifier/visualcrafting_ore", biomeModifierTag);
        }

        catch (Exception e) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.generate_failed", e.getMessage()).getString());
        }
    }

    List<String> gatherSelectedBiomeIds() {
        ArrayList<String> arrayList = new ArrayList<String>();
        int allIdx = 0;
        if (this.mode2BiomeSelectedIndices.contains(allIdx)) {
            // 「All」已勾选：返回当前维度分类下完整群系集合（维度 All 即全量 allBiomes）
            for (ResourceLocation biome : this.mode2Biomes) {
                arrayList.add(biome.toString());
            }
        } else if (!this.mode2Biomes.isEmpty()) {
            for (int n : this.mode2BiomeSelectedIndices) {
                if (n < 1 || n > this.mode2Biomes.size()) continue;
                arrayList.add(this.mode2Biomes.get(n - 1).toString());
            }

        }

        if (arrayList.isEmpty()) {
            arrayList.add("minecraft:plains");
        }

        return arrayList;
    }

    public JsonObject buildConfiguredFeature(Gson gson, String modid, String blockId, int veinSize) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "minecraft:ore");
        JsonObject jsonObject2 = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject3 = new JsonObject();
        JsonObject jsonObject4 = new JsonObject();
        jsonObject4.addProperty("predicate_type", "minecraft:tag_match");
        jsonObject4.addProperty("tag", "minecraft:stone_ore_replaceables");
        jsonObject3.add("target", jsonObject4);
        JsonObject jsonObject5 = new JsonObject();
        jsonObject5.addProperty("Name", blockId);
        jsonObject3.add("state", jsonObject5);
        jsonArray.add(jsonObject3);
        jsonObject2.add("targets", jsonArray);
        jsonObject2.addProperty("size", veinSize);
        jsonObject2.addProperty("discard_chance_on_air_exposure", 0.5);
        jsonObject.add("config", jsonObject2);
        return jsonObject;
    }

    /**
     * 补全资源引用命名空间：裸名（不含冒号）一律视为本模组资源，补上 visualcrafting: 前缀。
     * 主要用于 placed_feature 的 feature 字段，避免被 Minecraft 解析为默认命名空间 minecraft:，
     * 进而导致 "Unbound values in registry" 世界加载失败。
     */
    public static String qualifyResourceId(String resourceId) {
        if (resourceId == null || resourceId.isEmpty() || resourceId.indexOf(58) >= 0) {
            return resourceId;
        }

        return "visualcrafting:" + resourceId;
    }

    public JsonObject buildPlacedFeature(Gson gson, String modid, int veinSize, int veinsPerChunk, int minY, int maxY, String featureId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("feature", qualifyResourceId(featureId));
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("type", "minecraft:count");
        JsonObject jsonObject3 = new JsonObject();
        jsonObject3.addProperty("type", "minecraft:uniform");
        jsonObject3.addProperty("min_inclusive", veinSize);
        jsonObject3.addProperty("max_inclusive", veinsPerChunk);
        jsonObject2.add("count", jsonObject3);
        jsonArray.add(jsonObject2);
        JsonObject jsonObject4 = new JsonObject();
        jsonObject4.addProperty("type", "minecraft:in_square");
        jsonArray.add(jsonObject4);
        JsonObject jsonObject5 = new JsonObject();
        jsonObject5.addProperty("type", "minecraft:height_range");
        JsonObject jsonObject6 = new JsonObject();
        jsonObject6.addProperty("type", "minecraft:trapezoid");
        JsonObject jsonObject7 = new JsonObject();
        jsonObject7.addProperty("absolute", this.mode2MinY);
        jsonObject6.add("min_inclusive", jsonObject7);
        JsonObject jsonObject8 = new JsonObject();
        jsonObject8.addProperty("absolute", this.mode2MaxY);
        jsonObject6.add("max_inclusive", jsonObject8);
        jsonObject5.add("height", jsonObject6);
        jsonArray.add(jsonObject5);
        JsonObject jsonObject9 = new JsonObject();
        jsonObject9.addProperty("type", "minecraft:biome");
        jsonArray.add(jsonObject9);
        jsonObject.add("placement", jsonArray);
        return jsonObject;
    }

    public JsonObject buildBiomeModifier(Gson gson, List<String> list, String mineralFeatureId, String byproductFeatureId) {
        JsonArray jsonArray;
        JsonObject jsonObject;
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        File file3 = new File(this.getDatapackDataDir(), "neoforge/biome_modifier/add_visualcrafting_ore.json");
        if (file3.exists()) {
            try {
                jsonObject = (JsonObject)JsonParser.parseString(Files.readString(file3.toPath(), StandardCharsets.UTF_8));
                if (jsonObject.has("features") && jsonObject.get("features").isJsonArray()) {
                    for (JsonElement element : jsonObject.getAsJsonArray("features")) {
                        linkedHashSet.add(element.getAsString());
                    }

                }

            }

            catch (Exception e) {
                VisualCraftingScreen.logWarn("Corrupted biome_modifier JSON, starting fresh", e);
            }

        }

        linkedHashSet.add(mineralFeatureId);
        if (!byproductFeatureId.isEmpty()) {
            linkedHashSet.add(byproductFeatureId);
        }

        jsonObject = new JsonObject();
        jsonObject.addProperty("type", "neoforge:add_features");
        if (list.size() > 1) {
            jsonArray = new JsonArray();
            for (String idText : list) {
                jsonArray.add(idText);
            }

            jsonObject.add("biomes", jsonArray);
        } else {
            jsonObject.addProperty("biomes", list.get(0));
        }

        jsonArray = new JsonArray();
        for (String featureId : linkedHashSet) {
            jsonArray.add(featureId);
        }

        jsonObject.add("features", jsonArray);
        jsonObject.addProperty("step", "underground_ores");
        return jsonObject;
    }

    public JsonObject buildBiomeModifierTag() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("replace", Boolean.valueOf(false));
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("visualcrafting:add_visualcrafting_ore");
        jsonObject.add("values", jsonArray);
        return jsonObject;
    }

    /**
     * 泛指任何单方块的禁用入口：反查注册表中引用了目标方块的所有 placed_feature，分两条通道禁用：
     * <ol>
     *   <li>本模组自产的 placed_feature（visualcrafting_ore_mineral_* / byproduct_*）→ 数据包覆盖 count=0；</li>
     *   <li>其他来源（原版/其他模组）的 placed_feature → 生成 {@code neoforge:remove_features} biome modifier，
     *       按三维度群系 tag 从对应生成阶段整体移除，非侵入、可叠加；</li>
     * </ol>
     * 两条通道均登记到禁用清单，MergeManager 写出时强制保持（count=0 与 features 列表），禁用不复发。
     * <p>不走 placed_feature 的生成（噪音岩层 surface rule、结构、命令/脚本放置）反查不到，明确提示无法禁用。</p>
     */
    public void disableOreGeneration() {
        if (this.mode2BiomeDropdown != null) {
            this.mode2BiomeSelectedIndices = new LinkedHashSet<Integer>(this.mode2BiomeDropdown.getSelectedIndices());
        }

        File datapackRoot = this.getDatapackDir();
        File dataDir = this.getDatapackDataDir();
        String itemId = this.getItemBlockId(this.menu.slots.get(0).getItem());
        if (itemId.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_slot_empty").getString());
            return;
        }

        String itemName = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;

        // ---- 禁用前检测 ----
        // 检测 1：已在禁用清单中（幂等提示，不重复写盘）
        if (OreDisableRegistry.isDisabled(datapackRoot, itemName)) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_disable_already", itemName).getString());
            return;
        }

        // 检测 2：物品须能解析为方块
        Block targetBlock = this.resolveBlockFromItemId(itemId);
        if (targetBlock == null || targetBlock == Blocks.AIR) {
            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_disable_not_block", itemId).getString());
            return;
        }

        // 检测 3：反查注册表，确认存在引用该方块的 placed_feature
        Map<String, Boolean> found = this.findPlacedFeaturesForBlock(targetBlock);
        if (found.isEmpty()) {
            // 反查不到（surface rule / 结构 / 命令脚本放置等不走 placed_feature 的生成）
            // → 运行时通道兜底：由 WorldGenRegionBlockDisableMixin 将生成写入替换为空气
            this.sendRuntimeBlockDisable(targetBlock, true);
            return;
        }

        this.ensureDatapackExists();

        // ---- 反查结果分类：本模组自产 vs 外部来源 ----
        List<String> ownPlaced = new ArrayList<String>();
        List<String> externalPlaced = new ArrayList<String>();
        boolean anyOreConfig = false;
        for (Map.Entry<String, Boolean> hit : found.entrySet()) {
            String placedIdFull = hit.getKey();
            String shortId = placedIdFull.contains(":") ? placedIdFull.substring(placedIdFull.indexOf(':') + 1) : placedIdFull;
            boolean own = placedIdFull.startsWith(OreDisableRegistry.MOD_NAMESPACE)
                    && (shortId.startsWith(OreDisableRegistry.MINERAL_PREFIX)
                    || shortId.startsWith(OreDisableRegistry.BYPRODUCT_PREFIX));
            (own ? ownPlaced : externalPlaced).add(placedIdFull);
            if (Boolean.TRUE.equals(hit.getValue())) {
                anyOreConfig = true;
            }
        }

        int written = 0;
        boolean overridesValid = true;

        // ---- 通道 1：本模组 placed_feature → count=0 覆盖（保留原结构） ----
        for (String placedIdFull : ownPlaced) {
            String placedId = placedIdFull.contains(":") ? placedIdFull.substring(placedIdFull.indexOf(':') + 1) : placedIdFull;
            JsonObject override = null;
            File existing = new File(dataDir, OreDisableRegistry.PLACED_FEATURE_DIR + placedId + ".json");
            if (existing.exists()) {
                try {
                    JsonElement element = JsonParser.parseString(Files.readString(existing.toPath(), StandardCharsets.UTF_8));
                    if (element != null && element.isJsonObject()) {
                        override = element.getAsJsonObject();
                        OreDisableRegistry.forceZeroCount(override);
                    }
                } catch (Exception e) {
                    VisualCraftingScreen.logWarn("Corrupted placed_feature, fallback to fresh override: " + placedId, e);
                }
            }

            if (override == null) {
                override = OreDisableRegistry.buildDisabledPlacedFeature();
            }

            overridesValid = overridesValid && OreDisableRegistry.verifyZeroCount(override)
                    && OreDisableRegistry.hasQualifiedFeature(override);
            this.writePending(MergeManager.TYPE_WORLDGEN, OreDisableRegistry.placedRecipeId(placedId), override);
            written++;
        }

        // ---- 通道 2：外部 placed_feature → remove_features biome modifier + tag ----
        if (!externalPlaced.isEmpty()) {
            String step = anyOreConfig
                    ? OreDisableRegistry.STEP_UNDERGROUND_ORES
                    : OreDisableRegistry.STEP_VEGETAL_DECORATION;
            JsonObject removeModifier = OreDisableRegistry.buildRemoveFeaturesModifier(externalPlaced, step);
            this.writePending(MergeManager.TYPE_WORLDGEN, OreDisableRegistry.removeModifierRecipeId(itemName), removeModifier);

            JsonObject tag = this.mergeRemoveModifierTag(dataDir, OreDisableRegistry.removeModifierId(itemName));
            this.writePending(MergeManager.TYPE_WORLDGEN, OreDisableRegistry.removeModifierTagRecipeId(), tag);
            written++;
        }

        this.archiveLegacyDisableArtifacts(itemName);
        this.mode2DisabledOresCache = null;

        List<String> allPlaced = new ArrayList<String>();
        allPlaced.addAll(ownPlaced);
        allPlaced.addAll(externalPlaced);
        boolean registered = OreDisableRegistry.mark(datapackRoot, itemName, itemId, allPlaced);
        boolean listed = registered && OreDisableRegistry.isDisabled(datapackRoot, itemName);
        if (overridesValid && listed) {
            StringBuilder status = new StringBuilder(Component.translatable(
                    "gui.visualcrafting.status.mineral_disabled", itemName, Integer.valueOf(written)).getString());
            if (!externalPlaced.isEmpty()) {
                status.append(Component.translatable(
                        "gui.visualcrafting.status.mineral_disable_external_count", Integer.valueOf(externalPlaced.size())).getString());
            }
            status.append(" | ").append(Component.translatable("gui.visualcrafting.status.reload_hint").getString());
            this.showStatus(status.toString());
            LOGGER.info("[VisualCrafting] 已禁用方块 {}：本模组 placed_feature 置 0 共 {} 个，外部 remove_features 共 {} 个，清单已登记并通过校验",
                    itemName, Integer.valueOf(ownPlaced.size()), Integer.valueOf(externalPlaced.size()));
        } else {
            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_disable_failed", itemName).getString());
            LOGGER.warn("[VisualCrafting] 禁用方块 {} 校验失败：overridesValid={}, registered={}, listed={}",
                    itemName, Boolean.valueOf(overridesValid), Boolean.valueOf(registered), Boolean.valueOf(listed));
        }
    }

    /**
     * 把槽位物品 id 解析为方块：优先按注册表 BLOCK 反查，失败再尝试 BlockItem。
     * 非方块物品（如矿物粉、锭）返回 null。
     */
    private Block resolveBlockFromItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }

        try {
            ResourceLocation id = ResourceLocation.parse(itemId);
            Block block = BuiltInRegistries.BLOCK.get(ResourceKey.create(Registries.BLOCK, id));
            if (block != null && block != Blocks.AIR) {
                return block;
            }
            Item item = BuiltInRegistries.ITEM.get(ResourceKey.create(Registries.ITEM, id));
            if (item instanceof BlockItem) {
                return ((BlockItem) item).getBlock();
            }
        } catch (Exception e) {
            VisualCraftingScreen.logWarn("Failed to resolve block from item id: " + itemId, e);
        }

        return null;
    }

    /**
     * 遍历注册表反查：返回所有引用了目标方块的 placed_feature id → 是否为 Ore 配置。
     * 快速路径识别 {@code OreConfiguration}（targets 里的 BlockState），其余走通用反射扫描
     * config 内 Block / BlockState / Holder / BlockStateProvider / 集合 / 可选 / 数组 / Map 引用。
     */
    private Map<String, Boolean> findPlacedFeaturesForBlock(Block target) {
        Map<String, Boolean> hits = new LinkedHashMap<String, Boolean>();
        try {
            Registry<?> rawRegistry = BuiltInRegistries.REGISTRY.get(Registries.PLACED_FEATURE.location());
            if (!(rawRegistry instanceof Registry)) {
                return hits;
            }
            Registry<PlacedFeature> placedRegistry = (Registry<PlacedFeature>) rawRegistry;
            if (placedRegistry == null) {
                return hits;
            }
            for (Map.Entry<ResourceKey<PlacedFeature>, PlacedFeature> entry : placedRegistry.entrySet()) {
                PlacedFeature placed = entry.getValue();
                if (placed == null) {
                    continue;
                }

                Holder<ConfiguredFeature<?, ?>> holder = placed.feature();
                if (holder == null || !holder.isBound()) {
                    continue;
                }

                ConfiguredFeature<?, ?> configured = holder.value();
                if (configured == null) {
                    continue;
                }

                Boolean isOre = this.referencesBlock(configured.config(), target);
                if (isOre != null) {
                    hits.put(entry.getKey().location().toString(), isOre);
                }
            }
        } catch (Exception e) {
            VisualCraftingScreen.logWarn("Failed to scan placed features for block " + target, e);
        }

        return hits;
    }

    /**
     * 判断 feature 配置是否引用目标方块。返回 null 表示未命中；
     * TRUE 表示 Ore 配置命中，FALSE 表示非 Ore 配置命中（用于选择 remove_features 的生成阶段）。
     * 统一走通用反射扫描（OreConfiguration 的 targets/state 等字段均可被递归覆盖），
     * 避免依赖 record accessor 的私有字段布局。
     */
    private Boolean referencesBlock(FeatureConfiguration config, Block target) {
        boolean isOre = config instanceof OreConfiguration;
        try {
            for (Field field : config.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(config);
                if (this.containsBlockRef(value, target)) {
                    return Boolean.valueOf(isOre);
                }
            }
        } catch (Exception e) {
            // 反射失败不影响其他字段/其他 feature 的判定
        }

        return null;
    }

    /** 递归检查对象引用链中是否包含目标方块 */
    private boolean containsBlockRef(Object value, Block target) {
        if (value == null) {
            return false;
        }

        if (value instanceof Block) {
            return value == target;
        }

        if (value instanceof BlockState) {
            return ((BlockState) value).getBlock() == target;
        }

        if (value instanceof Holder) {
            try {
                Object held = ((Holder<?>) value).value();
                return held instanceof Block && held == target;
            } catch (Exception e) {
                return false;
            }
        }

        if (value instanceof BlockStateProvider) {
            try {
                return ((BlockStateProvider) value).getState(RandomSource.create(), BlockPos.ZERO).getBlock() == target;
            } catch (Exception e) {
                return false;
            }
        }

        if (value instanceof Optional) {
            Optional<?> optional = (Optional<?>) value;
            return optional.isPresent() && this.containsBlockRef(optional.get(), target);
        }

        if (value instanceof Iterable) {
            for (Object element : (Iterable<?>) value) {
                if (this.containsBlockRef(element, target)) {
                    return true;
                }
            }
            return false;
        }

        if (value instanceof Map) {
            for (Object element : ((Map<?, ?>) value).values()) {
                if (this.containsBlockRef(element, target)) {
                    return true;
                }
            }
            return false;
        }

        if (value.getClass().isArray()) {
            for (Object element : (Object[]) value) {
                if (this.containsBlockRef(element, target)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** 读取既有 remove modifier tag 并并入新 modifier id（replace=false 可叠加） */
    private JsonObject mergeRemoveModifierTag(File dataDir, String modifierId) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", Boolean.valueOf(false));
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        File file = new File(dataDir, "neoforge/tags/worldgen/biome_modifier/"
                + OreDisableRegistry.REMOVE_MODIFIER_TAG + ".json");
        if (file.exists()) {
            try {
                JsonElement element = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
                if (element != null && element.isJsonObject()) {
                    JsonElement existingValues = element.getAsJsonObject().get("values");
                    if (existingValues != null && existingValues.isJsonArray()) {
                        for (JsonElement value : existingValues.getAsJsonArray()) {
                            if (value != null && value.isJsonPrimitive()) {
                                values.add(value.getAsString());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                VisualCraftingScreen.logWarn("Corrupted remove modifier tag, starting fresh", e);
            }
        }

        values.add(modifierId.contains(":") ? modifierId : OreDisableRegistry.MOD_NAMESPACE + modifierId);
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        tag.add("values", array);
        return tag;
    }

    /**
     * 归档历史禁用实现的遗留产物（旧实现改写 configured_feature 目标方块为石头，并生成本模组 remove_* 修饰符文件）。
     * 这些文件对新的「覆盖 placed_feature 数量置 0」方案无用且可能干扰，统一移入世界目录
     * visualcrafting/legacy_disabled_artifacts 备份，不做物理删除。
     */
    private void archiveLegacyDisableArtifacts(String itemName) {
        File backupDir = new File(this.getPendingDir().getParentFile(), "legacy_disabled_artifacts");
        String[] candidates = {
            new File(this.getDatapackDataDir(), OreDisableRegistry.BIOME_MODIFIER_DIR + "remove_visualcrafting_ore_" + itemName + ".json").getAbsolutePath(),
            new File(this.getDatapackDir(), OreDisableRegistry.BIOME_MODIFIER_DIR + "remove_visualcrafting_ore_" + itemName + ".json").getAbsolutePath(),
            new File(this.getDatapackDir(), "remove_visualcrafting_ore_" + itemName + ".json").getAbsolutePath()
        };
        for (String path : candidates) {
            File legacy = new File(path);
            if (!legacy.isFile()) {
                continue;
            }

            try {
                backupDir.mkdirs();
                Files.move(legacy.toPath(), new File(backupDir, legacy.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("[VisualCrafting] 已归档旧禁用实现残留文件: {} -> {}", path, backupDir.getAbsolutePath());
            } catch (Exception e) {
                VisualCraftingScreen.logWarn("Failed to archive legacy disable artifact: " + path, e);
            }
        }
    }

    public void saveMode2Config() {
        if (this.mode2BiomeDropdown != null) {
            this.mode2BiomeSelectedIndices = new LinkedHashSet<Integer>(this.mode2BiomeDropdown.getSelectedIndices());
        }

        try {
            File file = this.getDatapackDataDir();
            file.mkdirs();
            File file2 = new File(file, "visualcrafting_config.json");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{\n");
            if (!this.mode2Dimensions.isEmpty()) {
                if (this.mode2DimIdx > 0 && this.mode2DimIdx <= this.mode2Dimensions.size()) {
                    stringBuilder.append("  \"dimension\": \"").append(this.mode2Dimensions.get(this.mode2DimIdx - 1)).append("\",\n");
                } else {
                    stringBuilder.append("  \"dimension\": \"__all__\",\n");
                }

            }

            if (!this.mode2Biomes.isEmpty()) {
                stringBuilder.append("  \"biomes\": [");
                boolean isFirst = true;
                int allIdx = 0;
                if (this.mode2BiomeSelectedIndices.contains(allIdx)) {
                    stringBuilder.append("\"__all__\"");
                    isFirst = false;
                }

                for (int n : this.mode2BiomeSelectedIndices) {
                    if (n < 1 || n > this.mode2Biomes.size()) continue;
                    if (!isFirst) {
                        stringBuilder.append(", ");
                    }

                    stringBuilder.append("\"").append(this.mode2Biomes.get(n - 1)).append("\"");
                    isFirst = false;
                }

                stringBuilder.append("],\n");
            }

            stringBuilder.append("  \"percentage\": ").append(this.mode2Pct).append(",\n");
            stringBuilder.append("  \"mineralPercentage\": ").append(this.mode2MineralPct).append(",\n");
            stringBuilder.append("  \"byproductPercentage\": ").append(this.mode2ByproductPct).append(",\n");
            stringBuilder.append("  \"mineralCountMin\": ").append(this.mode2MineralCountMin).append(",\n");
            stringBuilder.append("  \"mineralCountMax\": ").append(this.mode2MineralCountMax).append(",\n");
            stringBuilder.append("  \"byproductCountMin\": ").append(this.mode2ByproductCountMin).append(",\n");
            stringBuilder.append("  \"byproductCountMax\": ").append(this.mode2ByproductCountMax).append(",\n");
            stringBuilder.append("  \"minY\": ").append(this.mode2MinY).append(",\n");
            stringBuilder.append("  \"maxY\": ").append(this.mode2MaxY).append("\n");
            stringBuilder.append("}");
            Files.writeString(file2.toPath(), stringBuilder.toString(), StandardCharsets.UTF_8, new OpenOption[0]);
        }

        catch (Exception e) {
            LOGGER.warn("Failed to save mode2 config", e);
        }
    }

    public void showStatus(String message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(message), false);
        }
    }

    int slotAbsX(int slotIndex) {
        return this.leftPos + this.menu.getSlot(slotIndex).x;
    }

    int slotAbsY(int slotIndex) {
        return this.topPos + this.menu.getSlot(slotIndex).y;
    }

    public int gridAbsX() {
        return this.leftPos + 59 + this.gridOffsetX;
    }

    public int gridAbsY() {
        return this.topPos + 20 + this.gridOffsetY;
    }

    public int getGridSlotX(int slotIndex) {
        // 直接读取真实 Slot 坐标，支持任意网格尺寸（3/5/7/9），不再使用 %3 的 3×3 专用算法
        return this.slotAbsX(slotIndex);
    }

    public int getGridSlotY(int slotIndex) {
        return this.slotAbsY(slotIndex);
    }

    public int chemAbsX() {
        return this.getChemSlotX();
    }

    public int chemAbsY() {
        return this.getChemSlotY();
    }

    public int getMode() {
        return this.mode;
    }

    int infInputAbsX() {
        return this.slotAbsX(0);
    }

    int infInputAbsY() {
        return this.slotAbsY(0);
    }

    int infOutAbsX() {
        return this.slotAbsX(81);
    }

    int infOutAbsY() {
        return this.slotAbsY(81);
    }

    public int outSlotAbsX() {
        return this.slotAbsX(81);
    }

    public int outSlotAbsY() {
        return this.slotAbsY(81);
    }

    public int inputSlotAbsX() {
        return this.slotAbsX(0);
    }

    public int inputSlotAbsY() {
        return this.slotAbsY(0);
    }

    public int infOutputSlotAbsX() {
        return this.infOutAbsX();
    }

    public int infOutputSlotAbsY() {
        return this.infOutAbsY();
    }

    private void switchMode(int newMode) {
        int oldMode = this.mode;
        int prevGridSize = this.getGridSize();
        int prevSlotCount = prevGridSize * prevGridSize;
        ItemStack[] itemStackArray = new ItemStack[prevSlotCount];
        for (int i = 0; i < prevSlotCount; ++i) {
            itemStackArray[i] = (this.menu.slots.get(i)).getItem().copy();
        }

        ItemStack itemStack = this.menu.slots.get(81).getItem().copy();
        PacketDistributor.sendToServer(new ModMessages.ModeUpdatePacket(this.menu.blockPos, newMode), new CustomPacketPayload[0]);
        this.markedContainer = ItemStack.EMPTY;
        this.readBEState();
        this.mode = newMode;
        this.tier = 0;
        this.loadOffsets();
        this.updateGuiSize();
        this.rebuildWidgets();
        int newSlotCount = this.getGridSize() * this.getGridSize();
        for (int i = 0; i < prevSlotCount && i < newSlotCount; ++i) {
            this.menu.slots.get(i).set(itemStackArray[i]);
        }

        this.menu.slots.get(81).set(itemStack);
        if (newMode == 2) {
            this.updateMode2ButtonStates();
        }
    }

    // ======================== 统一 Slot 布局体系 ========================

    /**
     * 依据当前 gridSize 重算 GUI 尺寸与锚点。
     * 在 init() / onTier() / switchMode() 切换后调用，消除跨模式/跨 tier 尺寸不一致。
     */
    private void updateGuiSize() {
        int gridSize = this.getGridSize();
        this.imageWidth = 260 + (gridSize - 3) * 18;
        this.imageHeight = gridSize * 18 + 161;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    /**
     * 槽线统一读取真实 Slot 坐标：Slot 本体与槽线同一坐标源，
     * 叠加 invLineOffsetX/Y（默认 -1）：MC 槽背景精灵自 slotAbsX-1 起绘制 18×18，
     * 描边须同步 -1 偏移，保证槽线中心与槽位背景中心重合。
     */
    private void renderSlotOutline(GuiGraphics guiGraphics, int slotIndex) {
        guiGraphics.renderOutline(this.slotAbsX(slotIndex) + this.invLineOffsetX, this.slotAbsY(slotIndex) + this.invLineOffsetY, 18, 18, -1);
    }

    private void setSlotPosition(int slotIndex, int x, int y) {
        VisualCraftingScreen.setSlotX(this.menu.slots.get(slotIndex), x);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(slotIndex), y);
    }

    private void hideSlot(int slotIndex) {
        this.setSlotPosition(slotIndex, -2000, -2000);
    }

    private void hideCraftingSlotsAndOutput() {
        for (int i = 0; i <= 81; ++i) {
            this.hideSlot(i);
        }
    }

    private void hideAllVisualSlots() {
        for (int i = 0; i < this.menu.slots.size(); ++i) {
            this.hideSlot(i);
        }
    }

    /**
     * 当前模式下的最终槽位布局。rebuildWidgets() 中必须在所有 initModeXWidgets()
     * 分支之后最后调用，确保统一布局覆盖 init 阶段写入的坐标。
     */
    private void layoutCurrentModeSlots() {
        if (this.mode == 0) {
            this.layoutCraftingSlots();
        } else if (this.mode == 1) {
            this.layoutInfusingSlots();
        } else if (this.mode == 2) {
            this.layoutMode2Slots();
        } else if (this.mode == 5) {
            this.layoutMode5Slots();
        } else if (this.mode == 6) {
            this.layoutMode6Slots();
        } else if (this.mode == 7) {
            this.layoutMode7Slots();
        } else if (this.mode == 8) {
            this.layoutMode8Slots();
        } else {
            // 非法模式兜底：隐藏合成区，仅保留玩家背包
            this.hideCraftingSlotsAndOutput();
        }
        this.layoutPlayerInventorySlots();
    }

    /**
     * 玩家背包 + 快捷栏（slot 82..117）。
     * mode0 因底部有 format/tier 按钮，背包基准沿用 hotbarBaseY + invSlotSlotOffset 的既有布局；
     * 其余模式统一从 imageHeight - 83 起排，与各 initModeXWidgets 现状一致。
     */
    private void layoutPlayerInventorySlots() {
        int invBaseY;
        int invOffsetX = 0;
        int invOffsetY = 0;
        if (this.mode == 0) {
            invBaseY = 13 + this.getGridSize() * 18 + 8;
            invOffsetX = this.invSlotSlotOffsetX;
            invOffsetY = this.invSlotSlotOffsetY;
        } else {
            invBaseY = this.imageHeight - 83;
        }
        for (int slotIdx = 82; slotIdx < this.menu.slots.size(); ++slotIdx) {
            int col = (slotIdx - 82) % 9;
            int row = (slotIdx - 82) / 9;
            this.setSlotPosition(slotIdx, 8 + col * 18 + invOffsetX, invBaseY + row * 18 + invOffsetY);
        }
    }

    /**
     * Mode0 合成网格 + 输出槽：在 menu.updateSlotPositions 基础上叠加各 Slot 偏移。
     */
    private void layoutCraftingSlots() {
        int gridSize = this.getGridSize();
        int activeSlots = gridSize * gridSize;
        for (int i = 0; i < activeSlots; ++i) {
            Slot slot = this.menu.slots.get(i);
            this.setSlotPosition(i, slot.x + this.gridSlotOffsetX, slot.y + this.gridSlotOffsetY);
        }
        for (int i = activeSlots; i < 81; ++i) {
            this.hideSlot(i);
        }
        Slot outSlot = this.menu.slots.get(81);
        this.setSlotPosition(81, outSlot.x + this.outSlotSlotOffsetX, outSlot.y + this.outSlotSlotOffsetY);
    }

    /**
     * Mode1 灌注：输入槽 0 与输出槽 81，其余合成槽隐藏。
     */
    private void layoutInfusingSlots() {
        this.hideCraftingSlotsAndOutput();
        this.setSlotPosition(0, 80 + this.infInputSlotSlotOffsetX, 35 + this.infInputSlotSlotOffsetY);
        this.setSlotPosition(81, 148 + this.infOutSlotSlotOffsetX, 45 + this.infOutSlotSlotOffsetY);
    }

    /**
     * Mode2 矿物：mineral 槽 0 (y=42) 与 byproduct 槽 1 (y=70)，其余隐藏。
     */
    private void layoutMode2Slots() {
        this.hideCraftingSlotsAndOutput();
        this.setSlotPosition(0, 71 + this.mode2OffsetX, 42);
        this.setSlotPosition(1, 71 + this.mode2OffsetX, 70);
    }

    /**
     * Mode5 食物：效果槽 0 (y=17) 与 1 (y=40)，其余隐藏。
     */
    private void layoutMode5Slots() {
        this.hideCraftingSlotsAndOutput();
        this.setSlotPosition(0, 94, 17);
        this.setSlotPosition(1, 94, 40);
    }

    /**
     * Mode6 命名：仅输入槽 0 (11,74)，其余隐藏。
     */
    private void layoutMode6Slots() {
        this.hideCraftingSlotsAndOutput();
        this.setSlotPosition(0, 11, 74);
    }

    /**
     * Mode7 属性：全部合成槽隐藏（物品经槽位交互另有用途时由 init 覆盖）。
     */
    private void layoutMode7Slots() {
        this.hideCraftingSlotsAndOutput();
    }

    /**
     * Mode8 附魔：仅输出槽 81，保持 x=100 左对齐关系不变。
     */
    private void layoutMode8Slots() {
        this.hideCraftingSlotsAndOutput();
        this.setSlotPosition(81, 100, 12);
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -1072689136);
        int tabWidth = 24;
        int tabHeight = 24;
        int tabGap = 3;
        int tabStartX = this.leftPos + 8;
        int tabStartY = this.topPos - 26;
        int craftIconX = tabStartX + 4;
        int craftIconY = tabStartY + 4;
        guiGraphics.blit(TAB_CRAFT, craftIconX, craftIconY, 0.0f, 0.0f, 16, 16, 16, 16);
        if (this.mode == 0) {
            guiGraphics.renderOutline(tabStartX, tabStartY, tabWidth, tabHeight, -256);
        }

        int infuseIconX = tabStartX + tabWidth + tabGap + 4;
        guiGraphics.blit(TAB_INFUSE, infuseIconX, craftIconY, 0.0f, 0.0f, 16, 16, 16, 16);
        if (this.mode == 1) {
            guiGraphics.renderOutline(tabStartX + tabWidth + tabGap, tabStartY, tabWidth, tabHeight, -256);
        }

        int oreIconX = tabStartX + (tabWidth + tabGap) * 2 + 4;
        guiGraphics.blit(TAB_ORE, oreIconX, craftIconY, 0.0f, 0.0f, 16, 16, 16, 16);
        if (this.mode == 2) {
            guiGraphics.renderOutline(tabStartX + (tabWidth + tabGap) * 2, tabStartY, tabWidth, tabHeight, -256);
        }

        int tabFoodIconX = tabStartX + (tabWidth + tabGap) * 3 + 4;
        guiGraphics.blit(TAB_FOOD, tabFoodIconX, craftIconY, 0.0f, 0.0f, 16, 16, 16, 16);
                if (this.mode == 5) {
            guiGraphics.renderOutline(tabStartX + (tabWidth + tabGap) * 3, tabStartY, tabWidth, tabHeight, -256);
        }

        int tabNameIconX = tabStartX + (tabWidth + tabGap) * 4 + 4;
        guiGraphics.renderItem(ICON_NAME, tabNameIconX, craftIconY);
        if (this.mode == 6) {
            guiGraphics.renderOutline(tabStartX + (tabWidth + tabGap) * 4, tabStartY, tabWidth, tabHeight, -256);
        }

        int tabCreateIconX = tabStartX + (tabWidth + tabGap) * 5 + 4;
        guiGraphics.renderItem(ICON_CREATE, tabCreateIconX, craftIconY);
        if (this.mode == 7) {
            guiGraphics.renderOutline(tabStartX + (tabWidth + tabGap) * 5, tabStartY, tabWidth, tabHeight, -256);
        }

        int tabEnhanceIconX = tabStartX + (tabWidth + tabGap) * 6 + 4;
        guiGraphics.renderItem(ICON_ENHANCE, tabEnhanceIconX, craftIconY);
        if (this.mode == 8) {
            guiGraphics.renderOutline(tabStartX + (tabWidth + tabGap) * 6, tabStartY, tabWidth, tabHeight, -256);
        }

        if (this.mode == 0) {
            int gridSize = this.getGridSize();
            int gridX = this.slotAbsX(0) + this.invLineOffsetX;
            int gridY = this.slotAbsY(0) + this.invLineOffsetY;
            guiGraphics.renderOutline(gridX, gridY, gridSize * 18, gridSize * 18, -1);
            for (int i = 0; i < gridSize; ++i) {
                for (int j = 0; j < gridSize; ++j) {
                    this.renderSlotOutline(guiGraphics, i * gridSize + j);
                }

            }

            this.renderSlotOutline(guiGraphics, 81);
            this.renderCraftList(guiGraphics, mouseX, mouseY);
        } else if (this.mode == 1) {
            this.renderInfusingExtras(guiGraphics);
            this.renderInfuseList(guiGraphics, mouseX, mouseY);
                } else if (this.mode == 5) {
            this.renderMode5Extras(guiGraphics, mouseX, mouseY);
        } else if (this.mode == 6) {
            this.renderNameExtras(guiGraphics, mouseX, mouseY);
        } else if (this.mode == 7) {
            this.renderMode7Extras(guiGraphics);
        } else if (this.mode == 8) {
            this.renderMode8Extras(guiGraphics, mouseX, mouseY);
        } else {
            this.renderMode2Extras(guiGraphics);
        }

        for (int slotIdx = 82; slotIdx < this.menu.slots.size(); ++slotIdx) {
            this.renderSlotOutline(guiGraphics, slotIdx);
        }

    }

    private void renderCraftList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listX = this.leftPos + this.imageWidth - 88 + this.recipesOffsetX;
        int listY = this.topPos + 13 + this.recipesOffsetY;
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.label.recipes_count", this.recipes.size()).getString(), listX, listY - 14, 0x404040, false);
        int itemsPerPage = 7;
        int totalPages = (int)Math.ceil((double)this.recipes.size() / (double)itemsPerPage);
        int startIdx = this.scrollOffset * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, this.recipes.size());
        for (int i = startIdx; i < endIdx; ++i) {
            int rowOffset = i - startIdx;
            int rowY = listY + rowOffset * 20;
            VisualCraftingBlockEntity.SavedRecipe savedRecipe = this.recipes.get(i);
            ResourceLocation resourceLocation = savedRecipe.shaped ? ICO_SHAPE : ICO_SHLESS;
            RenderSystem.setShaderTexture((int)0, resourceLocation);
            guiGraphics.blit(resourceLocation, listX + 2, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
            guiGraphics.renderItem(savedRecipe.result, listX + 22, rowY + 2);
            guiGraphics.drawString(this.font, savedRecipe.result.getHoverName().getString(), listX + 42, rowY + 5, savedRecipe.banned ? 0x808080 : 0x404040, false);
            if (mouseX < listX || mouseX > listX + 88 || mouseY < rowY || mouseY > rowY + 18) continue;
            guiGraphics.fill(listX, rowY, listX + 88, rowY + 18, 0x30FFFFFF);
        }

        if (totalPages > 1) {
            guiGraphics.drawString(this.font, this.scrollOffset + 1 + "/" + totalPages, listX + 30, listY + itemsPerPage * 20 + 2, 0x808080, false);
        }
    }

    private void renderInfuseList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listX = this.getInfuseRecipesX();
        int listY = this.getInfuseRecipesY();
        int itemsPerPage = 5;
        int totalPages = (int)Math.ceil((double)this.infusingRecipes.size() / (double)itemsPerPage);
        int startIdx = this.scrollOffset * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, this.infusingRecipes.size());
        for (int i = startIdx; i < endIdx; ++i) {
            int rowOffset = i - startIdx;
            int rowY = listY + rowOffset * 18;
            VisualCraftingBlockEntity.InfusingRecipe infusingRecipe = this.infusingRecipes.get(i);
            if (infusingRecipe.banned) {
                RenderSystem.setShaderTexture((int)0, ICO_DEL);
                guiGraphics.blit(ICO_DEL, listX + 2, rowY + 1, 0.0f, 0.0f, 16, 16, 16, 16);
                guiGraphics.drawString(this.font, "§m " + infusingRecipe.output.getHoverName().getString(), listX + 22, rowY + 4, 0x808080, false);
            } else {
                guiGraphics.renderItem(infusingRecipe.output, listX + 2, rowY + 1);
                guiGraphics.drawString(this.font, infusingRecipe.output.getHoverName().getString(), listX + 22, rowY + 4, 0x404040, false);
            }

            if (mouseX < listX || mouseX > listX + 88 || mouseY < rowY || mouseY > rowY + 18) continue;
            guiGraphics.fill(listX, rowY, listX + 88, rowY + 18, 0x30FFFFFF);
        }

        if (totalPages > 1) {
            guiGraphics.drawString(this.font, this.scrollOffset + 1 + "/" + totalPages, listX + 30, listY + itemsPerPage * 18 + 2, 0x808080, false);
        }
    }
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.mode == 6 && this.mode6PickerOpen) {
            if (this.mode6PickerClick(mouseX, mouseY, button)) {
                return true;
            }

            this.mode6ClosePicker();
        }

        int tabWidth = 24;
        int tabGap = 3;
        int tabStartX = this.leftPos + 8;
        int tabStartY = this.topPos - 26;
        if (mouseX >= (double)tabStartX && mouseX < (double)(tabStartX + tabWidth) && mouseY >= (double)tabStartY && mouseY < (double)(tabStartY + 24)) {
            if (this.mode != 0) {
                this.switchMode(0);
            }

            return true;
        }

        if (mouseX >= (double)(tabStartX + tabWidth + tabGap) && mouseX < (double)(tabStartX + tabWidth * 2 + tabGap) && mouseY >= (double)tabStartY && mouseY < (double)(tabStartY + 24)) {
            if (this.mode != 1) {
                this.switchMode(1);
            }

            return true;
        }

        if (mouseX >= (double)(tabStartX + (tabWidth + tabGap) * 2) && mouseX < (double)(tabStartX + (tabWidth + tabGap) * 2 + tabWidth) && mouseY >= (double)tabStartY && mouseY < (double)(tabStartY + 24)) {
            if (this.mode != 2) {
                this.switchMode(2);
            }

            return true;
        }
                if (mouseX >= (double)(tabStartX + (tabWidth + tabGap) * 3) && mouseX < (double)(tabStartX + (tabWidth + tabGap) * 3 + tabWidth) && mouseY >= (double)tabStartY && mouseY < (double)(tabStartY + 24)) {
            if (this.mode != 5) {
                        this.switchMode(5);
            }

            return true;
        }

        if (mouseX >= (double)(tabStartX + (tabWidth + tabGap) * 4) && mouseX < (double)(tabStartX + (tabWidth + tabGap) * 4 + tabWidth) && mouseY >= (double)tabStartY && mouseY < (double)(tabStartY + 24)) {
            if (this.mode != 6) {
                        this.switchMode(6);
            }

            return true;
        }

        if (mouseX >= (double)(tabStartX + (tabWidth + tabGap) * 5) && mouseX < (double)(tabStartX + (tabWidth + tabGap) * 5 + tabWidth) && mouseY >= (double)tabStartY && mouseY < (double)(tabStartY + 24)) {
            if (this.mode != 7) {
                        this.switchMode(7);
            }

            return true;
        }

        if (mouseX >= (double)(tabStartX + (tabWidth + tabGap) * 6) && mouseX < (double)(tabStartX + (tabWidth + tabGap) * 6 + tabWidth) && mouseY >= (double)tabStartY && mouseY < (double)(tabStartY + 24)) {
            if (this.mode != 8) {
                        this.switchMode(8);
            }

            return true;
        }

        if (this.mode == 6 && this.mode6SwatchClick(mouseX, mouseY)) {
            return true;
        }

        if (this.mode == 1 && button == 1) {
            int chemSlotX = this.getChemSlotX();
            int chemSlotY = this.getChemSlotY();
            if (mouseX >= (double)chemSlotX && mouseX < (double)(chemSlotX + 18) && mouseY >= (double)chemSlotY && mouseY < (double)(chemSlotY + 18)) {
                this.menu.chemSlotData = null;
                this.selectedChemical = ItemStack.EMPTY;
                return true;
            }

        }

        // 下拉框使用独立的点击层。
        // 当有下拉框展开时，本次点击只能由展开的下拉框处理，
        // 禁止事件继续传递给其他 Dropdown、EditBox、Button 或底层控件。
        if (this.handleDropdownClick(mouseX, mouseY, button)) {
            return true;
        }

        if (this.mode == 5 && button == 0) {
            int ml = this.leftPos;
            int mt = this.topPos;
            String infiniteLabel = Component.translatable("gui.visualcrafting.mode5.label.infinite").getString();
            int infiniteX = ml + 173;
            int infiniteW = 12 + this.font.width(infiniteLabel);
            if (mouseX >= (double)infiniteX && mouseX < (double)(infiniteX + infiniteW) && mouseY >= (double)(mt + 44) && mouseY < (double)(mt + 54)) {
                this.mode5DurationInfinite = !this.mode5DurationInfinite;
                if (this.mode5DurationEdit != null) {
                    this.mode5DurationEdit.setEditable(!this.mode5DurationInfinite);
                    this.mode5DurationEdit.setValue(this.mode5DurationInfinite ? "" : String.valueOf(this.mode5Duration));
                }

                return true;
            }

            String alwaysText = (this.mode5AlwaysEdible ? "☑ " : "☐ ") + Component.translatable("gui.visualcrafting.label.ignore_saturation").getString();
            int alwaysX = ml + 122;
            int alwaysW = this.font.width(alwaysText);
            if (mouseX >= (double)alwaysX && mouseX < (double)(alwaysX + alwaysW) && mouseY >= (double)(mt + 91) && mouseY < (double)(mt + 101)) {
                this.mode5AlwaysEdible = !this.mode5AlwaysEdible;
                return true;
            }

        }

        if (this.mode == 0) {
            this.craftListClick(mouseX, mouseY, button);
        } else if (this.mode == 1) {
            this.infuseListClick(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleDropdownClick(double mouseX, double mouseY, int button) {
        DropdownWidget[] dropdowns = this.getCurrentModeDropdowns();

        if (dropdowns.length == 0) {
            return false;
        }

        boolean anyExpanded = false;

        for (DropdownWidget dropdown : dropdowns) {
            if (dropdown != null && dropdown.isExpanded()) {
                anyExpanded = true;
                break;
            }
        }

        if (anyExpanded && button != 0) {
            for (DropdownWidget dropdown : dropdowns) {
                if (dropdown != null) {
                    dropdown.collapse();
                }
            }

            return true;
        }

        if (button != 0) {
            return false;
        }

        for (int i = dropdowns.length - 1; i >= 0; --i) {
            DropdownWidget dropdown = dropdowns[i];

            if (dropdown != null && dropdown.isExpanded()) {
                dropdown.mouseClicked(mouseX, mouseY, button);

                for (DropdownWidget other : dropdowns) {
                    if (other != null && other != dropdown) {
                        other.collapse();
                    }
                }

                return true;
            }
        }

        for (int i = dropdowns.length - 1; i >= 0; --i) {
            DropdownWidget dropdown = dropdowns[i];

            if (dropdown != null && dropdown.isMouseOver(mouseX, mouseY)) {
                for (DropdownWidget other : dropdowns) {
                    if (other != null && other != dropdown) {
                        other.collapse();
                    }
                }

                dropdown.mouseClicked(mouseX, mouseY, button);

                return true;
            }
        }

        return false;
    }

    private DropdownWidget[] getCurrentModeDropdowns() {
        return switch (this.mode) {

            case 2 -> new DropdownWidget[]{
                    this.mode2BiomeDropdown,
                    this.mode2Dropdown
            };

            case 5 -> new DropdownWidget[]{
                    this.mode5PotionDropdown
            };

            case 7 -> new DropdownWidget[]{
                    this.mode7TypeDropdown,
                    this.mode7SubtypeDropdown
            };

            case 8 -> new DropdownWidget[]{
                    this.mode8AttrDropdown,
                    this.mode8OpDropdown,
                    this.mode8SlotDropdown,
                    this.mode8EnchantDropdown
            };

            default -> new DropdownWidget[0];
        };
    }

    private void craftListClick(double mouseX, double mouseY, int button) {
        int listX = this.leftPos + this.imageWidth - 88 + this.recipesOffsetX;
        int listY = this.topPos + 13 + this.recipesOffsetY;
        int itemsPerPage = 7;
        for (int i = this.scrollOffset * itemsPerPage; i < Math.min((this.scrollOffset + 1) * itemsPerPage, this.recipes.size()); ++i) {
            int rowY = listY + (i - this.scrollOffset * itemsPerPage) * 20;
            if (!(mouseX >= (double)listX && mouseX <= (double)(listX + 88) && mouseY >= (double)rowY && mouseY <= (double)(rowY + 18))) continue;
            VisualCraftingBlockEntity.SavedRecipe savedRecipe = this.recipes.get(i);
            if (button == 1) {
                PacketDistributor.sendToServer(new ModMessages.RemoveRecipePacket(this.menu.blockPos, i), new CustomPacketPayload[0]);
                break;
            }

            if (button != 0 || savedRecipe.banned) break;
            int slotCount = this.menu.getCraftSlotCount();
            for (int j = 0; j < Math.min(savedRecipe.ingredients.size(), slotCount); ++j) {
                this.menu.craftSlots.setItem(j, savedRecipe.ingredients.get(j).copy());
            }

            Slot slot = this.menu.slots.get(81);
            slot.set(savedRecipe.result.copy());
            break;
        }
    }

    private void infuseListClick(double mouseX, double mouseY, int button) {
        int listX = this.getInfuseRecipesX();
        int listY = this.getInfuseRecipesY();
        int itemsPerPage = 5;
        for (int i = this.scrollOffset * itemsPerPage; i < Math.min((this.scrollOffset + 1) * itemsPerPage, this.infusingRecipes.size()); ++i) {
            int rowY = listY + (i - this.scrollOffset * itemsPerPage) * 18;
            if (!(mouseX >= (double)listX && mouseX <= (double)(listX + 88) && mouseY >= (double)rowY && mouseY <= (double)(rowY + 18))) continue;
            VisualCraftingBlockEntity.InfusingRecipe infusingRecipe = this.infusingRecipes.get(i);
            if (button == 1) {
                PacketDistributor.sendToServer(new ModMessages.RemoveInfusingRecipePacket(this.menu.blockPos, i), new CustomPacketPayload[0]);
                break;
            }

            if (button != 0 || infusingRecipe.banned) break;
            this.menu.craftSlots.setItem(0, infusingRecipe.inputB.copy());
            Slot slot = this.menu.slots.get(81);
            slot.set(infusingRecipe.output.copy());
            if (!infusingRecipe.inputA.isEmpty()) {
                try {
                    CompoundTag compoundTag = MekanismIntegration.getChemicalTagFromItem(infusingRecipe.inputA);
                    if (compoundTag != null) {
                        ChemSlotData chemSlotData = MekanismIntegration.buildChemSlotDataFromTag(compoundTag);
                        if (chemSlotData != null) {
                            this.menu.chemSlotData = chemSlotData;
                        }

                        this.selectedChemical = infusingRecipe.inputA.copy();
                    }

                }

                catch (Throwable e) {
                    VisualCraftingScreen.logWarn("Mekanism not available, skipping infusion chemical rendering", e);
                }

            }

            this.menu.chemAmount = infusingRecipe.infusionAmount;
            break;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int chemX;
        int chemY;
        if (this.mode == 2 && scrollY != 0.0) {
            int sliderX = this.leftPos + 71 + this.mode2OffsetX;
            int mineralSliderY = this.topPos + 42;
            int byproductSliderY = this.topPos + 70;
            boolean isMineral = mouseX >= (double)sliderX && mouseX < (double)(sliderX + 18) && mouseY >= (double)mineralSliderY && mouseY < (double)(mineralSliderY + 18);
            boolean isByproduct = mouseX >= (double)sliderX && mouseX < (double)(sliderX + 18) && mouseY >= (double)byproductSliderY && mouseY < (double)(byproductSliderY + 18);
            if (!isMineral && !isByproduct) {
                return this.mode2BiomeDropdown != null && this.mode2BiomeDropdown.isExpanded() && this.mode2BiomeDropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY) ? true : this.mode2Dropdown != null && this.mode2Dropdown.isExpanded() && this.mode2Dropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY);
            }

            int delta = (int)(Screen.hasControlDown() ? Math.signum(scrollY) : Math.signum(scrollY) * 10);
            if (isMineral) {
                this.mode2MineralPct = Math.clamp(this.mode2MineralPct + delta, 0, 100);
            } else {
                this.mode2ByproductPct = Math.clamp(this.mode2ByproductPct + delta, 0, 100);
            }

            this.saveMode2Config();
            return true;
        }

        if (this.mode == 5 && scrollY != 0.0) {
            return this.mode5PotionDropdown != null && this.mode5PotionDropdown.isExpanded() && this.mode5PotionDropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY);
        }

        if (this.mode == 7 && scrollY != 0.0) {
            if (this.mode7TypeDropdown != null && this.mode7TypeDropdown.isExpanded() && this.mode7TypeDropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY)) {
                return true;
            }

            if (this.mode7SubtypeDropdown != null && this.mode7SubtypeDropdown.isExpanded() && this.mode7SubtypeDropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY)) {
                return true;
            }

        }

        if (this.mode == 8 && scrollY != 0.0) {
            if (this.mode8AttrDropdown != null && this.mode8AttrDropdown.isExpanded() && this.mode8AttrDropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY)) {
                return true;
            }

            if (this.mode8OpDropdown != null && this.mode8OpDropdown.isExpanded() && this.mode8OpDropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY)) {
                return true;
            }

            if (this.mode8SlotDropdown != null && this.mode8SlotDropdown.isExpanded() && this.mode8SlotDropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY)) {
                return true;
            }

            if (this.mode8EnchantDropdown != null && this.mode8EnchantDropdown.isExpanded() && this.mode8EnchantDropdown.mouseScrolled(mouseX, mouseY, 0.0, scrollY)) {
                return true;
            }

            int areaTop = this.topPos + MODE8_SCROLL_TOP;
            int areaBottom = this.topPos + this.imageHeight - 83;
            if (mouseY >= (double)areaTop && mouseY < (double)areaBottom && mouseX >= (double)(this.leftPos + 56) && mouseX < (double)(this.leftPos + this.imageWidth - 8)) {
                int maxScroll = this.mode8MaxScroll();
                this.mode8ScrollOffset = Math.clamp(this.mode8ScrollOffset - ((int)Math.signum(scrollY)), 0, maxScroll);
                this.repositionMode8Widgets();
                return true;
            }

        }

        if (this.mode == 1 && scrollY != 0.0) {
            chemX = this.getChemSlotX();
            chemY = this.getChemSlotY();
            if (mouseX >= (double)chemX && mouseX < (double)(chemX + 18) && mouseY >= (double)chemY && mouseY < (double)(chemY + 18)) {
                int chemStep = (int)(Screen.hasControlDown() ? Math.signum(scrollY) * 10 : Math.signum(scrollY) * 100);
                this.menu.chemAmount = Math.clamp(((this.menu).chemAmount + chemStep), 0, 9000);
                return true;
            }

        }

        if (this.mode == 1) {
            int totalRecipes = this.infusingRecipes.size();
            int itemsPerRow = 5;
            int maxScroll = Math.max(0, (int)Math.ceil((double)totalRecipes / (double)itemsPerRow) - 1);
            if (scrollY != 0.0) {
                this.scrollOffset = Math.clamp(this.scrollOffset - ((int)Math.signum(scrollY)), 0, maxScroll);
                return true;
            }

        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.inventoryLabelY = -9999;
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (this.mode == 2) {
            this.updateMode2ButtonStates();
        }

        if (this.mode == 5) {
            this.syncMode5FromSlot0();
        }

        if (this.mode == 6 && this.mode6PickerOpen) {
            guiGraphics.flush();
            RenderSystem.disableDepthTest();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
            this.renderNamePicker(guiGraphics, mouseX, mouseY);
            if (this.mode6PickerHexEdit != null && this.mode6PickerHexEdit.isVisible()) {
                this.mode6PickerHexEdit.render(guiGraphics, mouseX, mouseY, partialTicks);
            }

            guiGraphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }

        if (this.mode == 8) {
            this.renderMode8Widgets(guiGraphics, mouseX, mouseY, partialTicks);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (this.mode == 1) {
            int chemSlotX = this.getChemSlotX();
            int chemSlotY = this.getChemSlotY();
            if (mouseX >= chemSlotX && mouseX < chemSlotX + 18 && mouseY >= chemSlotY && mouseY < chemSlotY + 18) {
                ChemSlotData chemSlotData = this.menu.chemSlotData;
                int chemAmt = this.menu.chemAmount;
                if (chemSlotData != null && chemSlotData.chemicalName != null && !chemSlotData.chemicalName.isEmpty()) {
                    guiGraphics.renderTooltip(this.font, Component.literal(chemSlotData.chemicalName + " x" + chemAmt + "mB"), mouseX, mouseY);
                } else {
                    try {
                        CompoundTag compoundTag = MekanismIntegration.getChemicalTagFromItem(this.selectedChemical);
                        if (compoundTag != null) {
                            String chemId = compoundTag.getString("chemicalId");
                            String chemName = chemId.contains(":") ? chemId.substring(chemId.lastIndexOf(":") + 1) : chemId;
                            guiGraphics.renderTooltip(this.font, Component.literal(chemName + " x" + chemAmt + "mB"), mouseX, mouseY);
                        }

                    } catch (Throwable e) {
                        logWarn("Mekanism not available, skipping tooltip chemical name", e);
                    }

                }

            }

            int recipeListY = this.getInfuseRecipesY();
            int recipeListX = this.getInfuseRecipesX();
            int itemsPerRow = 5;
            for (int i = this.scrollOffset * itemsPerRow; i < Math.min((this.scrollOffset + 1) * itemsPerRow, this.infusingRecipes.size()); ++i) {
                int rowY = recipeListY + (i - this.scrollOffset * itemsPerRow) * 18;
                if (mouseX < recipeListX || mouseX > recipeListX + 18 || mouseY < rowY || mouseY > rowY + 18) continue;
                VisualCraftingBlockEntity.InfusingRecipe infusingRecipe = this.infusingRecipes.get(i);
                if (infusingRecipe.banned) continue;
                guiGraphics.renderComponentTooltip(this.font, List.of(infusingRecipe.output.getHoverName(), Component.literal("Infusion: " + infusingRecipe.inputA.getHoverName().getString() + " x" + infusingRecipe.infusionAmount), Component.literal("Input: " + infusingRecipe.inputB.getHoverName().getString())), mouseX, mouseY);
            }

        }
    }

    public void updateRecipes(List<VisualCraftingBlockEntity.SavedRecipe> list) {
        this.recipes = new ArrayList<VisualCraftingBlockEntity.SavedRecipe>(list);
    }

    public void updateInfusingRecipes(List<VisualCraftingBlockEntity.InfusingRecipe> list) {
        this.infusingRecipes = new ArrayList<VisualCraftingBlockEntity.InfusingRecipe>(list);
    }

    public void onClose() {
        // 关闭界面统一复位：模式回到合成、tier 归零，重新打开即为合成界面
        // 同时清除 mode5 暂存效果预览（渲染层暂存，不写入真实物品）
        this.mode5PreviewEffects.clear();
        PacketDistributor.sendToServer(new ModMessages.ModeUpdatePacket(this.menu.blockPos, VisualCraftingBlockEntity.MODE_CRAFTING), new CustomPacketPayload[0]);
        PacketDistributor.sendToServer(new ModMessages.TierUpdatePacket(this.menu.blockPos, 0), new CustomPacketPayload[0]);
        super.onClose();
    }

    private void loadOffsets() {
        File file = new File("config/visualcrafting/gui_offsets.properties");
        if (!file.exists()) {
            File file2 = file.getParentFile();
            if (!file2.exists()) {
                file2.mkdirs();
            }

            this.saveOffsets();
        } else {
            try {
                Properties properties = new Properties();
                try (FileInputStream fileInputStream = new FileInputStream(file);){
                    properties.load(fileInputStream);
                }

                this.btnOffsetX = Integer.parseInt(properties.getProperty("btnOffsetX", "6"));
                this.btnOffsetY = Integer.parseInt(properties.getProperty("btnOffsetY", "0"));
                this.gridOffsetX = Integer.parseInt(properties.getProperty("gridOffsetX", "-8"));
                this.gridOffsetY = Integer.parseInt(properties.getProperty("gridOffsetY", "-8"));
                this.gridSlotOffsetX = Integer.parseInt(properties.getProperty("gridSlotOffsetX", "16"));
                this.gridSlotOffsetY = Integer.parseInt(properties.getProperty("gridSlotOffsetY", "13"));
                this.infAmountLabelOffsetX = Integer.parseInt(properties.getProperty("infAmountLabelOffsetX", "24"));
                this.infAmountLabelOffsetY = Integer.parseInt(properties.getProperty("infAmountLabelOffsetY", "0"));
                this.infButtonsOffsetX = Integer.parseInt(properties.getProperty("infButtonsOffsetX", "6"));
                this.infButtonsOffsetY = Integer.parseInt(properties.getProperty("infButtonsOffsetY", "0"));
                this.infChemSlotOffsetX = Integer.parseInt(properties.getProperty("infChemSlotOffsetX", "2"));
                this.infChemSlotOffsetY = Integer.parseInt(properties.getProperty("infChemSlotOffsetY", "4"));
                this.infEditBoxOffsetX = Integer.parseInt(properties.getProperty("infEditBoxOffsetX", "185"));
                this.infEditBoxOffsetY = Integer.parseInt(properties.getProperty("infEditBoxOffsetY", "69"));
                this.inputLabelX = Integer.parseInt(properties.getProperty("inputLabelX", "22"));
                this.inputLabelY = Integer.parseInt(properties.getProperty("inputLabelY", "3"));
                this.outputLabelX = Integer.parseInt(properties.getProperty("outputLabelX", "22"));
                this.outputLabelY = Integer.parseInt(properties.getProperty("outputLabelY", "3"));
                this.chemLabelX = Integer.parseInt(properties.getProperty("chemLabelX", "20"));
                this.chemLabelY = Integer.parseInt(properties.getProperty("chemLabelY", "5"));
                this.chemLineX = Integer.parseInt(properties.getProperty("chemLineX", "0"));
                this.chemLineY = Integer.parseInt(properties.getProperty("chemLineY", "0"));
                this.infInputSlotOffsetX = Integer.parseInt(properties.getProperty("infInputSlotOffsetX", "-1"));
                this.infInputSlotOffsetY = Integer.parseInt(properties.getProperty("infInputSlotOffsetY", "-1"));
                this.infInputSlotSlotOffsetX = Integer.parseInt(properties.getProperty("infInputSlotSlotOffsetX", "3"));
                this.infInputSlotSlotOffsetY = Integer.parseInt(properties.getProperty("infInputSlotSlotOffsetY", "-5"));
                this.infInvLineOffsetX = Integer.parseInt(properties.getProperty("infInvLineOffsetX", "-1"));
                this.infInvLineOffsetY = Integer.parseInt(properties.getProperty("infInvLineOffsetY", "-1"));
                this.infInvSlotSlotOffsetX = Integer.parseInt(properties.getProperty("infInvSlotSlotOffsetX", "3"));
                this.infInvSlotSlotOffsetY = Integer.parseInt(properties.getProperty("infInvSlotSlotOffsetY", "-7"));
                this.infOutSlotLineOffsetX = Integer.parseInt(properties.getProperty("infOutSlotLineOffsetX", "-1"));
                this.infOutSlotLineOffsetY = Integer.parseInt(properties.getProperty("infOutSlotLineOffsetY", "-1"));
                this.infOutSlotSlotOffsetX = Integer.parseInt(properties.getProperty("infOutSlotSlotOffsetX", "1"));
                this.infOutSlotSlotOffsetY = Integer.parseInt(properties.getProperty("infOutSlotSlotOffsetY", "-2"));
                this.invLabelOffsetX = Integer.parseInt(properties.getProperty("invLabelOffsetX", "-179"));
                this.invLabelOffsetY = Integer.parseInt(properties.getProperty("invLabelOffsetY", "-4"));
                this.invLineOffsetX = Integer.parseInt(properties.getProperty("invLineOffsetX", "-1"));
                this.invLineOffsetY = Integer.parseInt(properties.getProperty("invLineOffsetY", "-1"));
                this.invSlotSlotOffsetX = Integer.parseInt(properties.getProperty("invSlotSlotOffsetX", "9"));
                this.invSlotSlotOffsetY = Integer.parseInt(properties.getProperty("invSlotSlotOffsetY", "21"));
                this.outSlotLineOffsetX = Integer.parseInt(properties.getProperty("outSlotLineOffsetX", "-1"));
                this.outSlotLineOffsetY = Integer.parseInt(properties.getProperty("outSlotLineOffsetY", "-1"));
                this.outSlotSlotOffsetX = Integer.parseInt(properties.getProperty("outSlotSlotOffsetX", "16"));
                this.outSlotSlotOffsetY = Integer.parseInt(properties.getProperty("outSlotSlotOffsetY", "11"));
                this.recipesOffsetX = Integer.parseInt(properties.getProperty("recipesOffsetX", "0"));
                this.recipesOffsetY = Integer.parseInt(properties.getProperty("recipesOffsetY", "2"));
                this.tierOffsetY = Integer.parseInt(properties.getProperty("tierOffsetY", "0"));
            }

            catch (Exception e) {
                VisualCraftingScreen.logWarn("Failed to load GUI offsets, using defaults", e);
            }

        }
    }

    private void saveOffsets() {
        File file = new File("config/visualcrafting/gui_offsets.properties");
        File file2 = file.getParentFile();
        if (!file2.exists()) {
            file2.mkdirs();
        }

        try {
            Properties properties = new Properties();
            properties.setProperty("btnOffsetX", String.valueOf(this.btnOffsetX));
            properties.setProperty("btnOffsetY", String.valueOf(this.btnOffsetY));
            properties.setProperty("gridOffsetX", String.valueOf(this.gridOffsetX));
            properties.setProperty("gridOffsetY", String.valueOf(this.gridOffsetY));
            properties.setProperty("gridSlotOffsetX", String.valueOf(this.gridSlotOffsetX));
            properties.setProperty("gridSlotOffsetY", String.valueOf(this.gridSlotOffsetY));
            properties.setProperty("infAmountLabelOffsetX", String.valueOf(this.infAmountLabelOffsetX));
            properties.setProperty("infAmountLabelOffsetY", String.valueOf(this.infAmountLabelOffsetY));
            properties.setProperty("infButtonsOffsetX", String.valueOf(this.infButtonsOffsetX));
            properties.setProperty("infButtonsOffsetY", String.valueOf(this.infButtonsOffsetY));
            properties.setProperty("infChemSlotOffsetX", String.valueOf(this.infChemSlotOffsetX));
            properties.setProperty("infChemSlotOffsetY", String.valueOf(this.infChemSlotOffsetY));
            properties.setProperty("infEditBoxOffsetX", String.valueOf(this.infEditBoxOffsetX));
            properties.setProperty("infEditBoxOffsetY", String.valueOf(this.infEditBoxOffsetY));
            properties.setProperty("inputLabelX", String.valueOf(this.inputLabelX));
            properties.setProperty("inputLabelY", String.valueOf(this.inputLabelY));
            properties.setProperty("outputLabelX", String.valueOf(this.outputLabelX));
            properties.setProperty("outputLabelY", String.valueOf(this.outputLabelY));
            properties.setProperty("chemLabelX", String.valueOf(this.chemLabelX));
            properties.setProperty("chemLabelY", String.valueOf(this.chemLabelY));
            properties.setProperty("chemLineX", String.valueOf(this.chemLineX));
            properties.setProperty("chemLineY", String.valueOf(this.chemLineY));
            properties.setProperty("infInputSlotOffsetX", String.valueOf(this.infInputSlotOffsetX));
            properties.setProperty("infInputSlotOffsetY", String.valueOf(this.infInputSlotOffsetY));
            properties.setProperty("infInputSlotSlotOffsetX", String.valueOf(this.infInputSlotSlotOffsetX));
            properties.setProperty("infInputSlotSlotOffsetY", String.valueOf(this.infInputSlotSlotOffsetY));
            properties.setProperty("infInvLineOffsetX", String.valueOf(this.infInvLineOffsetX));
            properties.setProperty("infInvLineOffsetY", String.valueOf(this.infInvLineOffsetY));
            properties.setProperty("infInvSlotSlotOffsetX", String.valueOf(this.infInvSlotSlotOffsetX));
            properties.setProperty("infInvSlotSlotOffsetY", String.valueOf(this.infInvSlotSlotOffsetY));
            properties.setProperty("infOutSlotLineOffsetX", String.valueOf(this.infOutSlotLineOffsetX));
            properties.setProperty("infOutSlotLineOffsetY", String.valueOf(this.infOutSlotLineOffsetY));
            properties.setProperty("infOutSlotSlotOffsetX", String.valueOf(this.infOutSlotSlotOffsetX));
            properties.setProperty("infOutSlotSlotOffsetY", String.valueOf(this.infOutSlotSlotOffsetY));
            properties.setProperty("invLabelOffsetX", String.valueOf(this.invLabelOffsetX));
            properties.setProperty("invLabelOffsetY", String.valueOf(this.invLabelOffsetY));
            properties.setProperty("invLineOffsetX", String.valueOf(this.invLineOffsetX));
            properties.setProperty("invLineOffsetY", String.valueOf(this.invLineOffsetY));
            properties.setProperty("invSlotSlotOffsetX", String.valueOf(this.invSlotSlotOffsetX));
            properties.setProperty("invSlotSlotOffsetY", String.valueOf(this.invSlotSlotOffsetY));
            properties.setProperty("outSlotLineOffsetX", String.valueOf(this.outSlotLineOffsetX));
            properties.setProperty("outSlotLineOffsetY", String.valueOf(this.outSlotLineOffsetY));
            properties.setProperty("outSlotSlotOffsetX", String.valueOf(this.outSlotSlotOffsetX));
            properties.setProperty("outSlotSlotOffsetY", String.valueOf(this.outSlotSlotOffsetY));
            properties.setProperty("recipesOffsetX", String.valueOf(this.recipesOffsetX));
            properties.setProperty("recipesOffsetY", String.valueOf(this.recipesOffsetY));
            properties.setProperty("tierOffsetY", String.valueOf(this.tierOffsetY));
            try (FileOutputStream fileOutputStream = new FileOutputStream(file);){
                properties.store(fileOutputStream, "VisualCrafting GUI offsets");
            }

        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Failed to save GUI offsets", e);
        }
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public static void drawOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public int getOutputSlotX() {
        return this.slotAbsX(81);
    }

    public int getOutputSlotY() {
        return this.slotAbsY(81);
    }

    VisualCraftingMenu getVisualCraftingMenu() {
        return this.menu;
    }

    int autoButtonWidth(Component label) {
        int textWidth = this.font.width(label);
        return Math.max(46, Math.min(84, textWidth + 10));
    }

    void renderInfusingExtras(GuiGraphics guiGraphics) {
        int inputX = this.infInputAbsX();
        int inputY = this.infInputAbsY();
        int outputX = this.infOutAbsX();
        int outputY = this.infOutAbsY();
        int chemX = this.getChemSlotX();
        int chemY = this.getChemSlotY();
        this.renderSlotOutline(guiGraphics, 0);
        this.renderSlotOutline(guiGraphics, 81);
        VisualCraftingScreen.drawOutline(guiGraphics, chemX, chemY, 18, 18, -1);
        int chemColor = -8355712;
        ChemSlotData chemSlotData = this.menu.chemSlotData;
        boolean chemMarked = chemSlotData != null || !this.selectedChemical.isEmpty();
        if (chemSlotData != null) {
            chemColor = chemSlotData.tintColor;
        } else if (!this.selectedChemical.isEmpty()) {
            try {
                CompoundTag compoundTag = MekanismIntegration.getChemicalTagFromItem(this.selectedChemical);
                if (compoundTag != null) {
                    chemColor = MekanismIntegration.getChemicalColorFromTag(compoundTag);
                }

            }

            catch (Throwable e) {
                VisualCraftingScreen.logWarn("Mekanism not available, skipping infusion extras chemical rendering", e);
            }

        }

        if (chemColor == 0) {
            // tint 反射兜底失败时使用醒目青色，保证已标记状态可见
            chemColor = 0x00D8FF;
        }

        int fillAlpha = chemMarked ? 0xE0000000 : 0x28000000;
        RenderSystem.enableBlend();
        guiGraphics.fill(chemX + 1, chemY + 1, chemX + 17, chemY + 17, fillAlpha | chemColor & 0xFFFFFF);
        RenderSystem.disableBlend();
        ItemStack markStack = this.selectedChemical;
        if (markStack.isEmpty() && chemSlotData != null) {
            try {
                CompoundTag tag = new CompoundTag();
                tag.putString("chemicalId", chemSlotData.chemicalId);
                tag.putLong("amount", this.menu.chemAmount);
                markStack = MekanismIntegration.createChemicalTagItem(tag);
            }

            catch (Throwable e) {
                VisualCraftingScreen.logWarn("Mekanism not available, skipping chemical mark icon", e);
            }

        }

        // 化学品载体是 BARRIER + CUSTOM_DATA 的幽灵物品，直接 renderItem 会画出屏障图标，
        // 因此化学品标记只保留 tint 底色块 + 同色描边，不渲染载体物品本身。
        boolean chemCarrier = !markStack.isEmpty()
                && !MekanismIntegration.getChemicalTagFromItem(markStack).isEmpty();
        if (chemMarked && !markStack.isEmpty() && !chemCarrier) {
            guiGraphics.renderItem(markStack, chemX + 1, chemY + 1);
        } else if (chemMarked) {
            // 已标记：绘制化学品贴图（tint 着色）+ 同色描边；不渲染 BARRIER 载体、也不再画问号
            String markedChemicalId = chemSlotData != null ? chemSlotData.chemicalId : null;
            if (markedChemicalId == null || markedChemicalId.isEmpty()) {
                markedChemicalId = MekanismIntegration.getChemicalTagFromItem(markStack).getString("chemicalId");
            }

            MekanismIntegration.renderChemicalIcon(guiGraphics, markedChemicalId, chemColor, chemX + 1, chemY + 1);
            VisualCraftingScreen.drawOutline(guiGraphics, chemX, chemY, 18, 18, 0xFF000000 | (chemColor & 0xFFFFFF));
        }

        int currentChemAmount = this.menu.chemAmount;
        if (currentChemAmount < 0) {
            currentChemAmount = 0;
        }

        String amountText = currentChemAmount + "mB";
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.label.input").getString(), inputX + this.inputLabelX, inputY + this.inputLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.label.output").getString(), outputX + this.outputLabelX, outputY + this.outputLabelY, 0x404040, false);
    }

    void renderMode2Extras(GuiGraphics guiGraphics) {
        // ===== Mode2 compact painting (baseline: bak_20260723_GUI_OK) =====
        // Column headers: dimension/biome above dropdowns, layer above y-range edits
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.col.biome").getString(), this.leftPos + 67 + this.mode2OffsetX, this.topPos + 7, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.col.layer").getString(), this.leftPos + 63 + this.mode2OffsetX, this.topPos + 106, 0x404040, false);
        guiGraphics.drawString(this.font, "~", this.leftPos + 136 + this.mode2OffsetX, this.topPos + 106, 0xFFFFFF, false);
        int invOffX = this.invLineOffsetX;
        int invOffY = this.invLineOffsetY;
        int slotX = this.leftPos + 71 + this.mode2OffsetX;
        int mineralY = this.topPos + 42;
        int byproductY = this.topPos + 70;
        int labelX = slotX + invOffX - 11;
        int labelStep = 9;
        // Mineral row: vertical "矿/物" label, 18x18 slot outline, amount header, pct below slot
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.vert.ore.1").getString(), labelX, mineralY + invOffY + 1, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.vert.ore.2").getString(), labelX, mineralY + invOffY + 1 + labelStep, 0x404040, false);
        this.renderSlotOutline(guiGraphics, 0);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.col.amount").getString(), this.leftPos + 91 + this.mode2OffsetX, this.topPos + 43, 0x404040, false);
        String mineralPctText = this.mode2MineralPct + "%";
        guiGraphics.drawString(this.font, mineralPctText, slotX + 9 - this.font.width(mineralPctText) / 2, this.topPos + 61, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "~", this.leftPos + 136 + this.mode2OffsetX, this.topPos + 47, 0xFFFFFF, false);
        // Byproduct row: vertical "伴/生" label, 18x18 slot outline, amount header, pct below slot
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.vert.by.1").getString(), labelX, byproductY + invOffY + 1, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.vert.by.2").getString(), labelX, byproductY + invOffY + 1 + labelStep, 0x404040, false);
        this.renderSlotOutline(guiGraphics, 1);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.col.amount").getString(), this.leftPos + 91 + this.mode2OffsetX, this.topPos + 77, 0x404040, false);
        String byproductPctText = this.mode2ByproductPct + "%";
        guiGraphics.drawString(this.font, byproductPctText, slotX + 9 - this.font.width(byproductPctText) / 2, this.topPos + 88, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "~", this.leftPos + 135 + this.mode2OffsetX, this.topPos + 80, 0xFFFFFF, false);
        // 「禁止生成」的当前状态由按钮文案体现（已禁用时显示 mode2.ban_gen.done），见 updateMode2ButtonLabels
    }

    private void syncMode5FromSlot0() {
        ItemStack itemStack = this.menu.slots.get(0).getItem();
        if (ItemStack.matches(itemStack, this.mode5LastSlot0Item)) {
            return;
        }

        this.mode5LastSlot0Item = itemStack.copy();
        // 槽位物品变化时清除暂存效果预览，避免预览附着在旧物品上
        this.mode5PreviewEffects.clear();
        if (itemStack.isEmpty()) {
            return;
        }

        FoodProperties foodProperties = (FoodProperties)itemStack.getComponents().get(DataComponents.FOOD);
        if (foodProperties == null) {
            return;
        }

        this.mode5Hunger = foodProperties.nutrition();
        this.mode5Saturation = foodProperties.saturation();
        if (this.mode5HungerEdit != null) {
            this.mode5HungerEdit.setValue(String.valueOf(this.mode5Hunger));
        }

        if (this.mode5SaturationEdit != null) {
            this.mode5SaturationEdit.setValue(String.valueOf(this.mode5Saturation));
        }

        this.mode5PotionSelectedIdx.clear();
        this.mode5PotionLevel = 1;
        List list = foodProperties.effects();
        if (!list.isEmpty()) {
            // 首个效果预填"当前选中"（等级同步到输入框，兼容单效果编辑流程）
            MobEffectInstance mobEffectInstance = ((FoodProperties.PossibleEffect)list.get(0)).effect();
            ResourceLocation resourceLocation = this.minecraft.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).getKey(mobEffectInstance.getEffect().value());
            if (resourceLocation != null) {
                String potionId = resourceLocation.toString();
                for (int i = 0; i < this.mode5PotionIds.size(); ++i) {
                    if (!this.mode5PotionIds.get(i).equals(potionId)) continue;
                    this.mode5PotionSelectedIdx.add(i);
                    break;
                }

            }

            this.mode5PotionLevel = mobEffectInstance.getAmplifier() + 1;

            // 其余效果全部追加到暂存预览列表（含无限时长 -1），使读取旧食物后"生成脚本"同样覆盖全部效果
            for (int k = 1; k < list.size(); ++k) {
                MobEffectInstance extraEffect = ((FoodProperties.PossibleEffect)list.get(k)).effect();
                ResourceLocation effectKey = this.minecraft.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).getKey(extraEffect.getEffect().value());
                if (effectKey == null) continue;
                String name = effectKey.toString();
                for (int i = 0; i < this.mode5PotionIds.size(); ++i) {
                    if (i >= this.mode5PotionNames.size()) break;
                    if (!this.mode5PotionIds.get(i).equals(name)) continue;
                    name = this.mode5PotionNames.get(i);
                    break;
                }
                int durTicks = extraEffect.getDuration();
                String durStr = durTicks < 0 ? "-1" : String.valueOf(durTicks / 20);
                this.mode5PreviewEffects.add(new String[]{name, String.valueOf(extraEffect.getAmplifier() + 1), durStr});
            }
        }

        if (this.mode5PotionDropdown != null) {
            this.loadMode5Potions();
        }

        if (this.mode5PotionLevelEdit != null) {
            this.mode5PotionLevelEdit.setValue(String.valueOf(this.mode5PotionLevel));
        }
    }

    private void loadMode5Potions() {
        this.mode5PotionNames.clear();
        this.mode5PotionIds.clear();
        try {
            if (this.minecraft != null && this.minecraft.level != null) {
                Registry<MobEffect> registry = this.minecraft.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
                for (MobEffect mobEffect : registry) {
                    ResourceLocation resourceLocation = BuiltInRegistries.MOB_EFFECT.getKey(mobEffect);
                    if (resourceLocation == null) continue;
                    this.mode5PotionIds.add(resourceLocation.toString());
                    String translationKey = "effect." + resourceLocation.getNamespace() + "." + resourceLocation.getPath();
                    String displayName = Language.getInstance().getOrDefault(translationKey);
                    if (displayName.equals(translationKey)) {
                        displayName = resourceLocation.getPath();
                    }

                    this.mode5PotionNames.add(displayName);
                }

            }

        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Failed to load potion effects from registry", null);
        }

        if (this.mode5PotionDropdown != null) {
            // 单选模式：直接以当前选中项作为下拉初始选项
            int curIdx = this.mode5PotionSelectedIdx.isEmpty() ? 0 : this.mode5PotionSelectedIdx.iterator().next();
            this.mode5PotionDropdown.setOptions(this.mode5PotionNames, curIdx);
        }
    }

    protected void renderMode5Extras(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int gl = this.leftPos;
        int gt = this.topPos;
        // Slot outlines
        this.renderSlotOutline(guiGraphics, 0);
        this.renderSlotOutline(guiGraphics, 1);
        // Labels
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.food"), gl + 72, gt + 20, 55, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.hunger"), gl + 116, gt + 20, 55, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.saturation"), gl + 172, gt + 20, 55, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.potion_effects"), gl + 56, gt + 68, 120, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.level"), gl + 180, gt + 68, 48, 4210752);
        // Return item indicator
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.return_item"), gl + 63, gt + 43, 58, 4210752);
        // Duration indicator
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.duration"), gl + 116, gt + 43, 55, 4210752);
        String infiniteLabel = Component.translatable("gui.visualcrafting.mode5.label.infinite").getString();
        String durationValue = (this.mode5DurationInfinite ? "☑ " : "☐ ") + infiniteLabel;
        guiGraphics.drawString(this.font, durationValue, gl + 173, gt + 43, 4210752);
        // Eat time and always edible
        String eatTimeText = Component.translatable("gui.visualcrafting.label.eat_time").getString();
        // 输入框起点 gl+93：标签右对齐并留 5px 间隙，y 与输入框(88~104)垂直居中
        guiGraphics.drawString(this.font, eatTimeText, gl + 93 - 5 - this.font.width(eatTimeText), gt + 92, 4210752);
        String alwaysEdibleText = (this.mode5AlwaysEdible ? "☑ " : "☐ ") + Component.translatable("gui.visualcrafting.label.ignore_saturation").getString();
        this.drawWrapped(guiGraphics, this.font, Component.literal(alwaysEdibleText), gl + 121, gt + 92, 110, 4210752);
        // 暂存效果预览：食物槽位金色高亮框 + 右上角徽标 + 悬停 tooltip
        if (!this.mode5PreviewEffects.isEmpty()) {
            guiGraphics.renderOutline(gl + 93, gt + 16, 18, 18, 0xFFD700);
            guiGraphics.drawString(this.font, "✦" + this.mode5PreviewEffects.size(), gl + 96, gt + 3, 0xFFD700, false);
            if (mouseX >= gl + 93 && mouseX < gl + 111 && mouseY >= gt + 16 && mouseY < gt + 34) {
                List<Component> list2 = new ArrayList<Component>();
                list2.add(Component.translatable("gui.visualcrafting.mode5.status.effect_added", this.mode5PreviewEffects.size()));
                for (String[] effect : this.mode5PreviewEffects) {
                    String durationText = effect[2].equals("-1") ? Component.translatable("gui.visualcrafting.mode5.label.infinite").getString() : effect[2] + "s";
                    list2.add(Component.literal("· " + effect[0] + " Lv." + effect[1] + " (" + durationText + ")"));
                }
                guiGraphics.renderComponentTooltip(this.font, list2, mouseX, mouseY);
            }
        }
    }

    private void buildFood(int gl, int gt) {
        // 隐藏网格槽 0~79，保留 80（食物槽）和 81（返回物槽）
        NonNullList<Slot> slots = menu.slots;
        try {
            java.lang.reflect.Field fx = Slot.class.getDeclaredField("x");
            java.lang.reflect.Field fy = Slot.class.getDeclaredField("y");
            fx.setAccessible(true);
            fy.setAccessible(true);
            for (int i = 0; i < 80; i++) {
                Slot s = slots.get(i);
                fx.setInt(s, -2000);
                fy.setInt(s, -2000);
            }

            Slot foodSlot = slots.get(80);
            fx.setInt(foodSlot, 95);
            fy.setInt(foodSlot, 18);
            Slot returnSlot = slots.get(81);
            fx.setInt(returnSlot, 95);
            fy.setInt(returnSlot, 41);
            for (int i = 82; i < slots.size(); i++) {
                Slot s = slots.get(i);
                int col = (i - 82) % 9;
                int row = (i - 82) / 9;
                fx.setInt(s, 8 + col * 18);
                fy.setInt(s, imageHeight - 83 + row * 18);
            }

        } catch (Exception e) {}
    }

    private void initCraftingWidgets() {
        int slotIdx;
        Button button2 = Button.builder(Component.translatable("gui.visualcrafting.shaped"), this::onShapedCraft).pos(this.leftPos + 2 + this.btnOffsetX, this.topPos + 12 + this.btnOffsetY).size(46, 16).build();
        Button button3 = Button.builder(Component.translatable("gui.visualcrafting.shapeless"), this::onShapelessCraft).pos(this.leftPos + 2 + this.btnOffsetX, this.topPos + 31 + this.btnOffsetY).size(46, 16).build();
        Button button4 = Button.builder(Component.translatable("gui.visualcrafting.delete_recipe"), this::onDelCraft).pos(this.leftPos + 2 + this.btnOffsetX, this.topPos + 50 + this.btnOffsetY).size(46, 16).build();
        Button button5 = Button.builder(Component.translatable("gui.visualcrafting.config"), button -> {
            File file = this.format == 0 ? new File(this.minecraft.gameDirectory, "kubejs/server_scripts") : new File(this.minecraft.gameDirectory, "scripts");
            File file2 = file;
            if (!file.exists()) {
                file.mkdirs();
            }

            Util.getPlatform().openFile(file);
        }).pos(this.leftPos + 2 + this.btnOffsetX, this.topPos + 69 + this.btnOffsetY).size(46, 16).build();
        this.funcButtons.add(this.addRenderableWidget(button2));
        this.funcButtons.add(this.addRenderableWidget(button3));
        this.funcButtons.add(this.addRenderableWidget(button4));
        this.funcButtons.add(this.addRenderableWidget(button5));
        MutableComponent mutableComponent = this.format == 0 ? Component.literal("KubeJS") : Component.literal("CRT");
        this.formatToggle = Button.builder(mutableComponent, this::onFormatToggle).pos(this.leftPos + 2 + this.btnOffsetX, this.topPos + this.imageHeight - 26 + this.tierOffsetY).size(46, 16).build();
        this.addRenderableWidget(this.formatToggle);
        Button button6 = Button.builder(Component.translatable(TIER_LABELS[0]), button -> this.onTier(0)).pos(this.leftPos + this.imageWidth - 94, this.topPos + this.imageHeight - 44 + this.tierOffsetY).size(46, 16).build();
        Button button7 = Button.builder(Component.translatable(TIER_LABELS[1]), button -> this.onTier(1)).pos(this.leftPos + this.imageWidth - 46, this.topPos + this.imageHeight - 44 + this.tierOffsetY).size(46, 16).build();
        Button button8 = Button.builder(Component.translatable(TIER_LABELS[2]), button -> this.onTier(2)).pos(this.leftPos + this.imageWidth - 94, this.topPos + this.imageHeight - 26 + this.tierOffsetY).size(46, 16).build();
        Button button9 = Button.builder(Component.translatable(TIER_LABELS[3]), button -> this.onTier(3)).pos(this.leftPos + this.imageWidth - 46, this.topPos + this.imageHeight - 26 + this.tierOffsetY).size(46, 16).build();
        this.tierButtons.add(this.addRenderableWidget(button6));
        this.tierButtons.add(this.addRenderableWidget(button7));
        this.tierButtons.add(this.addRenderableWidget(button8));
        this.tierButtons.add(this.addRenderableWidget(button9));
        this.tierButtons.forEach(b -> {
            b.visible = this.format != 0;
        });
        int gridSize = this.getGridSize();
        int invBaseY = 13 + gridSize * 18;
        int hotbarBaseY = invBaseY + 8;
        for (slotIdx = 82; slotIdx < this.menu.slots.size(); ++slotIdx) {
            Slot slot = this.menu.slots.get(slotIdx);
            int col = (slotIdx - 82) % 9;
            int row = (slotIdx - 82) / 9;
            VisualCraftingScreen.setSlotX(slot, 8 + col * 18 + this.invSlotSlotOffsetX);
            VisualCraftingScreen.setSlotY(slot, hotbarBaseY + row * 18 + this.invSlotSlotOffsetY);
        }

        // grid/out 槽最终坐标统一由 layoutCraftingSlots() 设置（rebuildWidgets 最后阶段），此处不再叠加，避免双叠加
    }

    private void initInfusingWidgets() {
        if (this.mode == 2) {
            this.initMode2Widgets();
        } else {
            for (int i = 1; i <= 80; ++i) {
                VisualCraftingScreen.setSlotX(this.menu.slots.get(i), -2000);
                VisualCraftingScreen.setSlotY(this.menu.slots.get(i), -2000);
            }

            VisualCraftingScreen.setSlotX(this.menu.slots.get(0), 80 + this.infInputSlotSlotOffsetX);
            VisualCraftingScreen.setSlotY(this.menu.slots.get(0), 35 + this.infInputSlotSlotOffsetY);
            VisualCraftingScreen.setSlotX(this.menu.slots.get(81), 148 + this.infOutSlotSlotOffsetX);
            VisualCraftingScreen.setSlotY(this.menu.slots.get(81), 45 + this.infOutSlotSlotOffsetY);
            WrappableButton wrappableButton = new WrappableButton(this.leftPos + 2 + this.infButtonsOffsetX, this.topPos + 12 + this.infButtonsOffsetY, 54, 16, Component.translatable("gui.visualcrafting.mode1.add"), this::onInfuseAdd);
            WrappableButton wrappableButton2 = new WrappableButton(this.leftPos + 2 + this.infButtonsOffsetX, this.topPos + 31 + this.infButtonsOffsetY, 54, 16, Component.translatable("gui.visualcrafting.mode1.delete"), this::onInfuseDelete);
            WrappableButton wrappableButton3 = new WrappableButton(this.leftPos + 2 + this.infButtonsOffsetX, this.topPos + 50 + this.infButtonsOffsetY, 54, 16, Component.translatable("gui.visualcrafting.config"), this::onInfuseConfig);
            this.funcButtons.add(this.addRenderableWidget(wrappableButton));
            this.funcButtons.add(this.addRenderableWidget(wrappableButton2));
            this.funcButtons.add(this.addRenderableWidget(wrappableButton3));
            for (int i = 82; i < this.menu.slots.size(); ++i) {
                Slot slot = this.menu.slots.get(i);
                int col = (i - 82) % 9;
                int row = (i - 82) / 9;
                VisualCraftingScreen.setSlotX(slot, 8 + col * 18);
                VisualCraftingScreen.setSlotY(slot, this.imageHeight - 83 + row * 18);
            }

        }
    }
        // ===================== Mode 6: 命名牌标签页 =====================
    private void initNameWidgets() {
        for (int i = 0; i <= 81; ++i) {
            VisualCraftingScreen.setSlotX(this.menu.slots.get(i), -2000);
            VisualCraftingScreen.setSlotY(this.menu.slots.get(i), -2000);
        }

        for (int i = 82; i < this.menu.slots.size(); ++i) {
            Slot slot = this.menu.slots.get(i);
            int col = (i - 82) % 9;
            int row = (i - 82) / 9;
            VisualCraftingScreen.setSlotX(slot, 8 + col * 18);
            VisualCraftingScreen.setSlotY(slot, this.imageHeight - 83 + row * 18);
        }

        VisualCraftingScreen.setSlotX(this.menu.slots.get(0), 11);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(0), 74);
        Component saveLabel = Component.translatable("gui.visualcrafting.name.save");
        Component removeLabel = Component.translatable("gui.visualcrafting.name.remove");
        Component configLabel = Component.translatable("gui.visualcrafting.config");
        this.mode6BtnSave = Button.builder(saveLabel, this::onMode6GenerateScript).pos(this.leftPos + 8, this.topPos + 12).size(this.autoButtonWidth(saveLabel), 16).build();
        this.mode6BtnDelete = Button.builder(removeLabel, this::onMode6DeleteRecipe).pos(this.leftPos + 8, this.topPos + 31).size(this.autoButtonWidth(removeLabel), 16).build();
        this.mode6BtnConfig = Button.builder(configLabel, this::onMode6Config).pos(this.leftPos + 8, this.topPos + 50).size(this.autoButtonWidth(configLabel), 16).build();
        this.funcButtons.add(this.addRenderableWidget(this.mode6BtnSave));
        this.funcButtons.add(this.addRenderableWidget(this.mode6BtnDelete));
        this.funcButtons.add(this.addRenderableWidget(this.mode6BtnConfig));
        this.mode6NameEdit = new EditBox(this.font, this.leftPos + 100, this.topPos + 13, 108, 16, Component.empty());
        this.mode6NameEdit.setMaxLength(200);
        this.mode6NameEdit.setValue(this.mode6NameInput);
        this.mode6NameEdit.setResponder(s -> this.mode6NameInput = s);
        this.addRenderableWidget(this.mode6NameEdit);
        for (int r = 0; r < MODE6_LORE_VISIBLE; ++r) {
            EditBox box = new EditBox(this.font, this.leftPos + 100, this.topPos + 33 + r * 18, 108, 16, Component.empty());
            box.setMaxLength(200);
            this.mode6LoreEdits[r] = box;
            this.addRenderableWidget(box);
        }

        this.mode6RefreshLoreEdits();
        Component[] fontLabels = new Component[]{
                Component.translatable("gui.visualcrafting.name.italic"),
                Component.translatable("gui.visualcrafting.name.bold"),
                Component.translatable("gui.visualcrafting.name.underline"),
                Component.translatable("gui.visualcrafting.name.strike")
        };
        for (int i = 0; i < 4; ++i) {
            final int idx = i;
            this.mode6FontButtons[i] = Button.builder(fontLabels[i], b -> this.onMode6FontToggle(idx)).pos(this.leftPos + 8 + i * 58, this.topPos + 110).size(54, 16).build();
            this.funcButtons.add(this.addRenderableWidget(this.mode6FontButtons[i]));
        }

        this.mode6UpdateFontLabels();
        this.mode6BtnGlow = Button.builder(VisualCraftingScreen.mode6FontLabel("gui.visualcrafting.name.glow", this.mode6Glow), b -> this.onMode6GlowToggle())
                .pos(this.leftPos + 33, this.topPos + 75).size(46, 16).build();
        this.funcButtons.add(this.addRenderableWidget(this.mode6BtnGlow));
        this.mode6PickerHexEdit = new EditBox(this.font, this.leftPos + 62, this.topPos + 150, 60, 16, Component.empty());
        this.mode6PickerHexEdit.setMaxLength(7);
        this.mode6PickerHexEdit.setValue("#FFFFFF");
        this.mode6PickerHexEdit.setResponder(s -> {
            int parsed = VisualCraftingScreen.parseHexColor(s);
            if (parsed >= 0) {
                this.mode6PickerColor = parsed;
            }

        });
        this.mode6PickerHexEdit.setVisible(false);
        this.addRenderableWidget(this.mode6PickerHexEdit);
        this.mode6PickerOpen = false;
        this.mode6PickerTarget = -2;
    }

    private void onMode6FontToggle(int idx) {
        boolean[] fonts = this.mode6TargetFonts();
        fonts[idx] = !fonts[idx];
        this.mode6UpdateFontLabels();
    }

    /** 当前字体作用目标对应的标志数组（-1 或越界 = 名称行）。 */

    private boolean[] mode6TargetFonts() {
        int target = this.mode6FontTarget;
        if (target >= 0 && target < MODE6_LORE_MAX && target < this.mode6LoreLines.size()) {
            return this.mode6LoreFonts[target];
        }

        return this.mode6NameFonts;
    }

    /** 指定作用目标是否启用彩虹色。 */

    private boolean mode6RainbowFor(int target) {
        if (target >= 0 && target < MODE6_LORE_MAX && target < this.mode6LoreLines.size()) {
            return this.mode6LoreRainbow[target];
        }

        return this.mode6NameRainbow;
    }

    /** 设置指定作用目标（-1 或越界 = 名称行）的彩虹色开关。 */

    private void mode6SetRainbowFor(int target, boolean value) {
        if (target >= 0 && target < MODE6_LORE_MAX && target < this.mode6LoreLines.size()) {
            this.mode6LoreRainbow[target] = value;
        } else {
            this.mode6NameRainbow = value;
        }
    }

    /** 切换输入槽时同步字体按钮状态：新槽未设置字体则显示为未勾选（自动取消）。 */

    private void mode6SyncFontTarget() {
        int line = this.mode6FocusedLoreLine();
        int target;
        if (line >= 0) {
            target = line;
        } else if (this.mode6NameEdit != null && this.mode6NameEdit.isFocused()) {
            target = -1;
        } else {
            return;
        }

        if (target != this.mode6FontTarget) {
            this.mode6FontTarget = target;
            this.mode6UpdateFontLabels();
        }
    }

    private int mode6FocusedLoreLine() {
        for (int i = 0; i < MODE6_LORE_VISIBLE; ++i) {
            EditBox box = this.mode6LoreEdits[i];
            if (box != null && box.isFocused()) {
                int line = this.mode6LoreScroll + i;
                return line < this.mode6LoreLines.size() ? line : -1;
            }

        }

        return -1;
    }

    private void mode6RefreshLoreEdits() {
        if (this.mode6LoreEdits[0] == null) {
            return;
        }

        this.mode6LoreRefreshing = true;
        for (int i = 0; i < MODE6_LORE_VISIBLE; ++i) {
            EditBox box = this.mode6LoreEdits[i];
            int line = this.mode6LoreScroll + i;
            boolean exists = line < this.mode6LoreLines.size();
            String value = exists ? this.mode6LoreLines.get(line) : "";
            if (!box.getValue().equals(value)) {
                box.setValue(value);
            }

            final int lineIndex = line;
            box.setResponder(s -> this.mode6OnLoreEdited(lineIndex, s));
            box.setEditable(exists);
        }

        this.mode6LoreRefreshing = false;
    }

    private void mode6OnLoreEdited(int line, String value) {
        if (this.mode6LoreRefreshing || line < 0 || line >= this.mode6LoreLines.size()) {
            return;
        }

        this.mode6LoreLines.set(line, value);
        if (line == this.mode6LoreLines.size() - 1 && !value.isEmpty() && this.mode6LoreLines.size() < MODE6_LORE_MAX) {
            this.mode6LoreLines.add("");
            this.mode6RefreshLoreEdits();
        }
    }

    private void mode6UpdateFontLabels() {
        if (this.mode6FontButtons[0] == null) {
            return;
        }

        boolean[] fonts = this.mode6TargetFonts();
        this.mode6FontButtons[0].setMessage(VisualCraftingScreen.mode6FontLabel("gui.visualcrafting.name.italic", fonts[0]));
        this.mode6FontButtons[1].setMessage(VisualCraftingScreen.mode6FontLabel("gui.visualcrafting.name.bold", fonts[1]));
        this.mode6FontButtons[2].setMessage(VisualCraftingScreen.mode6FontLabel("gui.visualcrafting.name.underline", fonts[2]));
        this.mode6FontButtons[3].setMessage(VisualCraftingScreen.mode6FontLabel("gui.visualcrafting.name.strike", fonts[3]));
    }

    private static Component mode6FontLabel(String key, boolean active) {
        return Component.literal(active ? "☑ " : "☐ ").append(Component.translatable(key));
    }

    private void onMode6GlowToggle() {
        this.mode6Glow = !this.mode6Glow;
        this.mode6UpdateGlowLabel();
    }

    private void mode6UpdateGlowLabel() {
        if (this.mode6BtnGlow != null) {
            this.mode6BtnGlow.setMessage(VisualCraftingScreen.mode6FontLabel("gui.visualcrafting.name.glow", this.mode6Glow));
        }
    }

    void renderNameExtras(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.mode6SyncFontTarget();
        this.renderSlotOutline(guiGraphics, 0);
        if (this.mode6PickerOpen) {
            return;
        }

        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.name.label.name").getString(), this.leftPos + 62, this.topPos + 17, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.name.label.lore").getString(), this.leftPos + 62, this.topPos + 37, 0x404040, false);
        this.renderNameSwatch(guiGraphics, mouseX, mouseY, this.leftPos + 82, this.topPos + 13, this.mode6NameColor, this.mode6NameRainbow);
        for (int i = 0; i < MODE6_LORE_VISIBLE; ++i) {
            int line = this.mode6LoreScroll + i;
            if (line >= this.mode6LoreLines.size()) {
                break;
            }

            this.renderNameSwatch(guiGraphics, mouseX, mouseY, this.leftPos + 82, this.topPos + 33 + i * 18, this.mode6LoreColors[line], this.mode6LoreRainbow[line]);
        }

        this.renderNameLoreScrollBar(guiGraphics);
    }

    private void renderNameLoreScrollBar(GuiGraphics guiGraphics) {
        int total = this.mode6LoreLines.size();
        if (total <= MODE6_LORE_VISIBLE) {
            return;
        }

        int trackX = this.leftPos + 212;
        int trackY = this.topPos + 33;
        int trackH = MODE6_LORE_VISIBLE * 18 - 2;
        guiGraphics.fill(trackX, trackY, trackX + 3, trackY + trackH, 0x40FFFFFF);
        int max = total - MODE6_LORE_VISIBLE;
        int thumbH = Math.max(8, trackH * MODE6_LORE_VISIBLE / total);
        int thumbY = trackY + (trackH - thumbH) * this.mode6LoreScroll / max;
        guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFFAAAAAA);
    }

    private void renderNameSwatch(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int color, boolean rainbow) {
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF2A2A2A);
        if (rainbow) {
            for (int i = 0; i < 12; ++i) {
                guiGraphics.fill(x + 2 + i, y + 2, x + 3 + i, y + 14, 0xFF000000 | VisualCraftingScreen.rainbowColor(i, 12));
            }

        } else if (color >= 0) {
            guiGraphics.fill(x + 2, y + 2, x + 14, y + 14, 0xFF000000 | (color & 0xFFFFFF));
        }

        if (!rainbow) {
            guiGraphics.drawString(this.font, "&", x + 5, y + 4, 0xFFFFFFFF, false);
        }

        boolean hovered = VisualCraftingScreen.inRect(mouseX, mouseY, x, y, 16, 16);
        guiGraphics.renderOutline(x, y, 16, 16, hovered ? 0xFFFFFFFF : 0xFF808080);
    }

    private void renderNamePicker(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int px = this.leftPos + 36;
        int py = this.topPos + 30;
        guiGraphics.fill(px, py, px + 188, py + 172, 0xFF101010);
        guiGraphics.renderOutline(px, py, 188, 172, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.name.picker.title").getString(), px + 8, py + 8, 0xFFFFFF, false);
        for (int i = 0; i < VisualCraftingScreen.NAME_PALETTE.length; ++i) {
            int sx = px + 8 + (i % 8) * 18;
            int sy = py + 24 + (i / 8) * 18;
            int color = VisualCraftingScreen.NAME_PALETTE[i];
            guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF000000 | color);
            boolean hovered = VisualCraftingScreen.inRect(mouseX, mouseY, sx, sy, 16, 16);
            int border = color == (this.mode6PickerColor & 0xFFFFFF) ? 0xFFFFFF00 : (hovered ? 0xFFFFFFFF : 0xFF555555);
            guiGraphics.renderOutline(sx, sy, 16, 16, border);
        }

        boolean pickerRainbow = this.mode6RainbowFor(this.mode6PickerTarget);
        if (pickerRainbow) {
            for (int i = 0; i < 16; ++i) {
                guiGraphics.fill(px + 8 + i, py + 120, px + 9 + i, py + 136, 0xFF000000 | VisualCraftingScreen.rainbowColor(i, 16));
            }

        } else {
            guiGraphics.fill(px + 8, py + 120, px + 24, py + 136, 0xFF000000 | (this.mode6PickerColor & 0xFFFFFF));
        }

        guiGraphics.renderOutline(px + 8, py + 120, 16, 16, 0xFFFFFFFF);
        this.drawNamePickerButton(guiGraphics, mouseX, mouseY, px + 8, py + 140, 62, 16, VisualCraftingScreen.mode6FontLabel("gui.visualcrafting.name.rainbow", pickerRainbow).getString());
        this.drawNamePickerButton(guiGraphics, mouseX, mouseY, px + 104, py + 140, 20, 16, Component.translatable("gui.visualcrafting.name.picker.none").getString());
        this.drawNamePickerButton(guiGraphics, mouseX, mouseY, px + 126, py + 140, 28, 16, Component.translatable("gui.visualcrafting.name.picker.ok").getString());
        this.drawNamePickerButton(guiGraphics, mouseX, mouseY, px + 156, py + 140, 28, 16, Component.translatable("gui.visualcrafting.name.picker.cancel").getString());
        if (pickerRainbow) {
            guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.name.rainbow.hint").getString(), px + 8, py + 160, 0xFFBBBBBB, false);
        }
    }

    private void drawNamePickerButton(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height, String label) {
        boolean hovered = VisualCraftingScreen.inRect(mouseX, mouseY, x, y, width, height);
        guiGraphics.fill(x, y, x + width, y + height, hovered ? 0xFF3A3A3A : 0xFF2A2A2A);
        guiGraphics.renderOutline(x, y, width, height, 0xFFAAAAAA);
        guiGraphics.drawString(this.font, label, x + (width - this.font.width(label)) / 2, y + 4, 0xFFFFFF, false);
    }

    private boolean mode6SwatchClick(double mouseX, double mouseY) {
        if (VisualCraftingScreen.inRect(mouseX, mouseY, this.leftPos + 82, this.topPos + 13, 16, 16)) {
            this.mode6OpenPicker(-1);
            return true;
        }

        for (int i = 0; i < MODE6_LORE_VISIBLE; ++i) {
            int line = this.mode6LoreScroll + i;
            if (line >= this.mode6LoreLines.size()) {
                break;
            }

            if (VisualCraftingScreen.inRect(mouseX, mouseY, this.leftPos + 82, this.topPos + 33 + i * 18, 16, 16)) {
                this.mode6OpenPicker(line);
                return true;
            }

        }

        return false;
    }

    private boolean mode6PickerClick(double mouseX, double mouseY, int button) {
        int px = this.leftPos + 36;
        int py = this.topPos + 30;
        if (mouseX < (double) px || mouseX >= (double) (px + 188) || mouseY < (double) py || mouseY >= (double) (py + 172)) {
            return false;
        }

        if (VisualCraftingScreen.inRect(mouseX, mouseY, px + 26, py + 120, 60, 16)) {
            if (this.mode6PickerHexEdit != null) {
                this.mode6PickerHexEdit.setVisible(true);
                this.mode6PickerHexEdit.setFocused(true);
                this.setFocused(this.mode6PickerHexEdit);
            }

            return true;
        }

        for (int i = 0; i < VisualCraftingScreen.NAME_PALETTE.length; ++i) {
            int sx = px + 8 + (i % 8) * 18;
            int sy = py + 24 + (i / 8) * 18;
            if (VisualCraftingScreen.inRect(mouseX, mouseY, sx, sy, 16, 16)) {
                this.mode6SetPickerColor(VisualCraftingScreen.NAME_PALETTE[i]);
                return true;
            }

        }

        if (VisualCraftingScreen.inRect(mouseX, mouseY, px + 8, py + 140, 62, 16)) {
            this.mode6SetRainbowFor(this.mode6PickerTarget, !this.mode6RainbowFor(this.mode6PickerTarget));
            return true;
        }

        if (VisualCraftingScreen.inRect(mouseX, mouseY, px + 104, py + 140, 20, 16)) {
            this.mode6SetPickerColor(-1);
            this.mode6ApplyPicker();
            return true;
        }

        if (VisualCraftingScreen.inRect(mouseX, mouseY, px + 126, py + 140, 28, 16)) {
            this.mode6ApplyPicker();
            return true;
        }

        if (VisualCraftingScreen.inRect(mouseX, mouseY, px + 156, py + 140, 28, 16)) {
            this.mode6ClosePicker();
            return true;
        }

        return true;
    }

    private void mode6SetPickerBackdropVisible(boolean visible) {
        Button[] buttons = new Button[]{this.mode6BtnSave, this.mode6BtnDelete, this.mode6BtnConfig, this.mode6BtnGlow};
        for (Button button : buttons) {
            if (button != null) {
                button.visible = visible;
            }

        }

        if (this.mode6NameEdit != null) {
            this.mode6NameEdit.visible = visible;
        }

        for (int r = 0; r < MODE6_LORE_VISIBLE; ++r) {
            if (this.mode6LoreEdits[r] != null) {
                this.mode6LoreEdits[r].visible = visible;
            }

        }

        for (int i = 0; i < this.mode6FontButtons.length; ++i) {
            if (this.mode6FontButtons[i] != null) {
                this.mode6FontButtons[i].visible = visible;
            }

        }

        for (int i = 82; i < this.menu.slots.size(); ++i) {
            Slot slot = this.menu.slots.get(i);
            if (visible) {
                int col = (i - 82) % 9;
                int row = (i - 82) / 9;
                VisualCraftingScreen.setSlotX(slot, 8 + col * 18);
                VisualCraftingScreen.setSlotY(slot, this.imageHeight - 83 + row * 18);
            } else {
                VisualCraftingScreen.setSlotX(slot, -2000);
                VisualCraftingScreen.setSlotY(slot, -2000);
            }

        }
    }

    private void mode6OpenPicker(int target) {
        this.mode6PickerTarget = target;
        int current = target == -1 ? this.mode6NameColor : (target >= 0 && target < MODE6_LORE_MAX ? this.mode6LoreColors[target] : -1);
        this.mode6PickerColor = current >= 0 ? current : 0xFFFFFF;
        if (this.mode6PickerHexEdit != null) {
            this.mode6PickerHexEdit.setValue(String.format("#%06X", this.mode6PickerColor));
            this.mode6PickerHexEdit.setVisible(true);
        }

        this.mode6SetPickerBackdropVisible(false);
        this.mode6PickerOpen = true;
    }

    private void mode6ClosePicker() {
        this.mode6PickerOpen = false;
        this.mode6PickerTarget = -2;
        if (this.mode6PickerHexEdit != null) {
            this.mode6PickerHexEdit.setVisible(false);
        }

        this.mode6SetPickerBackdropVisible(true);
    }

    private void mode6SetPickerColor(int color) {
        this.mode6PickerColor = color < 0 ? -1 : (color & 0xFFFFFF);
        if (this.mode6PickerHexEdit != null && this.mode6PickerColor >= 0) {
            String hex = String.format("#%06X", this.mode6PickerColor);
            if (!hex.equalsIgnoreCase(this.mode6PickerHexEdit.getValue())) {
                this.mode6PickerHexEdit.setValue(hex);
            }

        }
    }

    private void mode6ApplyPicker() {
        if (this.mode6PickerTarget == -1) {
            this.mode6NameColor = this.mode6PickerColor;
        } else if (this.mode6PickerTarget >= 0 && this.mode6PickerTarget < MODE6_LORE_MAX) {
            this.mode6LoreColors[this.mode6PickerTarget] = this.mode6PickerColor;
        }

        this.mode6ClosePicker();
    }
    private static final String MODE6_SCRIPT_DIR = "kubejs/startup_scripts";
    private static final String MODE6_SCRIPT_FILENAME = "visualcrafting_name.js";
    private static final String MODE6_SCRIPT_HEADER =
            "// === VisualCrafting Name Script (auto-generated) ===\n"
            + "// 命名牌：物品名称与描述修改（startup_scripts，保存后重启游戏生效）\n"
            + "ItemEvents.modification(event => {\n";
    private static final String MODE6_SCRIPT_FOOTER = "});\n";

    private void onMode6GenerateScript(Button button) {
        ItemStack stack = this.menu.slots.get(0).getItem();
        if (stack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.name.status.need_item").getString());
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        try {
            File dir = new File(this.minecraft.gameDirectory, MODE6_SCRIPT_DIR);
            dir.mkdirs();
            File file = new File(dir, MODE6_SCRIPT_FILENAME);
            String snippet = this.buildNameSnippet(itemId);
            String old = file.exists()
                    ? VisualCraftingScreen.sanitizeScriptText(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
                    : "";
            if (!old.contains("ItemEvents.modification(")) {
                old = MODE6_SCRIPT_HEADER + MODE6_SCRIPT_FOOTER;
            }

            String updated = VisualCraftingScreen.replaceMode6Block(old, itemId, snippet);
            Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));
            this.showStatus(Component.translatable("gui.visualcrafting.name.status.saved", new Object[]{itemId}).getString()
                    + " | " + Component.translatable("gui.visualcrafting.status.restart_hint").getString());
        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Generate name script failed: " + e.getMessage(), e);
            this.showStatus(Component.translatable("gui.visualcrafting.name.status.generate_failed", new Object[]{e.getMessage()}).getString());
        }
    }

    private static String replaceMode6Block(String content, String itemId, String snippet) {
        int[] range = VisualCraftingScreen.mode6BlockRange(content, itemId);
        if (range != null) {
            return content.substring(0, range[0]) + snippet + content.substring(range[1]);
        }

        int open = content.indexOf("ItemEvents.modification(");
        int brace = open >= 0 ? content.indexOf("{\n", open) : -1;
        if (brace < 0) {
            return content + snippet;
        }

        int insertAt = brace + 2;
        return content.substring(0, insertAt) + snippet + content.substring(insertAt);
    }

    private static int[] mode6BlockRange(String content, String itemId) {
        int idx = content.indexOf("event.modify('" + itemId + "',");
        if (idx < 0) {
            return null;
        }

        int lineStart = content.lastIndexOf('\n', idx) + 1;
        if (lineStart >= 2) {
            int prevLineStart = content.lastIndexOf('\n', lineStart - 2) + 1;
            if (content.substring(prevLineStart, lineStart).trim().equals("// " + itemId)) {
                lineStart = prevLineStart;
            }

        }

        int close = content.indexOf("\n    });", idx);
        int end;
        if (close >= 0) {
            end = close + 8;
        }

        else {
            close = content.indexOf("});", idx);
            if (close < 0) {
                return null;
            }

            end = close + 3;
        }

        if (end < content.length() && content.charAt(end) == '\r') {
            ++end;
        }

        if (end < content.length() && content.charAt(end) == '\n') {
            ++end;
        }

        return new int[]{lineStart, end};
    }

    private void onMode6DeleteRecipe(Button button) {
        ItemStack stack = this.menu.slots.get(0).getItem();
        if (stack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.name.status.need_item").getString());
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        try {
            File file = new File(new File(this.minecraft.gameDirectory, MODE6_SCRIPT_DIR), MODE6_SCRIPT_FILENAME);
            if (!file.exists()) {
                this.showStatus(Component.translatable("gui.visualcrafting.name.status.file_not_found").getString());
                return;
            }

            String old = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String cleaned = VisualCraftingScreen.sanitizeScriptText(old);
            boolean hadStrayText = !cleaned.equals(old);
            boolean removed = false;
            String result = cleaned;
            int[] range = VisualCraftingScreen.mode6BlockRange(cleaned, itemId);
            if (range != null) {
                result = cleaned.substring(0, range[0]) + cleaned.substring(range[1]);
                removed = true;
            }

            if (removed || hadStrayText) {
                Files.write(file.toPath(), result.getBytes(StandardCharsets.UTF_8));
            }

            if (removed) {
                this.showStatus(Component.translatable("gui.visualcrafting.name.status.removed", new Object[]{itemId}).getString()
                        + " | " + Component.translatable("gui.visualcrafting.status.restart_hint").getString());
            }

            else if (hadStrayText) {
                this.showStatus(Component.translatable("gui.visualcrafting.name.status.cleaned").getString());
            }

            else {
                this.showStatus(Component.translatable("gui.visualcrafting.name.status.entry_not_found", new Object[]{itemId}).getString());
            }

        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Remove name script failed: " + e.getMessage(), e);
            this.showStatus(Component.translatable("gui.visualcrafting.name.status.generate_failed", new Object[]{e.getMessage()}).getString());
        }
    }

    private void onMode6Config(Button button) {
        File file = new File(this.minecraft.gameDirectory, MODE6_SCRIPT_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }

        Util.getPlatform().openFile(file);
    }

    // ===================== Mode 7: 创建物品标签页 =====================
    private static final String MODE7_SCRIPT_DIR = "kubejs/startup_scripts";
    private static final String MODE7_SCRIPT_FILENAME = "visualcrafting_items.js";
    private static final String MODE7_SCRIPT_HEADER =
            "// === VisualCrafting Item Create Script (auto-generated) ===\n"
            + "// 创建物品（startup_scripts，保存后重启游戏生效）\n"
            + "StartupEvents.registry('item', event => {\n";
    private static final String MODE7_SCRIPT_FOOTER = "});\n";

    private void initMode7Widgets() {
        int slotIdx;
        for (slotIdx = 0; slotIdx <= 81; ++slotIdx) {
            VisualCraftingScreen.setSlotX(this.menu.slots.get(slotIdx), -2000);
            VisualCraftingScreen.setSlotY(this.menu.slots.get(slotIdx), -2000);
        }

        for (slotIdx = 82; slotIdx < this.menu.slots.size(); ++slotIdx) {
            Slot slot = this.menu.slots.get(slotIdx);
            int col = (slotIdx - 82) % 9;
            int row = (slotIdx - 82) / 9;
            VisualCraftingScreen.setSlotX(slot, 8 + col * 18);
            VisualCraftingScreen.setSlotY(slot, this.imageHeight - 83 + row * 18);
        }

        int BUTTON_HEIGHT = 16;
        this.mode7BtnGenerate = Button.builder(Component.translatable("gui.visualcrafting.mode7.generate"), this::onMode7GenerateScript).pos(this.leftPos + 8, this.topPos + 12).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.mode7.generate")), BUTTON_HEIGHT).build();
        this.mode7BtnConfig = Button.builder(Component.translatable("gui.visualcrafting.config"), this::onMode7Config).pos(this.leftPos + 8, this.topPos + 31).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.config")), BUTTON_HEIGHT).build();
        this.mode7BtnTexture = Button.builder(Component.translatable("gui.visualcrafting.mode7.choose_texture"), this::onMode7ChooseTexture).pos(this.leftPos + 8, this.topPos + 50).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.mode7.choose_texture")), BUTTON_HEIGHT).build();
        this.funcButtons.add(this.addRenderableWidget(this.mode7BtnGenerate));
        this.funcButtons.add(this.addRenderableWidget(this.mode7BtnConfig));
        this.funcButtons.add(this.addRenderableWidget(this.mode7BtnTexture));
        int controlX = this.leftPos + 100;
        this.mode7TypeDropdown = new DropdownWidget(this, controlX, this.topPos + 13, 108);
        this.mode7TypeDropdown.setOptions(this.mode7TypeLabels(), 0);
        this.mode7TypeDropdown.setOnSelect(this::onMode7TypeSelect);
        this.addRenderableWidget(this.mode7TypeDropdown);
        this.mode7SubtypeDropdown = new DropdownWidget(this, controlX, this.topPos + 33, 108);
        this.mode7SubtypeDropdown.setOptions(this.mode7SubtypeLabels(this.mode7TypeIdx), 0);
        this.mode7SubtypeDropdown.setOnSelect(index -> this.mode7SubtypeIdx = index);
        this.addRenderableWidget(this.mode7SubtypeDropdown);
        this.mode7NameEdit = new EditBox(this.font, controlX, this.topPos + 53, 108, 16, Component.empty());
        this.mode7NameEdit.setMaxLength(64);
        this.addRenderableWidget(this.mode7NameEdit);
        this.mode7RegIdEdit = new EditBox(this.font, controlX, this.topPos + 73, 108, 16, Component.empty());
        this.mode7RegIdEdit.setMaxLength(64);
        this.mode7RegIdEdit.setFilter(element -> element.matches("[a-zA-Z0-9_.\\-]*"));
        this.addRenderableWidget(this.mode7RegIdEdit);
    }

    private List<String> mode7TypeLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        for (String key : MODE7_TYPES) {
            labels.add(Component.translatable(key).getString());
        }

        return labels;
    }

    private List<String> mode7SubtypeLabels(int typeIdx) {
        ArrayList<String> labels = new ArrayList<String>();
        if (typeIdx < 0 || typeIdx >= MODE7_SUBTYPES.length) {
            typeIdx = 0;
        }

        for (String key : MODE7_SUBTYPES[typeIdx]) {
            labels.add(Component.translatable(key).getString());
        }

        return labels;
    }

    private void onMode7TypeSelect(int index) {
        this.mode7TypeIdx = index;
        this.mode7SubtypeIdx = 0;
        if (this.mode7SubtypeDropdown != null) {
            this.mode7SubtypeDropdown.setOptions(this.mode7SubtypeLabels(index), 0);
        }
    }

    protected void renderMode7Extras(GuiGraphics guiGraphics) {
        int gl = this.leftPos;
        int gt = this.topPos;
        int controlX = gl + 100;
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode7.label.type"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode7.label.type")), gt + 15, 60, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode7.label.subtype"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode7.label.subtype")), gt + 35, 60, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode7.label.name"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode7.label.name")), gt + 55, 60, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode7.label.reg_id"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode7.label.reg_id")), gt + 75, 60, 4210752);
        Component texStatus;
        if (this.mode7TexturePath == null || this.mode7TexturePath.isEmpty()) {
            texStatus = Component.translatable("gui.visualcrafting.label.unselected");
        } else {
            texStatus = Component.literal(new File(this.mode7TexturePath).getName());
        }

        this.drawWrapped(guiGraphics, this.font, texStatus, gl + 8, gt + 68, 88, 8355711);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode7.label.reg_id_hint"), gl + 8, gt + 96, 168, 8355711);
    }

    private void onMode7GenerateScript(Button button) {
        String displayName = this.mode7NameEdit.getValue().trim();
        if (displayName.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode7.status.need_name").getString());
            return;
        }

        String regId = this.mode7RegIdEdit.getValue().trim();
        if (regId.isEmpty() || !VisualCraftingScreen.mode7IsValidRegId(regId)) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode7.status.invalid_reg_id").getString());
            return;
        }

        String fullId = "kubejs:" + regId;
        if (this.mode7TexturePath == null || this.mode7TexturePath.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode7.status.need_texture").getString());
            return;
        }

        try {
            File texSrc = new File(this.mode7TexturePath);
            if (!texSrc.isFile()) {
                this.showStatus(Component.translatable("gui.visualcrafting.mode7.status.need_texture").getString());
                return;
            }

            String texNs = fullId.substring(0, fullId.indexOf(':'));
            String texItem = fullId.substring(fullId.indexOf(':') + 1);
            File texDir = new File(this.minecraft.gameDirectory, "kubejs/assets/" + texNs + "/textures/item");
            texDir.mkdirs();
            File texDst = new File(texDir, texItem + ".png");
            Files.copy(texSrc.toPath(), texDst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File dir = new File(this.minecraft.gameDirectory, MODE7_SCRIPT_DIR);
            dir.mkdirs();
            File file = new File(dir, MODE7_SCRIPT_FILENAME);
            String snippet = this.buildMode7ItemSnippet(fullId, displayName);
            snippet = snippet.replaceFirst(";\n$", "\n        .texture('" + texNs + ":item/" + texItem + "');\n");
            String old = file.exists()
                    ? VisualCraftingScreen.sanitizeScriptText(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
                    : "";
            if (!old.contains("StartupEvents.registry('item',")) {
                old = MODE7_SCRIPT_HEADER + MODE7_SCRIPT_FOOTER;
            }

            String updated = VisualCraftingScreen.replaceMode7Block(old, fullId, snippet);
            Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));
            this.showStatus(Component.translatable("gui.visualcrafting.mode7.status.saved", new Object[]{fullId}).getString()
                    + " | " + Component.translatable("gui.visualcrafting.status.restart_hint").getString());
        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Generate item script failed: " + e.getMessage(), e);
            this.showStatus(Component.translatable("gui.visualcrafting.mode7.status.generate_failed", new Object[]{e.getMessage()}).getString());
        }
    }

    private void onMode7Config(Button button) {
        File file = new File(this.minecraft.gameDirectory, MODE7_SCRIPT_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }

        Util.getPlatform().openFile(file);
    }

    // ===================== Mode 8: 物品增强（属性/附魔/耐久 + 自动类型检测 + 滚动） =====================
    private static final String MODE8_SCRIPT_DIR = "kubejs/startup_scripts";
    private static final String MODE8_SCRIPT_FILENAME = "visualcrafting_enhance.js";
    private static final String MODE8_SCRIPT_HEADER = "// === VisualCrafting Enhance Script (auto-generated) ===\n"
            + "// 物品增强：属性/附魔/耐久修改（startup_scripts，保存后重启游戏生效）\n"
            + "ItemEvents.modification(event => {\n";
    private static final String MODE8_SCRIPT_FOOTER = "});\n";

    private void initMode8Widgets() {
        for (int i = 0; i <= 81; ++i) {
            VisualCraftingScreen.setSlotX(this.menu.slots.get(i), -2000);
            VisualCraftingScreen.setSlotY(this.menu.slots.get(i), -2000);
        }

        VisualCraftingScreen.setSlotX(this.menu.slots.get(81), 100);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(81), 12);
        for (int i = 82; i < this.menu.slots.size(); ++i) {
            Slot slot = this.menu.slots.get(i);
            int col = (i - 82) % 9;
            int row = (i - 82) / 9;
            VisualCraftingScreen.setSlotX(slot, 8 + col * 18);
            VisualCraftingScreen.setSlotY(slot, this.imageHeight - 83 + row * 18);
        }

        int controlX = this.leftPos + 100;
        this.mode8AttrDropdown = new DropdownWidget(this, controlX, this.topPos + MODE8_SCROLL_TOP, 108);
        this.mode8AttrDropdown.setOptions(this.mode8AttrLabels(), 0);
        this.addWidget(this.mode8AttrDropdown);
        this.mode8AttrValueEdit = new EditBox(this.font, controlX, this.topPos + MODE8_SCROLL_TOP + MODE8_ROW_H, 54, 16, Component.empty());
        this.mode8AttrValueEdit.setMaxLength(16);
        this.mode8AttrValueEdit.setFilter(s -> s.matches("[0-9.\\-]*"));
        this.addWidget(this.mode8AttrValueEdit);
        this.mode8OpDropdown = new DropdownWidget(this, controlX + 58, this.topPos + MODE8_SCROLL_TOP + MODE8_ROW_H, 82);
        this.mode8OpDropdown.setOptions(this.mode8OpLabels(), 0);
        this.addWidget(this.mode8OpDropdown);
        this.mode8SlotDropdown = new DropdownWidget(this, controlX, this.topPos + MODE8_SCROLL_TOP + MODE8_ROW_H * 2, 108);
        this.mode8SlotDropdown.setOptions(Arrays.asList(MODE8_SLOT_LABELS_CN), 0);
        this.addWidget(this.mode8SlotDropdown);
        this.mode8EnchantDropdown = new DropdownWidget(this, controlX, this.topPos + MODE8_SCROLL_TOP + MODE8_ROW_H * 3, 108);
        this.mode8EnchantDropdown.setOptions(this.mode8EnchantLabels(), 0);
        this.addWidget(this.mode8EnchantDropdown);
        this.mode8EnchantLevelEdit = new EditBox(this.font, controlX, this.topPos + MODE8_SCROLL_TOP + MODE8_ROW_H * 4, 54, 16, Component.empty());
        this.mode8EnchantLevelEdit.setMaxLength(4);
        this.mode8EnchantLevelEdit.setFilter(s -> s.matches("[0-9]*"));
        this.addWidget(this.mode8EnchantLevelEdit);
        this.mode8DurabilityEdit = new EditBox(this.font, controlX, this.topPos + MODE8_SCROLL_TOP + MODE8_ROW_H * 5, 108, 16, Component.empty());
        this.mode8DurabilityEdit.setMaxLength(9);
        this.mode8DurabilityEdit.setFilter(s -> s.matches("[0-9]*"));
        this.addWidget(this.mode8DurabilityEdit);
        this.mode8BtnGenerate = Button.builder(Component.translatable("gui.visualcrafting.mode8.generate"), this::onMode8GenerateScript).pos(this.leftPos + 8, this.topPos + 12).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.mode8.generate")), 16).build();
        this.mode8BtnConfig = Button.builder(Component.translatable("gui.visualcrafting.config"), this::onMode8Config).pos(this.leftPos + 8, this.topPos + 31).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.config")), 16).build();
        this.mode8BtnChange = Button.builder(Component.translatable("gui.visualcrafting.mode8.change"), this::onMode8Change).pos(this.leftPos + 8, this.topPos + 50).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.mode8.change")), 16).build();
        this.funcButtons.add(this.addRenderableWidget(this.mode8BtnGenerate));
        this.funcButtons.add(this.addRenderableWidget(this.mode8BtnConfig));
        this.funcButtons.add(this.addRenderableWidget(this.mode8BtnChange));
        this.mode8ScrollOffset = 0;
    }

    private List<String> mode8AttrLabels() {
        ArrayList<String> labels = new ArrayList<>();
        this.mode8AttributeIds.clear();
        try {
            for (ResourceLocation id : BuiltInRegistries.ATTRIBUTE.keySet().stream().sorted().toList()) {
                Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(id);
                if (attribute == null) continue;
                this.mode8AttributeIds.add(id.toString());
                String translated = Component.translatable(attribute.getDescriptionId()).getString();
                labels.add(translated.equals(attribute.getDescriptionId()) ? id.toString() : translated + " [" + id + "]");
            }
        } catch (Throwable e) {
            VisualCraftingScreen.logWarn("Failed to read runtime attribute registry: " + e.getMessage(), e);
        }
        if (labels.isEmpty()) {
            this.mode8AttributeIds.add(MODE8_ATTRIBUTES[0][1]);
            labels.add(MODE8_ATTR_LABELS_CN[0]);
        }
        for (int i = 0; i < MODE8_TOOL_TIER_IDS.length; ++i) {
            this.mode8AttributeIds.add(MODE8_TOOL_TIER_PREFIX + MODE8_TOOL_TIER_IDS[i]);
            labels.add(MODE8_TOOL_TIER_LABELS[i]);
        }
        return labels;
    }

    private List<String> mode8OpLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        for (String key : MODE8_ATTR_OPS) labels.add(Component.translatable(key).getString());
        return labels;
    }

    private List<String> mode8EnchantLabels() {
        ArrayList<String> labels = new ArrayList<>();
        this.mode8EnchantmentIds.clear();
        try {
            if (this.minecraft != null && this.minecraft.level != null) {
                Registry<Enchantment> registry = this.minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                for (ResourceLocation id : registry.keySet().stream().sorted().toList()) {
                    Holder<Enchantment> holder = registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
                    if (holder == null) continue;
                    this.mode8EnchantmentIds.add(id.toString());
                    labels.add(Enchantment.getFullname(holder, 1).getString() + " [" + id + "]");
                }
            }
        } catch (Throwable e) {
            VisualCraftingScreen.logWarn("Failed to read runtime enchantment registry: " + e.getMessage(), e);
        }
        if (labels.isEmpty()) {
            this.mode8EnchantmentIds.add(MODE8_ENCHANTS[0][1]);
            labels.add(MODE8_ENCHANT_LABELS_CN[0]);
        }
        return labels;
    }

    private int mode8ToolTierIndex(ItemStack stack) {
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null) return 0;
        for (Tool.Rule rule : tool.rules()) {
            Optional<TagKey<Block>> key = rule.blocks().unwrapKey();
            if (key.isEmpty()) continue;
            String path = key.get().location().getPath();
            if (!path.startsWith("incorrect_for_") || !path.endsWith("_tool")) continue;
            String tier = path.substring("incorrect_for_".length(), path.length() - "_tool".length());
            for (int i = 0; i < MODE8_TOOL_TIER_IDS.length; ++i) {
                if (MODE8_TOOL_TIER_IDS[i].equals(tier)) return i;
            }
        }
        return 0;
    }

    int mode8MaxScroll() {
        int contentHeight = MODE8_ROW_H * 6;
        int visibleHeight = this.imageHeight - 83 - MODE8_SCROLL_TOP;
        if (visibleHeight >= contentHeight) {
            return 0;
        }

        return (contentHeight - visibleHeight + MODE8_ROW_H - 1) / MODE8_ROW_H;
    }

    private void repositionMode8Widgets() {
        if (this.mode8AttrDropdown == null) {
            return;
        }

        int scroll = this.mode8ScrollOffset;
        int y0 = this.topPos + MODE8_SCROLL_TOP - scroll * MODE8_ROW_H;
        this.mode8AttrDropdown.setPosition(this.leftPos + 100, y0);
        this.mode8AttrValueEdit.setPosition(this.leftPos + 100, y0 + MODE8_ROW_H);
        this.mode8OpDropdown.setPosition(this.leftPos + 158, y0 + MODE8_ROW_H);
        this.mode8SlotDropdown.setPosition(this.leftPos + 100, y0 + MODE8_ROW_H * 2);
        this.mode8EnchantDropdown.setPosition(this.leftPos + 100, y0 + MODE8_ROW_H * 3);
        this.mode8EnchantLevelEdit.setPosition(this.leftPos + 100, y0 + MODE8_ROW_H * 4);
        this.mode8DurabilityEdit.setPosition(this.leftPos + 100, y0 + MODE8_ROW_H * 5);
    }

    private void renderMode8Widgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (this.mode8AttrDropdown == null) {
            return;
        }

        guiGraphics.flush();
        RenderSystem.disableDepthTest();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
        this.mode8AttrDropdown.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.mode8AttrValueEdit.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.mode8OpDropdown.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.mode8SlotDropdown.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.mode8EnchantDropdown.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.mode8EnchantLevelEdit.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.mode8DurabilityEdit.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.pose().popPose();
        RenderSystem.enableDepthTest();
        guiGraphics.flush();
    }

    protected void renderMode8Extras(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack runtimeStack = this.menu.slots.get(81).getItem();
        String runtimeItemId = runtimeStack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(runtimeStack.getItem()).toString();
        int registrySignature = BuiltInRegistries.ATTRIBUTE.keySet().hashCode();
        if (this.minecraft != null && this.minecraft.level != null) {
            try {
                registrySignature = 31 * registrySignature + this.minecraft.level.registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT).keySet().hashCode();
            } catch (Throwable ignored) {
            }
        }
        if (!runtimeItemId.equals(this.mode8LastItemId) || registrySignature != this.mode8LastRegistrySignature) {
            this.mode8LastItemId = runtimeItemId;
            this.mode8LastRegistrySignature = registrySignature;
            if (this.mode8AttrDropdown != null) {
                this.mode8AttrDropdown.setOptions(this.mode8AttrLabels(), 0);
            }
            if (this.mode8EnchantDropdown != null) {
                this.mode8EnchantDropdown.setOptions(this.mode8EnchantLabels(), 0);
            }
        }
        int gl = this.leftPos;
        int gt = this.topPos;
        int scroll8 = this.mode8ScrollOffset;
        int y0 = gt + MODE8_SCROLL_TOP - scroll8 * MODE8_ROW_H;
        int controlX = gl + 100;
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode8.label.attr"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode8.label.attr")), y0 + 2, 60, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode8.label.attr_value"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode8.label.attr_value")), y0 + MODE8_ROW_H + 2, 60, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode8.label.slot"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode8.label.slot")), y0 + MODE8_ROW_H * 2 + 2, 60, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode8.label.enchant"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode8.label.enchant")), y0 + MODE8_ROW_H * 3 + 2, 60, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode8.label.level"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode8.label.level")), y0 + MODE8_ROW_H * 4 + 2, 60, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode8.label.durability"), controlX - 5 - this.font.width(Component.translatable("gui.visualcrafting.mode8.label.durability")), y0 + MODE8_ROW_H * 5 + 2, 60, 4210752);
        ItemStack stack = this.menu.slots.get(81).getItem();
        this.mode8DetectedType = this.mode8DetectType(stack);
        this.renderSlotOutline(guiGraphics, 81);
        Component detect;
        if (stack.isEmpty()) {
            detect = Component.translatable("gui.visualcrafting.mode8.label.placeholder");
        } else {
            String typeKey = switch (this.mode8DetectedType) {
                case 2 -> "gui.visualcrafting.mode8.detect.equipment";
                case 3 -> "gui.visualcrafting.mode8.detect.trinket";
                case 1 -> "gui.visualcrafting.mode8.detect.item";
                default -> "gui.visualcrafting.mode8.detect.none";
            };
            detect = Component.literal(stack.getHoverName().getString()).append(" (").append(Component.translatable(typeKey)).append(")");
        }

        // 单行截断显示，避免长物品名换行后与属性区/按钮组遮挡
        guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(detect.getString(), 130), gl + 122, gt + 18, 8453920);
        if (this.mode8MaxScroll() > 0) {
            this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode8.label.scroll_hint"), gl + 8, gt + this.imageHeight - 100, 180, 8355711);
        }
    }

    private int mode8DetectType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();
        if (item instanceof ArmorItem || item instanceof TieredItem) {
            return 2;
        }

        if (this.minecraft != null && this.minecraft.level != null) {
            try {
                Registry<Item> registry = this.minecraft.level.registryAccess().registryOrThrow(Registries.ITEM);
                String[] trinketTags = new String[]{"curios:ring", "curios:necklace", "curios:charm", "curios:bracelet", "curios:belt", "curios:hands", "curios:head", "curios:body", "curios:back", "curios:feet"};
                for (String tagId : trinketTags) {
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
                    Optional<net.minecraft.core.HolderSet.Named<Item>> holders = registry.getTag(tagKey);
                    if (holders.isPresent() && holders.get().contains(registry.wrapAsHolder(item))) {
                        return 3;
                    }
                }
            } catch (Throwable t) {
                VisualCraftingScreen.logWarn("mode8 trinket detection failed: " + t.getMessage(), t);
            }
        }

        return 1;
    }

    private void onMode8GenerateScript(Button button) {
        ItemStack stack = this.menu.slots.get(81).getItem();
        if (stack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode8.status.need_item").getString());
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        try {
            File dir = new File(this.minecraft.gameDirectory, MODE8_SCRIPT_DIR);
            dir.mkdirs();
            File file = new File(dir, MODE8_SCRIPT_FILENAME);
            String snippet = this.buildMode8Snippet(itemId);
            String old = file.exists()
                    ? VisualCraftingScreen.sanitizeScriptText(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
                    : "";
            if (!old.contains("ItemEvents.modification(")) {
                old = MODE8_SCRIPT_HEADER + MODE8_SCRIPT_FOOTER;
            }

            String updated = VisualCraftingScreen.replaceMode8Block(old, itemId, snippet);
            Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));
            this.showStatus(Component.translatable("gui.visualcrafting.mode8.status.saved", new Object[]{itemId}).getString()
                    + " | " + Component.translatable("gui.visualcrafting.status.restart_hint").getString());
        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Generate enhance script failed: " + e.getMessage(), e);
            this.showStatus(Component.translatable("gui.visualcrafting.mode8.status.generate_failed", new Object[]{e.getMessage()}).getString());
        }
    }

    private void onMode8Config(Button button) {
        File file = new File(this.minecraft.gameDirectory, MODE8_SCRIPT_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }

        Util.getPlatform().openFile(file);
    }

    private void onMode8Change(Button button) {
        ItemStack stack = this.menu.slots.get(81).getItem();
        if (stack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode8.status.need_item").getString());
            return;
        }

        try {
            ItemStack modified = stack.copy();
            boolean changed = false;

            String selectedAttrId = this.mode8AttributeIds.get(Math.clamp(this.mode8AttrDropdown.getSelectedIdx(), 0, this.mode8AttributeIds.size() - 1));
            if (selectedAttrId.startsWith(MODE8_TOOL_TIER_PREFIX) && stack.get(DataComponents.TOOL) != null) {
                String tier = selectedAttrId.substring(MODE8_TOOL_TIER_PREFIX.length());
                int tierIdx = 0;
                for (int i = 0; i < MODE8_TOOL_TIER_IDS.length; ++i) {
                    if (MODE8_TOOL_TIER_IDS[i].equals(tier)) {
                        tierIdx = i;
                        break;
                    }
                }
                int currentTierIdx = this.mode8ToolTierIndex(stack);
                Tool currentTool = modified.get(DataComponents.TOOL);
                if (tierIdx != currentTierIdx) {
                    var blockRegistry = this.minecraft.level.registryAccess().registryOrThrow(Registries.BLOCK);
                    List<Tool.Rule> rules = new ArrayList<>();
                    TagKey<Block> pickaxeTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "mineable/pickaxe"));
                    TagKey<Block> incorrectTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "incorrect_for_" + MODE8_TOOL_TIER_IDS[tierIdx] + "_tool"));
                    rules.add(new Tool.Rule(blockRegistry.getOrCreateTag(pickaxeTag), Optional.of(currentTool.defaultMiningSpeed()), Optional.of(true)));
                    rules.add(new Tool.Rule(blockRegistry.getOrCreateTag(incorrectTag), Optional.empty(), Optional.of(false)));
                    modified.set(DataComponents.TOOL, new Tool(rules, currentTool.defaultMiningSpeed(), currentTool.damagePerBlock()));
                    changed = true;
                }
            }

            String durability = this.mode8DurabilityEdit.getValue().trim();
            if (!durability.isEmpty()) {
                int value = Integer.parseInt(durability);
                if (value < 0) {
                    throw new NumberFormatException("durability must be >= 0");
                }

                modified.set(DataComponents.MAX_DAMAGE, value);
                changed = true;
            }

            String attrValue = this.mode8AttrValueEdit.getValue().trim();
            if (!attrValue.isEmpty() && !selectedAttrId.startsWith(MODE8_TOOL_TIER_PREFIX)) {
                double amount = Double.parseDouble(attrValue);
                String attrId = selectedAttrId;
                String slot = MODE8_SLOTS[this.mode8SlotDropdown.getSelectedIdx()];
                int opIdx = this.mode8OpDropdown.getSelectedIdx();
                if (opIdx < 0 || opIdx >= AttributeModifier.Operation.values().length) {
                    throw new NumberFormatException("bad operation index");
                }

                Holder<Attribute> attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrId)).orElse(null);
                if (attr == null) {
                    throw new IllegalArgumentException("unknown attribute: " + attrId);
                }

                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                ResourceLocation uid = ResourceLocation.parse("visualcrafting:vc_" + itemId.replace(':', '_')
                        + "_" + attrId.replace(':', '_') + "_" + slot);
                AttributeModifier mod = new AttributeModifier(uid, amount, AttributeModifier.Operation.values()[opIdx]);
                EquipmentSlotGroup group = VisualCraftingScreen.mode8SlotGroup(slot);
                ItemAttributeModifiers current = modified.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
                ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
                    if (!entry.modifier().id().equals(uid)) {
                        builder.add(entry.attribute(), entry.modifier(), entry.slot());
                    }
                }

                builder.add(attr, mod, group);
                modified.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
                changed = true;
            }

            String level = this.mode8EnchantLevelEdit.getValue().trim();
            if (!level.isEmpty()) {
                int lvl = Integer.parseInt(level);
                if (lvl <= 0) {
                    throw new NumberFormatException("enchant level must be > 0");
                }

                String enchantId = this.mode8EnchantmentIds.get(Math.clamp(this.mode8EnchantDropdown.getSelectedIdx(), 0, this.mode8EnchantmentIds.size() - 1));
                Holder<Enchantment> ench = this.minecraft.level.registryAccess()
                        .lookup(Registries.ENCHANTMENT)
                        .flatMap(lookup -> lookup.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(enchantId))))
                        .orElse(null);
                if (ench == null) {
                    throw new IllegalArgumentException("unknown enchantment: " + enchantId);
                }

                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                        modified.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                mutable.set(ench, lvl);
                modified.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
                changed = true;
            }

            if (!changed) {
                this.showStatus(Component.translatable("gui.visualcrafting.mode8.status.apply_failed", "no input").getString());
                return;
            }

            this.menu.slots.get(81).set(modified);
            PacketDistributor.sendToServer(new ModMessages.ApplyMode8ChangePacket(this.menu.blockPos, modified), new CustomPacketPayload[0]);
            this.showStatus(Component.translatable("gui.visualcrafting.mode8.status.applied", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).getString());
        } catch (NumberFormatException e) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode8.status.apply_failed", "invalid number: " + e.getMessage()).getString());
        } catch (Exception e) {
            VisualCraftingScreen.logWarn("Apply mode8 change failed: " + e.getMessage(), e);
            this.showStatus(Component.translatable("gui.visualcrafting.mode8.status.apply_failed", new Object[]{e.getMessage()}).getString());
        }
    }

    private static EquipmentSlotGroup mode8SlotGroup(String slot) {
        switch (slot) {
            case "mainhand":
                return EquipmentSlotGroup.MAINHAND;
            case "offhand":
                return EquipmentSlotGroup.OFFHAND;
            case "head":
                return EquipmentSlotGroup.HEAD;
            case "chest":
                return EquipmentSlotGroup.CHEST;
            case "legs":
                return EquipmentSlotGroup.LEGS;
            case "feet":
                return EquipmentSlotGroup.FEET;
            default:
                return EquipmentSlotGroup.ANY;
        }
    }

    private String buildMode8Snippet(String itemId) {
        StringBuilder sb = new StringBuilder();
        sb.append("    // ").append(itemId).append("\n");
        sb.append("    event.modify('").append(itemId).append("', item => {\n");
        String durability = this.mode8DurabilityEdit.getValue().trim();
        if (!durability.isEmpty()) {
            sb.append("        item.maxDamage = ").append(durability).append(";\n");
        }

        String selectedAttrId = this.mode8AttributeIds.get(Math.clamp(this.mode8AttrDropdown.getSelectedIdx(), 0, this.mode8AttributeIds.size() - 1));
        if (selectedAttrId.startsWith(MODE8_TOOL_TIER_PREFIX)) {
            ItemStack currentStack = this.menu.slots.get(81).getItem();
            if (currentStack.get(DataComponents.TOOL) != null) {
                String tier = selectedAttrId.substring(MODE8_TOOL_TIER_PREFIX.length());
                int level = switch (tier) {
                    case "stone" -> 1;
                    case "iron" -> 2;
                    case "diamond" -> 3;
                    case "netherite" -> 4;
                    default -> 0;
                };
                sb.append("        item.modifyTier(tier => { tier.level = ").append(level).append("; });\\n");
            }
        }

        String attrValue = this.mode8AttrValueEdit.getValue().trim();
        if (!attrValue.isEmpty()) {
            String attrId = this.mode8AttributeIds.get(Math.clamp(this.mode8AttrDropdown.getSelectedIdx(), 0, this.mode8AttributeIds.size() - 1));
            String slot = MODE8_SLOTS[this.mode8SlotDropdown.getSelectedIdx()];
            String op = String.valueOf(this.mode8OpDropdown.getSelectedIdx());
            String uid = "visualcrafting:vc_" + itemId.replace(':', '_');
            sb.append("        let __vcMods = item.item().getDefaultInstance().getAttributeModifiers()\n");
            sb.append("            .withModifierAdded('").append(attrId).append("', { operation: ").append(op)
                    .append(", amount: ").append(attrValue).append(", id: '").append(uid).append("' }, '").append(slot).append("')\n");
            sb.append("            .modifiers();\n");
            sb.append("        item.setAttributeModifiersWithTooltip(__vcMods);\n");
        }

        String enchantId = this.mode8EnchantmentIds.get(Math.clamp(this.mode8EnchantDropdown.getSelectedIdx(), 0, this.mode8EnchantmentIds.size() - 1));
        String level = this.mode8EnchantLevelEdit.getValue().trim();
        if (!level.isEmpty()) {
            sb.append("        item.override('minecraft:enchantments', { levels: { '").append(enchantId).append("': ").append(level).append(" } });\n");
        }

        sb.append("    });\n");
        return sb.toString();
    }

    private static String replaceMode8Block(String content, String itemId, String snippet) {
        int[] range = VisualCraftingScreen.mode8BlockRange(content, itemId);
        if (range != null) {
            return content.substring(0, range[0]) + snippet + content.substring(range[1]);
        }

        int open = content.indexOf("ItemEvents.modification(");
        int brace = open >= 0 ? content.indexOf("{\n", open) : -1;
        if (brace < 0) {
            return content + snippet;
        }

        int insertAt = brace + 2;
        return content.substring(0, insertAt) + snippet + content.substring(insertAt);
    }

    private static int[] mode8BlockRange(String content, String itemId) {
        int idx = content.indexOf("event.modify('" + itemId + "',");
        if (idx < 0) {
            return null;
        }

        int lineStart = content.lastIndexOf('\n', idx) + 1;
        if (lineStart >= 2) {
            int prevLineStart = content.lastIndexOf('\n', lineStart - 2) + 1;
            if (content.substring(prevLineStart, lineStart).trim().equals("// " + itemId)) {
                lineStart = prevLineStart;
            }

        }

        int close = content.indexOf("\n    });", idx);
        int end;
        if (close >= 0) {
            end = close + 8;
        } else {
            close = content.indexOf("});", idx);
            if (close < 0) {
                return null;
            }

            end = close + 3;
        }

        if (end < content.length() && content.charAt(end) == '\r') {
            ++end;
        }

        if (end < content.length() && content.charAt(end) == '\n') {
            ++end;
        }

        return new int[]{lineStart, end};
    }

    private void onMode7ChooseTexture(Button button) {
        File scriptFile = null;
        try {
            String ps1 = "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n"
                    + "Add-Type -AssemblyName System.Windows.Forms\n"
                    + "$dialog = New-Object System.Windows.Forms.OpenFileDialog\n"
                    + "$dialog.Title = 'Choose Texture (.png)'\n"
                    + "$dialog.Filter = 'PNG Image (*.png)|*.png'\n"
                    + "$dialog.Multiselect = $false\n"
                    + "$dialog.InitialDirectory = [Environment]::GetFolderPath('Desktop')\n"
                    + "if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {\n"
                    + "    Write-Output $dialog.FileName\n"
                    + "}\n";
            scriptFile = new File(System.getProperty("java.io.tmpdir"), "vc_choose_texture.ps1");
            Files.write(scriptFile.toPath(), ps1.getBytes(StandardCharsets.UTF_8));
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptFile.getAbsolutePath());
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor();
            if (!output.isEmpty()) {
                File selected = new File(output);
                if (selected.isFile()) {
                    this.mode7TexturePath = selected.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            VisualCraftingScreen.logWarn("Open texture chooser failed: " + e.getMessage(), e);
            this.showStatus("选择贴图失败: " + e.getMessage());
        } finally {
            if (scriptFile != null && scriptFile.exists()) {
                scriptFile.delete();
            }
        }
    }

    private String buildMode7ItemSnippet(String fullId, String displayName) {
        StringBuilder sb = new StringBuilder();
        sb.append("    // ").append(fullId).append("\n");
        sb.append("    event.create('").append(fullId).append("'");
        if (this.mode7TypeIdx == 0) {
            String[] kjsArmorTypes = new String[]{"helmet", "chestplate", "leggings", "boots"};
            sb.append(", '").append(kjsArmorTypes[this.mode7SubtypeIdx]).append("')");
            sb.append("\n        .displayName('").append(VisualCraftingScreen.jsTextEscape(displayName)).append("')");
            sb.append("\n        .material('minecraft:iron')");
            sb.append("\n        .maxDamage(").append(MODE7_ARMOR_DURABILITY[this.mode7SubtypeIdx]).append(");");
        } else if (this.mode7TypeIdx == 1) {
            String[] kjsToolTypes = new String[]{"sword", "pickaxe", "axe", "shovel", "hoe"};
            sb.append(", '").append(kjsToolTypes[this.mode7SubtypeIdx]).append("')");
            sb.append("\n        .displayName('").append(VisualCraftingScreen.jsTextEscape(displayName)).append("')");
            sb.append("\n        .tier('iron')");
            sb.append("\n        .attackDamageBaseline(").append(MODE7_TOOL_DAMAGE[this.mode7SubtypeIdx]).append(")");
            sb.append("\n        .speedBaseline(").append(MODE7_TOOL_SPEED[this.mode7SubtypeIdx]).append(")");
            sb.append("\n        .maxDamage(250);");
        } else {
            sb.append(")");
            sb.append("\n        .displayName('").append(VisualCraftingScreen.jsTextEscape(displayName)).append("')");
            sb.append("\n        .maxStackSize(1)");
            sb.append("\n        .tag('visualcrafting:trinket')");
            sb.append("\n        .tag('").append(MODE7_TRINKET_TAGS[this.mode7SubtypeIdx]).append("');");
        }

        sb.append("\n");
        return sb.toString();
    }

    private static boolean mode7IsValidRegId(String regId) {
        if (regId == null || regId.isEmpty()) {
            return false;
        }

        if (regId.indexOf(':') >= 0) {
            return false;
        }

        return VisualCraftingScreen.mode7Matches(regId);
    }

    private static boolean mode7Matches(String part) {
        for (int i = 0; i < part.length(); ++i) {
            char c = part.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-';
            if (!ok) {
                return false;
            }

        }

        return true;
    }

    private static String replaceMode7Block(String content, String fullId, String snippet) {
        int idx = content.indexOf("event.create('" + fullId + "',");
        if (idx < 0) {
            idx = content.indexOf("event.create('" + fullId + "')");
        }

        if (idx >= 0) {
            int lineStart = content.lastIndexOf('\n', idx) + 1;
            if (lineStart >= 2) {
                int prevLineStart = content.lastIndexOf('\n', lineStart - 2) + 1;
                if (content.substring(prevLineStart, lineStart).trim().equals("// " + fullId)) {
                    lineStart = prevLineStart;
                }

            }

            int end = content.indexOf(";\n", idx);
            int endLine = end >= 0 ? end + 2 : content.length();
            return content.substring(0, lineStart) + snippet + content.substring(endLine);
        }

        int open = content.indexOf("StartupEvents.registry('item',");
        int brace = open >= 0 ? content.indexOf("{\n", open) : -1;
        if (brace < 0) {
            return content + snippet;
        }

        int insertAt = brace + 2;
        return content.substring(0, insertAt) + snippet + content.substring(insertAt);
    }

    private String buildNameSnippet(String itemId) {
        StringBuilder sb = new StringBuilder();
        sb.append("    // ").append(itemId).append("\n");
        sb.append("    event.modify('").append(itemId).append("', item => {\n");
        sb.append("        item.set('minecraft:custom_name', ")
                .append(this.mode6ComponentExpr(this.mode6NameInput, this.mode6NameColor, this.mode6NameFonts, this.mode6NameRainbow))
                .append(");\n");
        ArrayList<String> lore = new ArrayList<String>();
        for (int r = 0; r < this.mode6LoreLines.size(); ++r) {
            String line = this.mode6LoreLines.get(r);
            line = line == null ? "" : line.trim();
            if (line.isEmpty()) {
                continue;
            }

            lore.add(this.mode6ComponentExpr(line, this.mode6LoreColors[r], this.mode6LoreFonts[r], this.mode6LoreRainbow[r]));
        }

        if (!lore.isEmpty()) {
            sb.append("        item.set('minecraft:lore', [").append(String.join(", ", lore)).append("]);\n");
        }

        if (this.mode6Glow) {
            sb.append("        item.set('minecraft:enchantment_glint_override', true);\n");
        }

        sb.append("    });\n");
        return sb.toString();
    }

    private String mode6ComponentExpr(String text, int color, boolean[] fonts, boolean rainbow) {
        if (rainbow) {
            return this.mode6RainbowComponentExpr(text, fonts);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Component.literal('").append(VisualCraftingScreen.jsTextEscape(text)).append("')");
        if (color >= 0) {
            sb.append(".color('").append(String.format("#%06X", color & 0xFFFFFF)).append("')");
        }

        if (fonts[1]) {
            sb.append(".bold(true)");
        }

        sb.append(".italic(").append(fonts[0]).append(")");
        if (fonts[2]) {
            sb.append(".underlined(true)");
        }

        if (fonts[3]) {
            sb.append(".strikethrough(true)");
        }

        return sb.toString();
    }

    /** 彩虹色组件：逐字符按色相均分染色，字体样式统一挂在父组件上。 */

    private String mode6RainbowComponentExpr(String text, boolean[] fonts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Component.empty()");
        if (fonts[1]) {
            sb.append(".bold(true)");
        }

        sb.append(".italic(").append(fonts[0]).append(")");
        if (fonts[2]) {
            sb.append(".underlined(true)");
        }

        if (fonts[3]) {
            sb.append(".strikethrough(true)");
        }

        String source = text == null ? "" : text;
        int count = source.codePointCount(0, source.length());
        int index = 0;
        for (int i = 0; i < count; ++i) {
            int codePoint = source.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            sb.append(".append(Component.literal('").append(VisualCraftingScreen.jsTextEscape(source.substring(index, next)))
                    .append("').color('").append(String.format("#%06X", VisualCraftingScreen.rainbowColor(i, count))).append("'))");
            index = next;
        }

        return sb.toString();
    }

    /** 彩虹色：按字符序号在 360° 色相上均分取色。 */

    static int rainbowColor(int index, int count) {
        float hue = count <= 1 ? 0.0F : 360.0F * (float) index / (float) count;
        return VisualCraftingScreen.hslToRgb(hue, 1.0F, 0.5F);
    }

    private static int hslToRgb(float hue, float saturation, float lightness) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 360.0F;
        float q = lightness < 0.5F ? lightness * (1.0F + saturation) : lightness + saturation - lightness * saturation;
        float p = 2.0F * lightness - q;
        int r = Math.round(VisualCraftingScreen.hueToChannel(p, q, h + 1.0F / 3.0F) * 255.0F);
        int g = Math.round(VisualCraftingScreen.hueToChannel(p, q, h) * 255.0F);
        int b = Math.round(VisualCraftingScreen.hueToChannel(p, q, h - 1.0F / 3.0F) * 255.0F);
        return (r << 16) | (g << 8) | b;
    }

    private static float hueToChannel(float p, float q, float t) {
        float value = t;
        if (value < 0.0F) {
            value += 1.0F;
        }

        if (value > 1.0F) {
            value -= 1.0F;
        }

        if (value < 1.0F / 6.0F) {
            return p + (q - p) * 6.0F * value;
        }

        if (value < 1.0F / 2.0F) {
            return q;
        }

        if (value < 2.0F / 3.0F) {
            return p + (q - p) * (2.0F / 3.0F - value) * 6.0F;
        }

        return p;
    }

    private static String jsTextEscape(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }

        }

        return sb.toString();
    }

    private static String sanitizeScriptText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("【来自手机的消息】", "")
                .replace("【来自电脑的消息】", "")
                .replace("【来自移动端的消息】", "")
                .replace("【来自手机消息】", "");
    }

    private static int parseHexColor(String text) {
        if (text == null) {
            return -1;
        }

        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        if (hex.length() != 6) {
            return -1;
        }

        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        }

        catch (Exception e) {
            return -1;
        }
    }

    private static boolean inRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= (double) x && mouseX < (double) (x + width) && mouseY >= (double) y && mouseY < (double) (y + height);
    }

    private void initMode2Widgets() {
        int slotIdx;
        for (slotIdx = 0; slotIdx <= 81; ++slotIdx) {
            VisualCraftingScreen.setSlotX(this.menu.slots.get(slotIdx), -2000);
            VisualCraftingScreen.setSlotY(this.menu.slots.get(slotIdx), -2000);
        }

        for (slotIdx = 82; slotIdx < this.menu.slots.size(); ++slotIdx) {
            Slot slot = this.menu.slots.get(slotIdx);
            int col = (slotIdx - 82) % 9;
            int row = (slotIdx - 82) / 9;
            VisualCraftingScreen.setSlotX(slot, 8 + col * 18);
            VisualCraftingScreen.setSlotY(slot, this.imageHeight - 83 + row * 18);
        }

        this.loadMode2Data();
        // ===== Mode2 compact layout (baseline: bak_20260723_GUI_OK) =====
        // Left column: three WrappableButtons 54x16 at topPos+12/+31/+50
        this.mode2BtnMineralGen = new WrappableButton(this.leftPos + 8, this.topPos + 12, 54, 16, Component.translatable("gui.visualcrafting.mode2.mineral_gen"), this::onMode2MineralGen);
        this.mode2BtnBanGen = new WrappableButton(this.leftPos + 8, this.topPos + 31, 54, 16, Component.translatable("gui.visualcrafting.mode2.ban_gen"), this::onMode2BanGen);
        this.mode2BtnConfig = new WrappableButton(this.leftPos + 8, this.topPos + 50, 54, 16, Component.translatable("gui.visualcrafting.config"), this::onMode2Config);
        this.funcButtons.add(this.addRenderableWidget(this.mode2BtnMineralGen));
        this.funcButtons.add(this.addRenderableWidget(this.mode2BtnBanGen));
        this.funcButtons.add(this.addRenderableWidget(this.mode2BtnConfig));
        // Right column: dimension + biome dropdowns, 116 wide, start at leftPos+67+mode2OffsetX
        int dropX = this.leftPos + 67 + this.mode2OffsetX;
        int dropW = 116;
        ArrayList<String> arrayList = new ArrayList<String>();
        if (this.mode2DataPending) {
            arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.loading"));
        } else {
            if (this.mode2BiomesByDim.containsKey("__all__")) {
                arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.all_dimensions"));
            }

            for (ResourceLocation dimension : this.mode2Dimensions) {
                String translationKey = "generator." + dimension.getNamespace() + "." + dimension.getPath();
                String displayName = Language.getInstance().getOrDefault(translationKey);
                if ((displayName).equals(translationKey)) {
                    displayName = dimension.getPath();
                }

                arrayList.add(displayName);
            }
        }

        this.mode2Dropdown = new DropdownWidget(this, dropX, this.topPos + 6, dropW);
        this.mode2Dropdown.setOptions(arrayList, this.mode2DimIdx);
        this.mode2Dropdown.setOnSelect(n -> {
            this.mode2DimIdx = n;
            this.syncBiomesFromDim();
            this.saveMode2Config();
            this.mode2BiomeSelectedIndices.clear();
            this.updateMode2ButtonLabels();
        });
        this.addRenderableWidget(this.mode2Dropdown);
        ArrayList arrayList2 = new ArrayList();
        if (this.mode2DataPending) {
            arrayList2.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.loading"));
        } else {
            if (!this.mode2Biomes.isEmpty()) {
                arrayList2.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.all_biomes"));
            }

            for (ResourceLocation resourceLocation : this.mode2Biomes) {
                String translationKey = "biome." + resourceLocation.getNamespace() + "." + resourceLocation.getPath();
                String displayName = Language.getInstance().getOrDefault(translationKey);
                if (displayName.equals(translationKey)) {
                    displayName = resourceLocation.getPath();
                }

                arrayList2.add(displayName);
            }
        }

        this.mode2BiomeDropdown = new DropdownWidget(this, dropX, this.topPos + 24, dropW);
        this.mode2BiomeDropdown.setMultiselect(true);
        this.mode2BiomeDropdown.setOptions(arrayList2, 0);
        this.mode2BiomeDropdown.setSelectedIndices(this.mode2BiomeSelectedIndices);
        this.mode2BiomeDropdown.setOnSelect(n -> this.onMode2BiomeAllClicked(n));
        this.addRenderableWidget(this.mode2BiomeDropdown);
        // Height-range (layer) edit boxes: 36x16, x=leftPos+99/+147+mode2OffsetX, y=topPos+102
        this.mode2MinYEdit = new EditBox(this.font, this.leftPos + 99 + this.mode2OffsetX, this.topPos + 102, 36, 16, Component.empty());
        this.mode2MinYEdit.setValue(String.valueOf(this.mode2MinY));
        this.mode2MinYEdit.setFilter(element -> element.isEmpty() || element.matches("-?\\d{0,3}"));
        this.mode2MinYEdit.setResponder(element -> {
            if (!element.isEmpty() && !element.equals("-")) {
                try {
                    int value = Integer.parseInt(element);
                    if (value < -63 || value > 319) {
                        this.mode2MinYEdit.setValue("");
                        return;
                    }

                    this.mode2MinY = value;
                } catch (NumberFormatException e) {
                    this.mode2MinYEdit.setValue("");
                }

            } else {
                this.mode2MinY = -63;
            }

        });
        this.addRenderableWidget(this.mode2MinYEdit);
        this.mode2MaxYEdit = new EditBox(this.font, this.leftPos + 147 + this.mode2OffsetX, this.topPos + 102, 36, 16, Component.empty());
        this.mode2MaxYEdit.setValue(String.valueOf(this.mode2MaxY));
        this.mode2MaxYEdit.setFilter(element -> element.isEmpty() || element.matches("-?\\d{0,3}"));
        this.mode2MaxYEdit.setResponder(element -> {
            if (!element.isEmpty() && !element.equals("-")) {
                try {
                    int value = Integer.parseInt(element);
                    if (value < -63 || value > 319) {
                        this.mode2MaxYEdit.setValue("");
                        return;
                    }

                    this.mode2MaxY = value;
                } catch (NumberFormatException e) {
                    this.mode2MaxYEdit.setValue("");
                }

            } else {
                this.mode2MaxY = 319;
            }

        });
        this.addRenderableWidget(this.mode2MaxYEdit);
        // Two 18x18 rows: mineral slot (y=42), byproduct slot (y=70); slot x=leftPos+71+mode2OffsetX
        VisualCraftingScreen.setSlotX(this.menu.slots.get(0), 71 + this.mode2OffsetX);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(0), 42);
        VisualCraftingScreen.setSlotX(this.menu.slots.get(1), 71 + this.mode2OffsetX);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(1), 70);
        // Amount count edit boxes 22x14: mineral row y=topPos+42 (x=111/147+mode2OffsetX), byproduct row y=topPos+76 (x=111/148+mode2OffsetX)
        this.mode2MineralCountMinEdit = new EditBox(this.font, this.leftPos + 111 + this.mode2OffsetX, this.topPos + 42, 22, 14, Component.empty());
        this.mode2MineralCountMinEdit.setValue(String.valueOf(this.mode2MineralCountMin));
        this.mode2MineralCountMinEdit.setFilter(element -> element.isEmpty() || element.matches("\\d{0,3}"));
        this.mode2MineralCountMinEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode2MineralCountMin = 1;
            } else {
                try {
                    int parsedMin = Integer.parseInt(element);
                    int clampedMin = Math.clamp(parsedMin, 1, 256);
                    this.mode2MineralCountMin = clampedMin;
                    if (parsedMin != clampedMin) {
                        this.mode2MineralCountMinEdit.setValue(String.valueOf(clampedMin));
                    }
                } catch (Exception e) {
                    VisualCraftingScreen.logWarn("Failed to parse mode2MineralCountMin", null);
                }

            }

        });
        this.addRenderableWidget(this.mode2MineralCountMinEdit);
        this.mode2MineralCountMaxEdit = new EditBox(this.font, this.leftPos + 147 + this.mode2OffsetX, this.topPos + 42, 22, 14, Component.empty());
        this.mode2MineralCountMaxEdit.setValue(String.valueOf(this.mode2MineralCountMax));
        this.mode2MineralCountMaxEdit.setFilter(element -> element.isEmpty() || element.matches("\\d{0,3}"));
        this.mode2MineralCountMaxEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode2MineralCountMax = 3;
            } else {
                try {
                    int parsedMax = Integer.parseInt(element);
                    int clampedMax = Math.clamp(parsedMax, 1, 256);
                    this.mode2MineralCountMax = clampedMax;
                    if (parsedMax != clampedMax) {
                        this.mode2MineralCountMaxEdit.setValue(String.valueOf(clampedMax));
                    }
                } catch (Exception e) {
                    VisualCraftingScreen.logWarn("Failed to parse mode2MineralCountMax", null);
                }

            }

        });
        this.addRenderableWidget(this.mode2MineralCountMaxEdit);
        this.mode2ByproductCountMinEdit = new EditBox(this.font, this.leftPos + 111 + this.mode2OffsetX, this.topPos + 76, 22, 14, Component.empty());
        this.mode2ByproductCountMinEdit.setValue(String.valueOf(this.mode2ByproductCountMin));
        this.mode2ByproductCountMinEdit.setFilter(element -> element.isEmpty() || element.matches("\\d{0,3}"));
        this.mode2ByproductCountMinEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode2ByproductCountMin = 1;
            } else {
                try {
                    int parsedBMin = Integer.parseInt(element);
                    int clampedBMin = Math.clamp(parsedBMin, 1, 256);
                    this.mode2ByproductCountMin = clampedBMin;
                    if (parsedBMin != clampedBMin) {
                        this.mode2ByproductCountMinEdit.setValue(String.valueOf(clampedBMin));
                    }
                } catch (Exception e) {
                    VisualCraftingScreen.logWarn("Failed to parse mode2ByproductCountMin", null);
                }

            }

        });
        this.addRenderableWidget(this.mode2ByproductCountMinEdit);
        this.mode2ByproductCountMaxEdit = new EditBox(this.font, this.leftPos + 148 + this.mode2OffsetX, this.topPos + 76, 22, 14, Component.empty());
        this.mode2ByproductCountMaxEdit.setValue(String.valueOf(this.mode2ByproductCountMax));
        this.mode2ByproductCountMaxEdit.setFilter(element -> element.isEmpty() || element.matches("\\d{0,3}"));
        this.mode2ByproductCountMaxEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode2ByproductCountMax = 2;
            } else {
                try {
                    int parsedBMax = Integer.parseInt(element);
                    int clampedBMax = Math.clamp(parsedBMax, 1, 256);
                    this.mode2ByproductCountMax = clampedBMax;
                    if (parsedBMax != clampedBMax) {
                        this.mode2ByproductCountMaxEdit.setValue(String.valueOf(clampedBMax));
                    }
                } catch (Exception e) {
                    VisualCraftingScreen.logWarn("Failed to parse mode2ByproductCountMax", null);
                }

            }

        });
        this.addRenderableWidget(this.mode2ByproductCountMaxEdit);
        this.mode2WidgetsInited = true;
        this.reverseParseOreGenFiles();
        this.updateMode2ButtonLabels();
        this.updateMode2ButtonStates();
    }

    private void initMode5Widgets() {
        int slotIdx;
        for (slotIdx = 0; slotIdx <= 81; ++slotIdx) {
            VisualCraftingScreen.setSlotX(this.menu.slots.get(slotIdx), -2000);
            VisualCraftingScreen.setSlotY(this.menu.slots.get(slotIdx), -2000);
        }

        for (slotIdx = 82; slotIdx < this.menu.slots.size(); ++slotIdx) {
            Slot slot = this.menu.slots.get(slotIdx);
            int col = (slotIdx - 82) % 9;
            int row = (slotIdx - 82) / 9;
            VisualCraftingScreen.setSlotX(slot, 8 + col * 18);
            VisualCraftingScreen.setSlotY(slot, this.imageHeight - 83 + row * 18);
        }

        this.loadMode5Potions();
        int BUTTON_HEIGHT = 16;
        this.mode5BtnSave = Button.builder(Component.translatable("gui.visualcrafting.mode5.generate"), this::onMode5GenerateScript).pos(this.leftPos + 8, this.topPos + 12).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.mode5.generate")), BUTTON_HEIGHT).build();
        this.mode5BtnDelete = Button.builder(Component.translatable("gui.visualcrafting.delete_recipe"), this::onMode5DeleteRecipe).pos(this.leftPos + 8, this.topPos + 31).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.delete_recipe")), BUTTON_HEIGHT).build();
        this.mode5BtnConfig = Button.builder(Component.translatable("gui.visualcrafting.config"), this::onMode5Config).pos(this.leftPos + 8, this.topPos + 50).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.config")), BUTTON_HEIGHT).build();
        this.mode5BtnAddEffect = Button.builder(Component.translatable("gui.visualcrafting.mode5.add_effect"), this::onMode5AddEffect).pos(this.leftPos + 8, this.topPos + 69).size(this.autoButtonWidth(Component.translatable("gui.visualcrafting.mode5.add_effect")), BUTTON_HEIGHT).build();
        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnSave));
        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnDelete));
        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnConfig));
        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnAddEffect));
        VisualCraftingScreen.setSlotX(this.menu.slots.get(0), 94);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(0), 17);
        VisualCraftingScreen.setSlotX(this.menu.slots.get(1), 94);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(1), 40);
        this.mode5HungerEdit = new EditBox(this.font, this.leftPos + 144, this.topPos + 18, 24, 16, Component.empty());
        this.mode5HungerEdit.setFilter(element -> element.isEmpty() || element.matches("\\d{0,4}"));
        this.mode5HungerEdit.setValue(String.valueOf(this.mode5Hunger));
        this.mode5HungerEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode5Hunger = 4;
            } else {
                try {
                    int val = Integer.parseInt(element);
                    if (val < 0) {
                        this.mode5HungerEdit.setValue("");
                        return;
                    }

                    this.mode5Hunger = val;
                }

                catch (Exception e) {
                    this.mode5HungerEdit.setValue("");
                }

            }

        });
        this.addRenderableWidget(this.mode5HungerEdit);
        this.mode5SaturationEdit = new EditBox(this.font, this.leftPos + 204, this.topPos + 18, 24, 16, Component.empty());
        this.mode5SaturationEdit.setFilter(element -> element.isEmpty() || element.matches("\\d*\\.?\\d{0,2}"));
        this.mode5SaturationEdit.setValue(String.valueOf(this.mode5Saturation));
        this.mode5SaturationEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode5Saturation = 0.3f;
            } else {
                try {
                    float f = Float.parseFloat(element);
                    if (f < 0.0f) {
                        this.mode5SaturationEdit.setValue("");
                        return;
                    }

                    this.mode5Saturation = f;
                }

                catch (Exception e) {
                    this.mode5SaturationEdit.setValue("");
                }

            }

        });
        this.addRenderableWidget(this.mode5SaturationEdit);
        this.mode5PotionDropdown = new DropdownWidget(this, this.leftPos + 92, this.topPos + 64, 80);
        // 单选：每次只配置一个效果，单次添加
        this.mode5PotionDropdown.setOnSelect(this::onMode5PotionSelect);
        this.mode5PotionDropdown.setOptions(this.mode5PotionNames, 0);
        this.addRenderableWidget(this.mode5PotionDropdown);
        this.mode5PotionLevelEdit = new EditBox(this.font, this.leftPos + 204, this.topPos + 64, 24, 16, Component.empty());
        this.mode5PotionLevelEdit.setFilter(element -> element.isEmpty() || element.matches("\\d{0,3}"));
        this.mode5PotionLevelEdit.setValue(String.valueOf(this.mode5PotionLevel));
        this.mode5PotionLevelEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode5PotionLevel = 1;
            } else {
                try {
                    int val2 = Integer.parseInt(element);
                    if (val2 < 0 || val2 > 255) {
                        this.mode5PotionLevelEdit.setValue("");
                        return;
                    }

                    this.mode5PotionLevel = val2;
                }

                catch (Exception e) {
                    this.mode5PotionLevelEdit.setValue("");
                }

            }

        });
        this.addRenderableWidget(this.mode5PotionLevelEdit);
        this.mode5DurationEdit = new EditBox(this.font, this.leftPos + 144, this.topPos + 41, 24, 16, Component.empty());
        this.mode5DurationEdit.setFilter(element -> element.isEmpty() || element.matches("\\d{0,5}"));
        this.mode5DurationEdit.setValue(this.mode5DurationInfinite ? "" : String.valueOf(this.mode5Duration));
        this.mode5DurationEdit.setEditable(!this.mode5DurationInfinite);
        this.mode5DurationEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode5Duration = 30;
            } else {
                try {
                    int val3 = Integer.parseInt(element);
                    if (val3 < 0) {
                        this.mode5DurationEdit.setValue("");
                        return;
                    }

                    this.mode5Duration = val3;
                }

                catch (Exception e) {
                    this.mode5DurationEdit.setValue("");
                }

            }

        });
        this.addRenderableWidget(this.mode5DurationEdit);
        this.mode5EatSecondsEdit = new EditBox(this.font, this.leftPos + 93, this.topPos + 88, 24, 16, Component.empty());
        this.mode5EatSecondsEdit.setFilter(element -> element.isEmpty() || element.matches("\\d*\\.?\\d{0,2}"));
        this.mode5EatSecondsEdit.setValue(this.mode5EatSeconds == 0 ? "" : String.valueOf((float)this.mode5EatSeconds / 20.0f));
        this.mode5EatSecondsEdit.setResponder(element -> {
            if (element.isEmpty()) {
                this.mode5EatSeconds = 0;
            } else {
                try {
                    float f = Float.parseFloat(element);
                    this.mode5EatSeconds = Math.max(0, Math.round(f * 20.0f));
                }

                catch (Exception e) {
                    this.mode5EatSecondsEdit.setValue("");
                }

            }

        });
        this.addRenderableWidget(this.mode5EatSecondsEdit);
    }

    private void drawWrapped(GuiGraphics guiGraphics, Font font, Component component, int x, int y, int width, int color) {
        for (FormattedCharSequence formattedCharSequence : font.split(component, width)) {
            guiGraphics.drawString(font, formattedCharSequence, x, y, color, false);
            y += 9;
        }
    }

    private String buildModifySnippet(String itemId, String namespace) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  // ").append(itemId).append("\n");
        stringBuilder.append("  event.modify('").append(itemId).append("', function(item) {\n");
        stringBuilder.append("    var builder = new $FoodBuilder();\n");
        stringBuilder.append("    builder.nutrition(").append(this.mode5Hunger).append(");\n");
        stringBuilder.append("    builder.saturation(saturationFixer(").append(this.mode5Saturation).append(", ").append(this.mode5Hunger).append("));\n");
        stringBuilder.append("    builder.alwaysEdible(").append(this.mode5AlwaysEdible).append(");\n");
        if (this.mode5EatSeconds > 0) {
            stringBuilder.append("    builder.eatSeconds(").append((float)this.mode5EatSeconds / 20.0f).append(");\n");
        }

        net.minecraft.world.item.ItemStack returnStack = this.menu.slots.get(1).getItem();
        if (!returnStack.isEmpty()) {
            String convertToId = BuiltInRegistries.ITEM.getKey(returnStack.getItem()).toString();
            stringBuilder.append("    builder.usingConvertsTo(Item.of('").append(convertToId).append("'));\n");
        } else {
            stringBuilder.append("    builder.usingConvertsTo(Item.of('minecraft:air'));\n");
        }

        if (!this.mode5PreviewEffects.isEmpty()) {
            // 生成脚本遍历暂存效果列表："添加效果" 配置的每一个效果独立生成（含无限时长 -1 的处理）
            for (String[] effect : this.mode5PreviewEffects) {
                String previewName = effect[0];
                int potionLevel = Integer.parseInt(effect[1]) - 1;
                int duration;
                String durationStr = effect[2];
                if ("-1".equals(durationStr)) {
                    duration = -1;
                } else {
                    // 预览以秒为单位，生成脚本换算为 tick：tick = 秒 * 20
                    duration = Integer.parseInt(durationStr) * 20;
                }
                // 预览存的是显示名，映射回 potionId 后写入脚本
                String potionId = previewName;
                for (int i = 0; i < this.mode5PotionIds.size(); ++i) {
                    if (i < this.mode5PotionNames.size() && this.mode5PotionNames.get(i).equals(previewName)) {
                        potionId = this.mode5PotionIds.get(i);
                        break;
                    }
                }
                stringBuilder.append("    builder.effect('").append(potionId).append("', ").append(duration).append(", ").append(potionLevel).append(", 1.0);\n");
            }

        } else if (!this.mode5PotionSelectedIdx.isEmpty()) {
            java.util.Iterator<Integer> iter = this.mode5PotionSelectedIdx.iterator();
            while (iter.hasNext()) {
                int recipeIndex = iter.next();
                if (recipeIndex < 0 || recipeIndex >= this.mode5PotionIds.size()) continue;
                String potionId = this.mode5PotionIds.get(recipeIndex);
                int potionLevel = this.mode5PotionLevel - 1;
                // 界面以秒为单位，生成脚本换算为 tick：tick = 秒 * 20
                int duration = this.mode5DurationInfinite ? -1 : this.mode5Duration * 20;
                stringBuilder.append("    builder.effect('").append(potionId).append("', ").append(duration).append(", ").append(potionLevel).append(", 1.0);\n");
            }

        }

        stringBuilder.append("    item.set('food', builder.build());\n");
        stringBuilder.append("  });\n");
        return stringBuilder.toString();
    }

    private String buildFileHeader() {
        return "// === VisualCrafting Food Script (auto-generated) ===\n// KubeJS 1.21+ FoodBuilder API\n// 配合 saturationFixer 修正饱和度计算\n\nvar $FoodBuilder = Java.loadClass('dev.latvian.mods.kubejs.item.FoodBuilder');\n\nfunction saturationFixer(expectedSaturation, nutrition) {\n    return expectedSaturation / (2 * nutrition);\n}\n";
    }

    private String buildItemEventsHeader() {
        return "\nItemEvents.modification(function(event) {\n";
    }

    private String buildItemEventsFooter() {
        return "})\n";
    }

    private void onMode5GenerateScript(Button button) {
        ItemStack itemStack = this.menu.slots.get(0).getItem();
        if (itemStack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.need_item").getString());
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        String namespace = itemId.contains(":") ? itemId.substring(0, itemId.indexOf(58)) : "unknown";
        String snippet = this.buildModifySnippet(itemId, namespace);
        String commentMarker;
        try {
            String existingScript;
            File file = new File(this.minecraft.gameDirectory, "kubejs/startup_scripts");
            file.mkdirs();
            File file2 = new File(file, "visualcrafting_food.js");
            StringBuilder stringBuilder = new StringBuilder();
            if (file2.exists()) {
                existingScript = new String(Files.readAllBytes(file2.toPath()), StandardCharsets.UTF_8);
                commentMarker = "  // " + itemId + "\n";
                int commentStart = (existingScript).indexOf(commentMarker);
                if (commentStart >= 0) {
                    int skipNewlines;
                    int endPos;
                    int closeParenEnd = (existingScript).indexOf("  });\n", commentStart);
                    int nextComment = (existingScript).indexOf("  // ", commentStart + commentMarker.length());
                    if (closeParenEnd >= 0) {
                        endPos = closeParenEnd + "  });\n".length();
                    } else {
                        endPos = (existingScript).indexOf("\n})\n", commentStart);
                        if (endPos < 0) {
                            endPos = (existingScript).indexOf("})", commentStart);
                        }

                        if (endPos < 0) {
                            endPos = (existingScript).length();
                        }

                    }

                    if (nextComment >= 0 && nextComment < endPos) {
                        endPos = nextComment;
                    }

                    stringBuilder.append(existingScript, 0, commentStart);
                    stringBuilder.append(snippet);
                    for (skipNewlines = endPos; skipNewlines < (existingScript).length() && (existingScript).charAt(skipNewlines) == '\n'; ++skipNewlines) {
                    }

                    stringBuilder.append(existingScript, skipNewlines, (existingScript).length());
                } else {
                    int lastEnd = (existingScript).lastIndexOf("\n})\n");
                    if (lastEnd < 0) {
                        lastEnd = (existingScript).lastIndexOf("})");
                    }

                    if (lastEnd >= 0) {
                        stringBuilder.append(existingScript, 0, lastEnd);
                        if (!(existingScript).substring(0, lastEnd).endsWith("\n")) {
                            stringBuilder.append("\n");
                        }

                        stringBuilder.append(snippet);
                        stringBuilder.append(existingScript, lastEnd, (existingScript).length());
                    } else {
                        stringBuilder.append(existingScript);
                        if (!(existingScript).endsWith("\n")) {
                            stringBuilder.append("\n");
                        }

                        stringBuilder.append(snippet);
                    }

                }

            } else {
                stringBuilder.append(this.buildFileHeader());
                stringBuilder.append(this.buildItemEventsHeader());
                stringBuilder.append(snippet);
                stringBuilder.append(this.buildItemEventsFooter());
            }

            JsonObject scriptContent = new JsonObject();
            scriptContent.addProperty("script", stringBuilder.toString());
            this.writePending(MergeManager.TYPE_STARTUP_SCRIPTS, "visualcrafting_food.js", scriptContent);
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.saved", new Object[]{itemId}).getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());
        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Generate script failed: " + e.getMessage(), e);
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.generate_failed", new Object[]{e.getMessage()}).getString());
        }
    }

    private void onMode5DeleteRecipe(Button button) {
        ItemStack itemStack = this.menu.slots.get(0).getItem();
        if (itemStack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.need_item").getString());
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        try {
            String entryBlock;
            int closeBraceIdx;
            File file = new File(this.minecraft.gameDirectory, "kubejs/startup_scripts/visualcrafting_food.js");
            if (!file.exists()) {
                this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.file_not_found").getString());
                return;
            }

            String scriptText = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String commentMarker = "  // " + itemId + "\n";
            int commentStart = scriptText.indexOf(commentMarker);
            if (commentStart < 0) {
                this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.entry_not_found", new Object[]{itemId}).getString());
                return;
            }

            int nextCommentIdx = scriptText.indexOf("  // ", commentStart + commentMarker.length());
            int endMarkerIdx = scriptText.indexOf("\n})\n", commentStart);
            if (endMarkerIdx < 0) {
                endMarkerIdx = scriptText.indexOf("})\n", commentStart);
            }

            int endPos = nextCommentIdx >= 0 && (endMarkerIdx < 0 || nextCommentIdx < endMarkerIdx) ? nextCommentIdx : (endMarkerIdx >= 0 ? endMarkerIdx : scriptText.length());
            int closeParenIdx = scriptText.indexOf("  });\n", commentStart);
            if (closeParenIdx >= 0 && closeParenIdx < endPos) {
                endPos = closeParenIdx + "  });\n".length();
            }

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(scriptText, 0, commentStart);
            stringBuilder.append(scriptText, endPos, scriptText.length());
            String scriptWithoutEntry = stringBuilder.toString();
            int modEventIdx = scriptWithoutEntry.indexOf("ItemEvents.modification(function(event) {");
            if (modEventIdx >= 0 && (closeBraceIdx = scriptWithoutEntry.indexOf("})\n", modEventIdx)) >= 0) {
                entryBlock = scriptWithoutEntry.substring(modEventIdx + "ItemEvents.modification(function(event) {".length(), closeBraceIdx);
                boolean hasModifyEntry = false;
                for (String line : (entryBlock).split("\n")) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.startsWith("event.modify(") && (!trimmedLine.startsWith("//") || trimmedLine.startsWith("// ===") || !line.contains("  // "))) continue;
                    hasModifyEntry = true;
                    break;
                }

                if (!hasModifyEntry) {
                    int skipPos;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(scriptWithoutEntry, 0, modEventIdx);
                    for (skipPos = closeBraceIdx + "})\n".length(); skipPos < scriptWithoutEntry.length() && scriptWithoutEntry.charAt(skipPos) == '\n'; ++skipPos) {
                    }

                    stringBuilder2.append(scriptWithoutEntry, skipPos, scriptWithoutEntry.length());
                    scriptWithoutEntry = stringBuilder2.toString();
                }

            }

            scriptWithoutEntry = scriptWithoutEntry.replaceAll("\\n{3,}", "\n\n");
            JsonObject scriptContent = new JsonObject();
            scriptContent.addProperty("script", scriptWithoutEntry);
            this.writePending(MergeManager.TYPE_STARTUP_SCRIPTS, "visualcrafting_food.js", scriptContent);
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.deleted", new Object[]{itemId}).getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());
        }

        catch (Exception e) {
            VisualCraftingScreen.logWarn("Delete recipe failed: " + e.getMessage(), e);
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.delete_failed", new Object[]{e.getMessage()}).getString());
        }
    }

    private void onMode5Config(Button button) {
        File file = new File(this.minecraft.gameDirectory, "kubejs/startup_scripts");
        if (!file.exists()) {
            file.mkdirs();
        }

        Util.getPlatform().openFile(file);
    }

    private void onMode5PotionSelect(int index) {
        // 单选：每次只记录当前下拉选中的那一个效果
        this.mode5PotionSelectedIdx.clear();
        this.mode5PotionSelectedIdx.add(index);
    }

    private void onMode5AddEffect(Button button) {
        ItemStack itemStack = this.menu.slots.get(0).getItem();
        if (itemStack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.need_item").getString());
            return;
        }
        if (this.mode5PotionSelectedIdx.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.effect_no_select").getString());
            return;
        }
        // 单次添加：只添加当前配置的那一个效果（当前下拉选中项 + 当前等级 + 当前时长）
        int recipeIndex = this.mode5PotionSelectedIdx.iterator().next();
        if (recipeIndex < 0 || recipeIndex >= this.mode5PotionIds.size()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.effect_no_select").getString());
            return;
        }
        String potionId = this.mode5PotionIds.get(recipeIndex);
        String name = recipeIndex < this.mode5PotionNames.size() ? this.mode5PotionNames.get(recipeIndex) : potionId;
        String level = String.valueOf(this.mode5PotionLevel);
        // 每个暂存效果独立固化自己的名称/等级/时长/无限标志（"-1" 表示无限），不去重不替换，互不影响
        String duration = this.mode5DurationInfinite ? "-1" : String.valueOf(this.mode5Duration);
        this.mode5PreviewEffects.add(new String[]{name, level, duration});
        this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.effect_added", this.mode5PreviewEffects.size()).getString());
        // 添加后立即清空当前勾选与设置，等待配置下一个效果
        this.mode5PotionSelectedIdx.clear();
        if (this.mode5PotionDropdown != null) {
            this.mode5PotionDropdown.setSelected(0);
            this.mode5PotionDropdown.collapse();
        }
        // 重置等级与时长输入为默认值
        this.mode5PotionLevel = 1;
        if (this.mode5PotionLevelEdit != null) {
            this.mode5PotionLevelEdit.setValue("1");
        }
        this.mode5DurationInfinite = false;
        this.mode5Duration = 30;
        if (this.mode5DurationEdit != null) {
            this.mode5DurationEdit.setEditable(true);
            this.mode5DurationEdit.setValue("30");
        }
    }

}