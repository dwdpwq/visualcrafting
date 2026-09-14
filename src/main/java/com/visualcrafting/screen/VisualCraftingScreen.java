package com.visualcrafting.screen;

import com.google.gson.Gson;

import com.google.gson.GsonBuilder;

import com.google.gson.JsonArray;

import com.google.gson.JsonElement;

import com.google.gson.JsonObject;

import com.google.gson.JsonParser;

import com.mojang.blaze3d.systems.RenderSystem;

import com.visualcrafting.block.VisualCraftingBlockEntity;

import com.visualcrafting.network.DimensionBiomesData;

import com.visualcrafting.network.ModMessages;

import com.visualcrafting.merge.MergeManager;

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

import java.nio.file.attribute.FileAttribute;

import java.util.ArrayList;

import java.util.Iterator;

import java.util.LinkedHashMap;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.Properties;

import java.util.Set;

import java.util.TreeSet;
import java.util.function.IntConsumer;

import net.minecraft.Util;

import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;

import net.minecraft.client.gui.components.events.GuiEventListener;

import net.minecraft.client.gui.screens.Screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.minecraft.client.renderer.GameRenderer;

import net.minecraft.core.HolderLookup;

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

import net.minecraft.resources.ResourceLocation;

import net.minecraft.util.FormattedCharSequence;

import net.minecraft.world.effect.MobEffect;

import net.minecraft.world.effect.MobEffectInstance;

import net.minecraft.world.entity.player.Inventory;

import net.minecraft.world.food.FoodProperties;

import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.world.inventory.Slot;

import net.minecraft.world.item.Item;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.Items;

import net.minecraft.world.level.ItemLike;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.world.level.storage.LevelResource;

import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.NonNullList;

public class VisualCraftingScreen

extends AbstractContainerScreen<VisualCraftingMenu> {

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

     int gridSlotOffsetX = 11;

     int gridSlotOffsetY = 11;

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

     int outSlotSlotOffsetX = 11;

     int outSlotSlotOffsetY = 10;

     int recipesOffsetX = 0;

     int recipesOffsetY = 2;

     int tierOffsetY = 0;

    List<VisualCraftingBlockEntity.SavedRecipe> recipes = new ArrayList<VisualCraftingBlockEntity.SavedRecipe>();

    List<VisualCraftingBlockEntity.InfusingRecipe> infusingRecipes = new ArrayList<VisualCraftingBlockEntity.InfusingRecipe>();

    List<Button> tierButtons = new ArrayList<Button>();

    List<Button> funcButtons = new ArrayList<Button>();

    Button formatToggle;

     ItemStack markedContainer = ItemStack.EMPTY;

    String statusMessage = "";

    private long statusUntil = 0L;

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

    int mode5Duration = 600;

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

    boolean mode5AlwaysEdible;

    ItemStack selectedChemical = ItemStack.EMPTY;

    private final ItemStack[] ghostItems = new ItemStack[9];

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("VisualCrafting");

    public static void logWarn(String string, Throwable throwable) {

        LOGGER.warn(string, throwable);

    }

    private static void logDebug(String string) {

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

                String string = compoundTag2.getString("type");

                if ("chemical".equals(string) && compoundTag2.contains("data")) {

                    this.menu.setChemGhost(i, compoundTag2.getCompound("data"));

                    try {

                        this.ghostItems[i] = MekanismIntegration.createChemicalTagItem(compoundTag2.getCompound("data"));

                    }

                    catch (Throwable throwable) {

                        VisualCraftingScreen.logWarn("Mekanism not available, skipping chemical ghost item", throwable);

                        this.ghostItems[i] = ItemStack.EMPTY;

                    }

                    continue;

                }

                this.ghostItems[i] = "item".equals(string) && compoundTag2.contains("data") ? ItemStack.parse(this.minecraft.level.registryAccess(), compoundTag2.getCompound("data")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;

            }

        }

    }

    static void setSlotX(Slot slot, int x) {

        if (SLOT_X != null) {

            try {

                SLOT_X.setInt(slot, x);

            }

            catch (IllegalAccessException illegalAccessException) {

                VisualCraftingScreen.logWarn("Failed to reflectively set Slot.x", illegalAccessException);

            }

        }

    }

    static void setSlotY(Slot slot, int y) {

        if (SLOT_Y != null) {

            try {

                SLOT_Y.setInt(slot, y);

            }

            catch (IllegalAccessException illegalAccessException) {

                VisualCraftingScreen.logWarn("Failed to reflectively set Slot.y", illegalAccessException);

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

        int gridSize = this.getGridSize();

        this.imageWidth = 260 + (gridSize - 3) * 18;

        this.imageHeight = gridSize * 18 + 161;

        this.leftPos = (this.width - this.imageWidth) / 2;

        this.topPos = (this.height - this.imageHeight) / 2;

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

        } else {

            // 兜底：非法模式（例如旧存档里被写成 4 的值）统一回退到合成界面
            this.mode = VisualCraftingBlockEntity.MODE_CRAFTING;

            this.menu.setCurrentMode(this.mode);

            this.menu.updateSlotPositions(this.tier);

            this.initCraftingWidgets();

        }

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

            boolean bl2 = false;

            for (int i = 0; i < maxSlots; ++i) {

                ItemStack itemStack = this.menu.craftSlots.getItem(i);

                arrayList.add(itemStack.copy());

                if (itemStack.isEmpty()) continue;

                bl2 = true;

            }

            if (bl2) {

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

        int gridSize = this.getGridSize();

        this.imageWidth = 260 + (gridSize - 3) * 18;

        this.imageHeight = gridSize * 18 + 161;

        this.leftPos = (this.width - this.imageWidth) / 2;

        this.topPos = (this.height - this.imageHeight) / 2;

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

                } catch (Throwable throwable) {

                    VisualCraftingScreen.logWarn("Failed to synthesize chemical item from ghost slot data", throwable);

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

                catch (Throwable throwable) {

                    VisualCraftingScreen.logWarn("Mekanism not available, skipping chemical data extraction", throwable);

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

            String string = null;

            String string2 = null;

            int mineralVeinSize = -1;

            int mineralVeinsPerChunk = -1;

            int mineralMinY = -1;

            int mineralMaxY = -1;

            int byproductVeinSize = -63;

            int byproductMinY = 319;

            String string3 = null;

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

                        } else if (biomesElem.isJsonArray() && biomesElem.getAsJsonArray().size() > 0) {

                            biomeTag = biomesElem.getAsJsonArray().get(0).getAsString();

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

                } catch (Exception ex) {

                    logWarn("Failed to parse mineral cluster size from placed feature", null);

                }

            }

            // Parse byproduct cluster size from placed feature

            if (byproductPlacedFile.exists()) {

                try (FileReader reader = new FileReader(byproductPlacedFile, StandardCharsets.UTF_8)) {

                    JsonObject placedJson = gson.fromJson(reader, JsonObject.class);

                    byproductChance = placedJson.getAsJsonObject("config").getAsJsonObject("size").getAsInt();

                    this.mode2ByproductPct = Math.clamp((int)((double)byproductChance / 0.08), 0, 100);

                } catch (Exception ex) {

                    logWarn("Failed to parse byproduct cluster size from placed feature", null);

                }

            }

            // Apply parsed values

            if (string != null && this.minecraft != null) {

                try {

                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(string));

                    if (item != null) {

                        this.menu.slots.get(0).set(new ItemStack(item, 1));

                    }

                } catch (Exception ex) {

                    logWarn("Failed to load mode2 mineral item", ex);

                }

            }

            if (string2 != null && this.minecraft != null) {

                try {

                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(string2));

                    if (item != null) {

                        this.menu.slots.get(1).set(new ItemStack(item, 1));

                    }

                } catch (Exception ex) {

                    logWarn("Failed to load mode2 byproduct item", ex);

                }

            }

            if (primaryCountMin > 0) {

                this.mode2MineralCountMin = primaryCountMin;

                if (this.mode2MineralCountMinEdit != null) {

                    this.mode2MineralCountMinEdit.setValue(String.valueOf(primaryCountMin));

                }

            }

            if (primaryCountMax > 0) {

                this.mode2MineralCountMax = primaryCountMax;

                if (this.mode2MineralCountMaxEdit != null) {

                    this.mode2MineralCountMaxEdit.setValue(String.valueOf(primaryCountMax));

                }

            }

            if (byproductCountMin > 0) {

                this.mode2ByproductCountMin = byproductCountMin;

                if (this.mode2ByproductCountMinEdit != null) {

                    this.mode2ByproductCountMinEdit.setValue(String.valueOf(byproductCountMin));

                }

            }

            if (byproductCountMax > 0) {

                this.mode2ByproductCountMax = byproductCountMax;

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

            ResourceLocation biomeLoc = ResourceLocation.tryParse(string3);

            if (string3 != null && biomeLoc != null) {

                int idx = this.mode2Biomes.indexOf(biomeLoc);

                if (idx >= 0) {

                    this.mode2BiomeIdx = idx;

                    this.mode2BiomeSelectedIndices.add(idx);

                }

            }

        } catch (Exception ex) {

            logWarn("Failed to reverse-parse ore gen files", null);

        }

    }

    public void loadMode2Data() {

        DimensionBiomesData dimensionBiomesData = this.loadDimBiomesFromFile();

        if (dimensionBiomesData != null) {

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

            String string = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            return DimensionBiomesData.fromJson(string);

        }

        catch (Exception exception) {

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

        for (Map.Entry entry : dimensionBiomesData.biomesByDim.entrySet()) {

            ArrayList<ResourceLocation> arrayList = new ArrayList<ResourceLocation>();

            List<String> list = (List<String>)entry.getValue();

            for (String string : list) {

                arrayList.add(ResourceLocation.parse(string));

            }

            this.mode2BiomesByDim.put((String)entry.getKey(), arrayList);

        }

        if (!dimensionBiomesData.allBiomes.isEmpty()) {

            ArrayList arrayList = new ArrayList();

            for (String string : dimensionBiomesData.allBiomes) {

                arrayList.add(ResourceLocation.parse(string));

            }

            this.mode2BiomesByDim.put("__all__", arrayList);

            this.mode2DimIdx = this.mode2Dimensions.size();

        }

        this.syncBiomesFromDim();

        if (this.mode2DimIdx > this.mode2Dimensions.size()) {

            this.mode2DimIdx = 0;

        }

        if (this.mode2BiomeIdx >= this.mode2Biomes.size()) {

            this.mode2BiomeIdx = 0;

        }

        if (this.mode2BiomeSelectedIndices.isEmpty()) {

            this.mode2BiomeSelectedIndices.add(this.mode2BiomeIdx);

        }

        this.updateMode2ButtonLabels();

        this.mode2DataPending = false;

    }

    String getCurrentDimCategory() {

        return this.mode2DimIdx >= 0 && this.mode2DimIdx < this.mode2Dimensions.size() ? this.mode2Dimensions.get(this.mode2DimIdx).toString() : "__all__";

    }

    public void syncBiomesFromDim() {

        String string = this.getCurrentDimCategory();

        List<ResourceLocation> list = this.mode2BiomesByDim.get(string);

        if (list == null) {

            list = this.mode2BiomesByDim.get("__all__");

        }

        this.mode2Biomes = list != null ? list : new ArrayList();

    }

    public void updateMode2ButtonLabels() {

        String string;

        String string2;

        ArrayList<String> arrayList;

        if (this.mode2Dropdown != null) {

            arrayList = new ArrayList<String>();

            for (ResourceLocation object : this.mode2Dimensions) {

                string2 = "generator." + object.getNamespace() + "." + object.getPath();

                string = Language.getInstance().getOrDefault(string2);

                if (string.equals(string2)) {

                    string = object.getPath();

                }

                arrayList.add(string);

            }

            if (this.mode2BiomesByDim.containsKey("__all__")) {

                arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.all_biomes"));

            }

            if (this.mode2DataPending) {

                arrayList.clear();

                arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.loading"));

            }

            this.mode2Dropdown.setOptions(arrayList, this.mode2DimIdx);

        }

        if (this.mode2BiomeDropdown != null) {

            arrayList = new ArrayList();

            for (ResourceLocation resourceLocation : this.mode2Biomes) {

                string2 = "biome." + resourceLocation.getNamespace() + "." + resourceLocation.getPath();

                string = Language.getInstance().getOrDefault(string2);

                if (string.equals(string2)) {

                    string = resourceLocation.getPath();

                }

                arrayList.add(string);

            }

            if (this.mode2DataPending) {

                arrayList.clear();

                arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.loading"));

            }

            this.mode2BiomeDropdown.setMultiselect(true);

            this.mode2BiomeDropdown.setOptions(arrayList, 0);

            this.mode2BiomeDropdown.setSelectedIndices(this.mode2BiomeSelectedIndices);

        }

        if (this.mode2BtnMineralGen != null) {

            String string3 = this.getItemBlockId(this.menu.slots.get(0).getItem());

            String string4 = !string3.isEmpty() && string3.contains(":") ? string3.substring(string3.indexOf(58) + 1) : "";

            File file = new File(this.getDatapackDataDir(), "worldgen/configured_feature/visualcrafting_ore_mineral_" + string4 + ".json");

            if (!string4.isEmpty() && file.exists()) {

                this.mode2BtnMineralGen.setMessage(Component.translatable("gui.visualcrafting.label.update_mineral"));

            } else {

                this.mode2BtnMineralGen.setMessage(Component.translatable("gui.visualcrafting.mode2.mineral_gen"));

            }

        }

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

    private void onMode2MineralGen(Button button) {

        String string = this.getItemBlockId(this.menu.slots.get(0).getItem());

        if (string.isEmpty()) {

            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_slot_empty").getString());

        } else {

            this.buildAndWriteOreGenFiles(string);

            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_created").getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());

            this.updateMode2ButtonLabels();

        }

    }

    private void onMode2BanGen(Button button) {

        // WIP 提示：原常驻标记已取消，改为点击本按钮时提示
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal("WIP"), false);
        }

        String string = this.getItemBlockId(this.menu.slots.get(0).getItem());

        if (string.isEmpty()) {

            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_slot_empty").getString());

        } else {

            this.removeOreGenFiles();

            this.showStatus(Component.translatable("gui.visualcrafting.status.mineral_removed").getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());

            this.updateMode2ButtonLabels();

            this.updateMode2ButtonStates();

        }

    }

    private void onMode2Config(Button button) {

        this.saveMode2Config();

        Path path = this.getDatapackDataDir().toPath().resolve("worldgen");

        try {

            Files.createDirectories(path, new FileAttribute[0]);

        }

        catch (Exception exception) {

            VisualCraftingScreen.logWarn("Failed to create datapack data directories", null);

        }

        Util.getPlatform().openFile(path.toFile());

        String string = this.getItemBlockId(this.menu.slots.get(0).getItem());

        if (!string.isEmpty()) {

            this.buildAndWriteOreGenFiles(string);

            this.showStatus(Component.translatable("gui.visualcrafting.status.config_saved").getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());

            this.updateMode2ButtonLabels();

        } else {

            this.showStatus(Component.translatable("gui.visualcrafting.status.config_saved_no_ore").getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());

        }

    }

    public void updateMode2ButtonStates() {

        if (this.mode2BtnMineralGen != null) {

            ItemStack itemStack = this.menu.slots.get(0).getItem();

            boolean bl = !itemStack.isEmpty();

            this.mode2BtnMineralGen.active = bl;

            this.mode2BtnBanGen.active = bl;

        }

    }

    String getItemBlockId(ItemStack itemStack) {

        if (itemStack.isEmpty()) {

            return "";

        }

        try {

            return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

        }

        catch (Exception exception) {

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

        catch (Exception exception) {

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
        catch (Exception exception) {
            VisualCraftingScreen.logWarn("Failed to write pending for " + recipeId, exception);
        }
    }


    public void ensureDatapackExists() {
        Object object;

        File file = this.getDatapackDir();

        File file2 = new File(file, "pack.mcmeta");

        if (!file2.exists()) {

            file.mkdirs();

            try {

                object = "{\n  \"pack\": {\n    \"pack_format\": 57,\n    \"description\": \"VisualCrafting Ore Generation\"\n  }\n}";

                Files.writeString(file2.toPath(), (String)object, StandardCharsets.UTF_8, new OpenOption[0]);

            }

            catch (Exception exception) {

                VisualCraftingScreen.logWarn("Failed to write pack.mcmeta", null);

            }

        }

        File f_vt = new File(file, "data/visualcrafting/villager_trade"); if (!f_vt.exists()) {

            f_vt.mkdirs();

        }

    }

    public void buildAndWriteOreGenFiles(String string) {

        if (this.mode2BiomeDropdown != null) {

            this.mode2BiomeSelectedIndices = new LinkedHashSet<Integer>(this.mode2BiomeDropdown.getSelectedIndices());

        }

        String string2 = this.getItemBlockId(this.menu.slots.get(1).getItem());

        List<String> list = this.gatherSelectedBiomeIds();

        String string3 = string.contains(":") ? string.substring(string.indexOf(58) + 1) : string;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {

            int veinSize = Math.max(1, (int)((double)this.mode2MineralPct * 0.15));

            String string4 = "visualcrafting:visualcrafting_ore_mineral_" + string3;

            JsonObject mineralConfigured = this.buildConfiguredFeature(gson, string4, string, veinSize);

            this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/configured_feature/visualcrafting_ore_mineral_" + string3, mineralConfigured);

            JsonObject byproductConfigured = null;

            String object = "";

            if (!string2.isEmpty()) {

                int byproductVein = Math.max(1, (int)((double)this.mode2ByproductPct * 0.08));

                object = "visualcrafting:visualcrafting_ore_byproduct_" + string3;

                byproductConfigured = this.buildConfiguredFeature(gson, (String)object, string2, byproductVein);

                this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/configured_feature/visualcrafting_ore_byproduct_" + string3, byproductConfigured);

            }

            JsonObject mineralPlaced = this.buildPlacedFeature(gson, string4, this.mode2MineralCountMin, this.mode2MineralCountMax, this.mode2MinY, this.mode2MaxY, "visualcrafting_ore_mineral_" + string3);

            this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/placed_feature/visualcrafting_ore_mineral_" + string3, mineralPlaced);

            if (!string2.isEmpty()) {

                JsonObject byproductPlaced = this.buildPlacedFeature(gson, (String)object, this.mode2ByproductCountMin, this.mode2ByproductCountMax, this.mode2MinY, this.mode2MaxY, "visualcrafting_ore_byproduct_" + string3);

                this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/placed_feature/visualcrafting_ore_byproduct_" + string3, byproductPlaced);

            }

            JsonObject biomeModifier = this.buildBiomeModifier(gson, list, string4, (String)object);

            this.writePending(MergeManager.TYPE_WORLDGEN, "neoforge/biome_modifier/add_visualcrafting_ore", biomeModifier);

            JsonObject biomeModifierTag = this.buildBiomeModifierTag();

            this.writePending(MergeManager.TYPE_WORLDGEN, "neoforge/tags/worldgen/biome_modifier/visualcrafting_ore", biomeModifierTag);

        }

        catch (Exception exception) {

            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.generate_failed", exception.getMessage()).getString());

        }

    }

    List<String> gatherSelectedBiomeIds() {

        ArrayList<String> arrayList = new ArrayList<String>();

        if (!this.mode2Biomes.isEmpty()) {

            for (int n : this.mode2BiomeSelectedIndices) {

                if (n < 0 || n >= this.mode2Biomes.size()) continue;

                arrayList.add(this.mode2Biomes.get(n).toString());

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

    public JsonObject buildPlacedFeature(Gson gson, String modid, int veinSize, int veinsPerChunk, int minY, int maxY, String targetBlock) {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("feature", targetBlock);

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


    public JsonObject buildBiomeModifier(Gson gson, List<String> list, String string, String string2) {

        JsonArray jsonArray;

        JsonObject jsonObject;

        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();

        File file3 = new File(this.getDatapackDataDir(), "neoforge/biome_modifier/add_visualcrafting_ore.json");

        if (file3.exists()) {

            try {

                jsonObject = (JsonObject)JsonParser.parseString(Files.readString(file3.toPath(), StandardCharsets.UTF_8));

                if (jsonObject.has("features") && jsonObject.get("features").isJsonArray()) {

                    for (JsonElement object : jsonObject.getAsJsonArray("features")) {

                        linkedHashSet.add(object.getAsString());

                    }

                }

            }

            catch (Exception exception) {

                VisualCraftingScreen.logWarn("Corrupted biome_modifier JSON, starting fresh", exception);

            }

        }

        linkedHashSet.add(string);

        if (!string2.isEmpty()) {

            linkedHashSet.add(string2);

        }

        jsonObject = new JsonObject();

        jsonObject.addProperty("type", "neoforge:add_features");

        if (list.size() > 1) {

            jsonArray = new JsonArray();

            for (String string3 : list) {

                jsonArray.add(string3);

            }

            jsonObject.add("biomes", jsonArray);

        } else {

            jsonObject.addProperty("biomes", list.get(0));

        }

        jsonArray = new JsonArray();

        for (String string4 : linkedHashSet) {

            jsonArray.add(string4);

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


    public void removeOreGenFiles() {
        if (this.mode2BiomeDropdown != null) {
            this.mode2BiomeSelectedIndices = new LinkedHashSet<Integer>(this.mode2BiomeDropdown.getSelectedIndices());
        }
        File datapackDir = this.getDatapackDataDir();
        String itemId = this.getItemBlockId(this.menu.slots.get(0).getItem());
        String itemName = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Disable ore generation by changing state.Name to minecraft:stone in configured_feature JSONs
        String[] featureIds = { "visualcrafting_ore_mineral_" + itemName, "visualcrafting_ore_byproduct_" + itemName };
        for (String featureId : featureIds) {
            File cfgFile = new File(datapackDir, "worldgen/configured_feature/" + featureId + ".json");
            if (!cfgFile.exists()) {
                continue;
            }
            try {
                JsonObject root = (JsonObject)JsonParser.parseString(Files.readString(cfgFile.toPath(), StandardCharsets.UTF_8));
                if (root.has("config") && root.get("config").isJsonObject()) {
                    JsonObject config = root.getAsJsonObject("config");
                    if (config.has("targets") && config.get("targets").isJsonArray()) {
                        JsonArray targets = config.getAsJsonArray("targets");
                        for (JsonElement t : targets) {
                            if (t.isJsonObject()) {
                                JsonObject targetObj = t.getAsJsonObject();
                                if (targetObj.has("state") && targetObj.get("state").isJsonObject()) {
                                    JsonObject state = targetObj.getAsJsonObject("state");
                                    state.addProperty("Name", "minecraft:stone");
                                }
                            }
                        }
                    }
                }
                this.writePending(MergeManager.TYPE_WORLDGEN, "worldgen/configured_feature/" + featureId, root);
            } catch (Exception e) {
                LOGGER.warn("Failed to disable configured_feature: {}", featureId, e);
            }
        }

        // Remove old remove_* modifier file from previous implementation if exists
        File oldRemoveFile = new File(this.getDatapackDir(), "neoforge/biome_modifier/remove_visualcrafting_ore_" + itemName + ".json");
        if (oldRemoveFile.exists()) {
            oldRemoveFile.delete();
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

                if (this.mode2DimIdx >= 0 && this.mode2DimIdx < this.mode2Dimensions.size()) {

                    stringBuilder.append("  \"dimension\": \"").append(this.mode2Dimensions.get(this.mode2DimIdx)).append("\",\n");

                } else {

                    stringBuilder.append("  \"dimension\": \"__all__\",\n");

                }

            }

            if (!this.mode2Biomes.isEmpty()) {

                stringBuilder.append("  \"biomes\": [");

                boolean bl = true;

                for (int n : this.mode2BiomeSelectedIndices) {

                    if (n < 0 || n >= this.mode2Biomes.size()) continue;

                    if (!bl) {

                        stringBuilder.append(", ");

                    }

                    stringBuilder.append("\"").append(this.mode2Biomes.get(n)).append("\"");

                    bl = false;

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

        catch (Exception exception) {

            LOGGER.warn("Failed to save mode2 config", exception);

        }

    }

    public void showStatus(String string) {

        this.statusMessage = string;

        this.statusUntil = System.currentTimeMillis() + 3000L;

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

        return this.gridAbsX() + slotIndex % 3 * 18 + this.gridSlotOffsetX;

    }

    public int getGridSlotY(int slotIndex) {

        return this.gridAbsY() + slotIndex / 3 * 18 + this.gridSlotOffsetY;

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

        return this.slotAbsX(0);

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

        int tabNameIconX = tabStartX + (tabWidth + tabGap) * 4 + 5;

        guiGraphics.renderItem(ICON_NAME, tabNameIconX, craftIconY);

        if (this.mode == 6) {

            guiGraphics.renderOutline(tabStartX + (tabWidth + tabGap) * 4, tabStartY, tabWidth, tabHeight, -256);

        }

        if (this.mode == 0) {

            int gridSize = this.getGridSize();

            int gridX = this.gridAbsX();

            int gridY = this.gridAbsY();

            guiGraphics.renderOutline(gridX + this.gridSlotOffsetX, gridY + this.gridSlotOffsetY, gridSize * 18, gridSize * 18, -1);

            for (int i = 0; i < gridSize; ++i) {

                for (int j = 0; j < gridSize; ++j) {

                    guiGraphics.renderOutline(gridX + j * 18 + this.gridSlotOffsetX, gridY + i * 18 + this.gridSlotOffsetY, 18, 18, -1);

                }

            }

            guiGraphics.renderOutline(this.getOutputSlotX() + this.outSlotLineOffsetX, this.getOutputSlotY() + this.outSlotLineOffsetY, 18, 18, -1);

            this.renderCraftList(guiGraphics, mouseX, mouseY);

        } else if (this.mode == 1) {

            this.renderInfusingExtras(guiGraphics);

            this.renderInfuseList(guiGraphics, mouseX, mouseY);

                } else if (this.mode == 5) {

            this.renderMode5Extras(guiGraphics);

        } else if (this.mode == 6) {

            this.renderNameExtras(guiGraphics, mouseX, mouseY);

        } else {

            this.renderMode2Extras(guiGraphics);

        }

        for (int slotIdx = 82; slotIdx < this.menu.slots.size(); ++slotIdx) {

            guiGraphics.renderOutline(this.slotAbsX(slotIdx) + this.invLineOffsetX, this.slotAbsY(slotIdx) + this.invLineOffsetY, 18, 18, -1);

        }

        if (!this.statusMessage.isEmpty() && System.currentTimeMillis() < this.statusUntil) {

            int statusWidth = this.font.width(this.statusMessage);

            int statusX = this.leftPos + (this.imageWidth - statusWidth) / 2;

            int statusY = this.topPos + this.imageHeight - 30;

            guiGraphics.fill(statusX - 4, statusY, statusX + statusWidth + 4, statusY + 16, -872415232);

            guiGraphics.renderOutline(statusX - 4, statusY, statusWidth + 8, 16, -256);

            guiGraphics.drawString(this.font, this.statusMessage, statusX, statusY + 2, -256, false);

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

                guiGraphics.drawString(this.font, "\u00a7m " + infusingRecipe.output.getHoverName().getString(), listX + 22, rowY + 4, 0x808080, false);

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

        int dropdownOpen = 0;

        if (this.mode == 2) {

            if (this.mode2BiomeDropdown != null && this.mode2BiomeDropdown.isExpanded()) {

                dropdownOpen = 1;

            }

            if (this.mode2Dropdown != null && this.mode2Dropdown.isExpanded()) {

                dropdownOpen = 1;

            }

        }

        if (this.mode == 5 && this.mode5PotionDropdown != null && this.mode5PotionDropdown.isExpanded()) {

            dropdownOpen = 1;

        }

        if (dropdownOpen != 0) {

            if (this.mode == 2) {

                if (this.mode2BiomeDropdown != null) {

                    this.mode2BiomeDropdown.mouseClicked(mouseX, mouseY, button);

                }

                if (this.mode2Dropdown != null) {

                    this.mode2Dropdown.mouseClicked(mouseX, mouseY, button);

                }

            }

            if (this.mode == 5 && this.mode5PotionDropdown != null) {

                this.mode5PotionDropdown.mouseClicked(mouseX, mouseY, button);

            }

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

            String alwaysText = (this.mode5AlwaysEdible ? "\u2611 " : "\u25A1 ") + Component.translatable("gui.visualcrafting.label.ignore_saturation").getString();

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

                catch (Throwable throwable) {

                    VisualCraftingScreen.logWarn("Mekanism not available, skipping infusion chemical rendering", throwable);

                }

            }

            this.menu.chemAmount = infusingRecipe.infusionAmount;

            break;

        }

    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        return super.keyPressed(keyCode, scanCode, modifiers);

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

                    } catch (Throwable throwable) {

                        logWarn("Mekanism not available, skipping tooltip chemical name", throwable);

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

                this.gridSlotOffsetX = Integer.parseInt(properties.getProperty("gridSlotOffsetX", "11"));

                this.gridSlotOffsetY = Integer.parseInt(properties.getProperty("gridSlotOffsetY", "11"));

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

                this.outSlotSlotOffsetX = Integer.parseInt(properties.getProperty("outSlotSlotOffsetX", "11"));

                this.outSlotSlotOffsetY = Integer.parseInt(properties.getProperty("outSlotSlotOffsetY", "10"));

                this.recipesOffsetX = Integer.parseInt(properties.getProperty("recipesOffsetX", "0"));

                this.recipesOffsetY = Integer.parseInt(properties.getProperty("recipesOffsetY", "2"));

                this.tierOffsetY = Integer.parseInt(properties.getProperty("tierOffsetY", "0"));

            }

            catch (Exception exception) {

                VisualCraftingScreen.logWarn("Failed to load GUI offsets, using defaults", exception);

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

        catch (Exception exception) {

            VisualCraftingScreen.logWarn("Failed to save GUI offsets", exception);

        }

    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {

        return super.keyReleased(keyCode, scanCode, modifiers);

    }

    public static void drawOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int n5) {

        guiGraphics.fill(x, y, x + width, y + 1, n5);

        guiGraphics.fill(x, y + height - 1, x + width, y + height, n5);

        guiGraphics.fill(x, y, x + 1, y + height, n5);

        guiGraphics.fill(x + width - 1, y, x + width, y + height, n5);

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

        VisualCraftingScreen.drawOutline(guiGraphics, inputX + this.infInputSlotOffsetX, inputY + this.infInputSlotOffsetY, 18, 18, -1);

        VisualCraftingScreen.drawOutline(guiGraphics, outputX + this.infOutSlotLineOffsetX, outputY + this.infOutSlotLineOffsetY, 18, 18, -1);

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
            catch (Throwable throwable) {
                VisualCraftingScreen.logWarn("Mekanism not available, skipping infusion extras chemical rendering", throwable);
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
            catch (Throwable throwable) {
                VisualCraftingScreen.logWarn("Mekanism not available, skipping chemical mark icon", throwable);
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
        guiGraphics.renderOutline(slotX + invOffX, mineralY + invOffY, 18, 18, -1);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.col.amount").getString(), this.leftPos + 91 + this.mode2OffsetX, this.topPos + 43, 0x404040, false);
        String mineralPctText = this.mode2MineralPct + "%";
        guiGraphics.drawString(this.font, mineralPctText, slotX + 9 - this.font.width(mineralPctText) / 2, this.topPos + 61, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "~", this.leftPos + 136 + this.mode2OffsetX, this.topPos + 47, 0xFFFFFF, false);

        // Byproduct row: vertical "伴/生" label, 18x18 slot outline, amount header, pct below slot
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.vert.by.1").getString(), labelX, byproductY + invOffY + 1, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.vert.by.2").getString(), labelX, byproductY + invOffY + 1 + labelStep, 0x404040, false);
        guiGraphics.renderOutline(slotX + invOffX, byproductY + invOffY, 18, 18, -1);
        guiGraphics.drawString(this.font, Component.translatable("gui.visualcrafting.mode2.col.amount").getString(), this.leftPos + 91 + this.mode2OffsetX, this.topPos + 77, 0x404040, false);
        String byproductPctText = this.mode2ByproductPct + "%";
        guiGraphics.drawString(this.font, byproductPctText, slotX + 9 - this.font.width(byproductPctText) / 2, this.topPos + 88, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "~", this.leftPos + 135 + this.mode2OffsetX, this.topPos + 80, 0xFFFFFF, false);

        // WIP 标记已取消常驻显示，改为点击「禁止生成」按钮时提示（见 onMode2BanGen）
    }


    private void syncMode5FromSlot0() {

        ItemStack itemStack = this.menu.slots.get(0).getItem();

        if (ItemStack.matches(itemStack, this.mode5LastSlot0Item)) {

            return;

        }

        this.mode5LastSlot0Item = itemStack.copy();

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

            MobEffectInstance mobEffectInstance = ((FoodProperties.PossibleEffect)list.get(0)).effect();

            ResourceLocation resourceLocation = this.minecraft.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).getKey(mobEffectInstance.getEffect().value());

            if (resourceLocation != null) {

                String string = resourceLocation.toString();

                for (int i = 0; i < this.mode5PotionIds.size(); ++i) {

                    if (!this.mode5PotionIds.get(i).equals(string)) continue;

                    this.mode5PotionSelectedIdx.add(i);

                    break;

                }

            }

            this.mode5PotionLevel = mobEffectInstance.getAmplifier() + 1;

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

                    String string = "effect." + resourceLocation.getNamespace() + "." + resourceLocation.getPath();

                    String string2 = Language.getInstance().getOrDefault(string);

                    if (string2.equals(string)) {

                        string2 = resourceLocation.getPath();

                    }

                    this.mode5PotionNames.add(string2);

                }

            }

        }

        catch (Exception exception) {

            VisualCraftingScreen.logWarn("Failed to load potion effects from registry", null);

        }

        if (this.mode5PotionDropdown != null) {

            this.mode5PotionDropdown.setOptions(this.mode5PotionNames, 0);

            this.mode5PotionDropdown.setSelectedIndices(this.mode5PotionSelectedIdx);

        }

    }

    protected void renderMode5Extras(GuiGraphics guiGraphics) {
        int gl = this.leftPos;
        int gt = this.topPos;

        // Slot outlines
        guiGraphics.renderOutline(gl + 93, gt + 16, 18, 18, -1);
        guiGraphics.renderOutline(gl + 93, gt + 39, 18, 18, -1);

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
        String durationValue = (this.mode5DurationInfinite ? "\u2611 " : "\u25A1 ") + infiniteLabel;
        guiGraphics.drawString(this.font, durationValue, gl + 173, gt + 43, 4210752);

        // Eat time and always edible
        String eatTimeText = Component.translatable("gui.visualcrafting.label.eat_time").getString();
        // 输入框起点 gl+93：标签右对齐并留 5px 间隙，y 与输入框(88~104)垂直居中
        guiGraphics.drawString(this.font, eatTimeText, gl + 93 - 5 - this.font.width(eatTimeText), gt + 92, 4210752);
        String alwaysEdibleText = (this.mode5AlwaysEdible ? "\u2611 " : "\u25A1 ") + Component.translatable("gui.visualcrafting.label.ignore_saturation").getString();
        this.drawWrapped(guiGraphics, this.font, Component.literal(alwaysEdibleText), gl + 121, gt + 92, 110, 4210752);
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

        } catch (Exception ignored) {}

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

        int activeSlots = gridSize * gridSize;

        for (int i = 0; i < activeSlots; ++i) {

            Slot slot = this.menu.slots.get(i);

            VisualCraftingScreen.setSlotX(slot, slot.x + this.gridSlotOffsetX);

            VisualCraftingScreen.setSlotY(slot, slot.y + this.gridSlotOffsetY);

        }

        Slot slot = this.menu.slots.get(81);

        VisualCraftingScreen.setSlotX(slot, slot.x + this.outSlotSlotOffsetX);

        VisualCraftingScreen.setSlotY(slot, slot.y + this.outSlotSlotOffsetY);

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
        return Component.literal(active ? "\u2611 " : "\u25A1 ").append(Component.translatable(key));
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
        guiGraphics.renderOutline(this.slotAbsX(0) - 1, this.slotAbsY(0) - 1, 18, 18, -1);
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
        catch (Exception exception) {
            VisualCraftingScreen.logWarn("Generate name script failed: " + exception.getMessage(), exception);
            this.showStatus(Component.translatable("gui.visualcrafting.name.status.generate_failed", new Object[]{exception.getMessage()}).getString());
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
        catch (Exception exception) {
            VisualCraftingScreen.logWarn("Remove name script failed: " + exception.getMessage(), exception);
            this.showStatus(Component.translatable("gui.visualcrafting.name.status.generate_failed", new Object[]{exception.getMessage()}).getString());
        }
    }

    private void onMode6Config(Button button) {
        File file = new File(this.minecraft.gameDirectory, MODE6_SCRIPT_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        Util.getPlatform().openFile(file);
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
        catch (Exception exception) {
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
        for (ResourceLocation object2 : this.mode2Dimensions) {
            String n7 = "generator." + object2.getNamespace() + "." + object2.getPath();
            Object object = Language.getInstance().getOrDefault(n7);
            if (((String)object).equals(n7)) {
                object = object2.getPath();
            }
            arrayList.add((String)object);
        }
        if (this.mode2BiomesByDim.containsKey("__all__")) {
            arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.all_biomes"));
        }
        if (this.mode2DataPending) {
            arrayList.clear();
            arrayList.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.loading"));
        }

        this.mode2Dropdown = new DropdownWidget(dropX, this.topPos + 6, dropW);
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
        for (ResourceLocation resourceLocation : this.mode2Biomes) {
            Object object = "biome." + resourceLocation.getNamespace() + "." + resourceLocation.getPath();
            String string2 = Language.getInstance().getOrDefault((String)object);
            if (string2.equals(object)) {
                string2 = resourceLocation.getPath();
            }
            arrayList2.add(string2);
        }
        if (this.mode2DataPending) {
            arrayList2.clear();
            arrayList2.add(Language.getInstance().getOrDefault("gui.visualcrafting.label.loading"));
        }

        this.mode2BiomeDropdown = new DropdownWidget(dropX, this.topPos + 24, dropW);
        this.mode2BiomeDropdown.setMultiselect(true);
        this.mode2BiomeDropdown.setOptions(arrayList2, 0);
        this.mode2BiomeDropdown.setSelectedIndices(this.mode2BiomeSelectedIndices);
        this.addRenderableWidget(this.mode2BiomeDropdown);

        // Height-range (layer) edit boxes: 36x16, x=leftPos+99/+147+mode2OffsetX, y=topPos+102
        this.mode2MinYEdit = new EditBox(this.font, this.leftPos + 99 + this.mode2OffsetX, this.topPos + 102, 36, 16, Component.empty());
        this.mode2MinYEdit.setValue(String.valueOf(this.mode2MinY));
        this.mode2MinYEdit.setFilter(string -> string.isEmpty() || string.matches("-?\\d{0,3}"));
        this.mode2MinYEdit.setResponder(string -> {
            if (!string.isEmpty() && !string.equals("-")) {
                try {
                    int value = Integer.parseInt(string);
                    if (value < -63 || value > 319) {
                        this.mode2MinYEdit.setValue("");
                        return;
                    }
                    this.mode2MinY = value;
                } catch (NumberFormatException numberFormatException) {
                    this.mode2MinYEdit.setValue("");
                }
            } else {
                this.mode2MinY = -63;
            }
        });
        this.addRenderableWidget(this.mode2MinYEdit);

        this.mode2MaxYEdit = new EditBox(this.font, this.leftPos + 147 + this.mode2OffsetX, this.topPos + 102, 36, 16, Component.empty());
        this.mode2MaxYEdit.setValue(String.valueOf(this.mode2MaxY));
        this.mode2MaxYEdit.setFilter(string -> string.isEmpty() || string.matches("-?\\d{0,3}"));
        this.mode2MaxYEdit.setResponder(string -> {
            if (!string.isEmpty() && !string.equals("-")) {
                try {
                    int value = Integer.parseInt(string);
                    if (value < -63 || value > 319) {
                        this.mode2MaxYEdit.setValue("");
                        return;
                    }
                    this.mode2MaxY = value;
                } catch (NumberFormatException numberFormatException) {
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
        this.mode2MineralCountMinEdit.setFilter(string -> string.isEmpty() || string.matches("\\d{0,3}"));
        this.mode2MineralCountMinEdit.setResponder(string -> {
            if (string.isEmpty()) {
                this.mode2MineralCountMin = 1;
            } else {
                try {
                    this.mode2MineralCountMin = Math.clamp(Integer.parseInt(string), 1, 999);
                } catch (Exception exception) {
                    VisualCraftingScreen.logWarn("Failed to parse mode2MineralCountMin", null);
                }
            }
        });
        this.addRenderableWidget(this.mode2MineralCountMinEdit);

        this.mode2MineralCountMaxEdit = new EditBox(this.font, this.leftPos + 147 + this.mode2OffsetX, this.topPos + 42, 22, 14, Component.empty());
        this.mode2MineralCountMaxEdit.setValue(String.valueOf(this.mode2MineralCountMax));
        this.mode2MineralCountMaxEdit.setFilter(string -> string.isEmpty() || string.matches("\\d{0,3}"));
        this.mode2MineralCountMaxEdit.setResponder(string -> {
            if (string.isEmpty()) {
                this.mode2MineralCountMax = 3;
            } else {
                try {
                    this.mode2MineralCountMax = Math.clamp(Integer.parseInt(string), 1, 999);
                } catch (Exception exception) {
                    VisualCraftingScreen.logWarn("Failed to parse mode2MineralCountMax", null);
                }
            }
        });
        this.addRenderableWidget(this.mode2MineralCountMaxEdit);

        this.mode2ByproductCountMinEdit = new EditBox(this.font, this.leftPos + 111 + this.mode2OffsetX, this.topPos + 76, 22, 14, Component.empty());
        this.mode2ByproductCountMinEdit.setValue(String.valueOf(this.mode2ByproductCountMin));
        this.mode2ByproductCountMinEdit.setFilter(string -> string.isEmpty() || string.matches("\\d{0,3}"));
        this.mode2ByproductCountMinEdit.setResponder(string -> {
            if (string.isEmpty()) {
                this.mode2ByproductCountMin = 1;
            } else {
                try {
                    this.mode2ByproductCountMin = Math.clamp(Integer.parseInt(string), 1, 999);
                } catch (Exception exception) {
                    VisualCraftingScreen.logWarn("Failed to parse mode2ByproductCountMin", null);
                }
            }
        });
        this.addRenderableWidget(this.mode2ByproductCountMinEdit);

        this.mode2ByproductCountMaxEdit = new EditBox(this.font, this.leftPos + 148 + this.mode2OffsetX, this.topPos + 76, 22, 14, Component.empty());
        this.mode2ByproductCountMaxEdit.setValue(String.valueOf(this.mode2ByproductCountMax));
        this.mode2ByproductCountMaxEdit.setFilter(string -> string.isEmpty() || string.matches("\\d{0,3}"));
        this.mode2ByproductCountMaxEdit.setResponder(string -> {
            if (string.isEmpty()) {
                this.mode2ByproductCountMax = 2;
            } else {
                try {
                    this.mode2ByproductCountMax = Math.clamp(Integer.parseInt(string), 1, 999);
                } catch (Exception exception) {
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

        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnSave));

        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnDelete));

        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnConfig));

        VisualCraftingScreen.setSlotX(this.menu.slots.get(0), 94);

        VisualCraftingScreen.setSlotY(this.menu.slots.get(0), 17);

        VisualCraftingScreen.setSlotX(this.menu.slots.get(1), 94);

        VisualCraftingScreen.setSlotY(this.menu.slots.get(1), 40);

        this.mode5HungerEdit = new EditBox(this.font, this.leftPos + 144, this.topPos + 18, 24, 16, Component.empty());

        this.mode5HungerEdit.setFilter(string -> string.isEmpty() || string.matches("\\d{0,4}"));

        this.mode5HungerEdit.setValue(String.valueOf(this.mode5Hunger));

        this.mode5HungerEdit.setResponder(string -> {

            if (string.isEmpty()) {

                this.mode5Hunger = 4;

            } else {

                try {

                    int val = Integer.parseInt(string);

                    if (val < 0) {

                        this.mode5HungerEdit.setValue("");

                        return;

                    }

                    this.mode5Hunger = val;

                }

                catch (Exception exception) {

                    this.mode5HungerEdit.setValue("");

                }

            }

        });

        this.addRenderableWidget(this.mode5HungerEdit);

        this.mode5SaturationEdit = new EditBox(this.font, this.leftPos + 204, this.topPos + 18, 24, 16, Component.empty());

        this.mode5SaturationEdit.setFilter(string -> string.isEmpty() || string.matches("\\d*\\.?\\d{0,2}"));

        this.mode5SaturationEdit.setValue(String.valueOf(this.mode5Saturation));

        this.mode5SaturationEdit.setResponder(string -> {

            if (string.isEmpty()) {

                this.mode5Saturation = 0.3f;

            } else {

                try {

                    float f = Float.parseFloat(string);

                    if (f < 0.0f) {

                        this.mode5SaturationEdit.setValue("");

                        return;

                    }

                    this.mode5Saturation = f;

                }

                catch (Exception exception) {

                    this.mode5SaturationEdit.setValue("");

                }

            }

        });

        this.addRenderableWidget(this.mode5SaturationEdit);

        this.mode5PotionDropdown = new DropdownWidget(this.leftPos + 92, this.topPos + 64, 80);

        this.mode5PotionDropdown.setMultiselect(true);

        this.mode5PotionDropdown.setOnSelect(this::onMode5PotionSelect);

        this.mode5PotionDropdown.setOptions(this.mode5PotionNames, 0);

        this.addRenderableWidget(this.mode5PotionDropdown);

        this.mode5PotionLevelEdit = new EditBox(this.font, this.leftPos + 204, this.topPos + 64, 24, 16, Component.empty());

        this.mode5PotionLevelEdit.setFilter(string -> string.isEmpty() || string.matches("\\d{0,3}"));

        this.mode5PotionLevelEdit.setValue(String.valueOf(this.mode5PotionLevel));

        this.mode5PotionLevelEdit.setResponder(string -> {

            if (string.isEmpty()) {

                this.mode5PotionLevel = 1;

            } else {

                try {

                    int val2 = Integer.parseInt(string);

                    if (val2 < 0 || val2 > 255) {

                        this.mode5PotionLevelEdit.setValue("");

                        return;

                    }

                    this.mode5PotionLevel = val2;

                }

                catch (Exception exception) {

                    this.mode5PotionLevelEdit.setValue("");

                }

            }

        });

        this.addRenderableWidget(this.mode5PotionLevelEdit);

        this.mode5DurationEdit = new EditBox(this.font, this.leftPos + 144, this.topPos + 41, 24, 16, Component.empty());

        this.mode5DurationEdit.setFilter(string -> string.isEmpty() || string.matches("\\d{0,5}"));

        this.mode5DurationEdit.setValue(this.mode5DurationInfinite ? "" : String.valueOf(this.mode5Duration));

        this.mode5DurationEdit.setEditable(!this.mode5DurationInfinite);

        this.mode5DurationEdit.setResponder(string -> {

            if (string.isEmpty()) {

                this.mode5Duration = 600;

            } else {

                try {

                    int val3 = Integer.parseInt(string);

                    if (val3 < 0) {

                        this.mode5DurationEdit.setValue("");

                        return;

                    }

                    this.mode5Duration = val3;

                }

                catch (Exception exception) {

                    this.mode5DurationEdit.setValue("");

                }

            }

        });

        this.addRenderableWidget(this.mode5DurationEdit);

        this.mode5EatSecondsEdit = new EditBox(this.font, this.leftPos + 93, this.topPos + 88, 24, 16, Component.empty());

        this.mode5EatSecondsEdit.setFilter(string -> string.isEmpty() || string.matches("\\d*\\.?\\d{0,2}"));

        this.mode5EatSecondsEdit.setValue(this.mode5EatSeconds == 0 ? "" : String.valueOf((float)this.mode5EatSeconds / 20.0f));

        this.mode5EatSecondsEdit.setResponder(string -> {

            if (string.isEmpty()) {

                this.mode5EatSeconds = 0;

            } else {

                try {

                    float f = Float.parseFloat(string);

                    this.mode5EatSeconds = Math.max(0, Math.round(f * 20.0f));

                }

                catch (Exception exception) {

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

    private String buildModifySnippet(String string, String string2) {

        Object object;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("  // ").append(string).append("\n");

        stringBuilder.append("  event.modify('").append(string).append("', function(item) {\n");

        stringBuilder.append("    var builder = new $FoodBuilder();\n");

        stringBuilder.append("    builder.nutrition(").append(this.mode5Hunger).append(");\n");

        stringBuilder.append("    builder.saturation(saturationFixer(").append(this.mode5Saturation).append(", ").append(this.mode5Hunger).append("));\n");

        stringBuilder.append("    builder.alwaysEdible(").append(this.mode5AlwaysEdible).append(");\n");

        if (this.mode5EatSeconds > 0) {

            stringBuilder.append("    builder.eatSeconds(").append((float)this.mode5EatSeconds / 20.0f).append(");\n");

        }

        net.minecraft.world.item.ItemStack returnStack = this.menu.slots.get(1).getItem();

        if (!returnStack.isEmpty()) {

            String string3 = BuiltInRegistries.ITEM.getKey(returnStack.getItem()).toString();

            stringBuilder.append("    builder.usingConvertsTo(Item.of('").append(string3).append("'));\n");

        } else {

            stringBuilder.append("    builder.usingConvertsTo(Item.of('minecraft:air'));\n");

        }

        if (!this.mode5PotionSelectedIdx.isEmpty()) {

            java.util.Iterator<Integer> iter = this.mode5PotionSelectedIdx.iterator();

            while (iter.hasNext()) {

                int recipeIndex = iter.next();

                if (recipeIndex < 0 || recipeIndex >= this.mode5PotionIds.size()) continue;

                String string4 = this.mode5PotionIds.get(recipeIndex);

                int potionLevel = this.mode5PotionLevel - 1;

                int duration = this.mode5DurationInfinite ? -1 : this.mode5Duration;

                stringBuilder.append("    builder.effect('").append(string4).append("', ").append(duration).append(", ").append(potionLevel).append(", 1.0);\n");

            }

        }

        stringBuilder.append("    item.set('food', builder.build());\n");

        stringBuilder.append("  });\n");

        return stringBuilder.toString();

    }

    private String buildFileHeader() {

        return "// === VisualCrafting Food Script (auto-generated) ===\n// KubeJS 1.21+ FoodBuilder API\n// \u914d\u5408 saturationFixer \u4fee\u6b63\u9971\u548c\u5ea6\u8ba1\u7b97\n\nvar $FoodBuilder = Java.loadClass('dev.latvian.mods.kubejs.item.FoodBuilder');\n\nfunction saturationFixer(expectedSaturation, nutrition) {\n    return expectedSaturation / (2 * nutrition);\n}\n";

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
        String string = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        String string2 = string.contains(":") ? string.substring(0, string.indexOf(58)) : "unknown";
        String string3 = this.buildModifySnippet(string, string2);
        String commentMarker;
        try {
            Object object;
            File file = new File(this.minecraft.gameDirectory, "kubejs/startup_scripts");
            file.mkdirs();
            File file2 = new File(file, "visualcrafting_food.js");
            StringBuilder stringBuilder = new StringBuilder();
            if (file2.exists()) {
                String string4;
                object = new String(Files.readAllBytes(file2.toPath()), StandardCharsets.UTF_8);
                commentMarker = "  // " + string + "\n";
                int commentStart = ((String)object).indexOf(commentMarker);
                if (commentStart >= 0) {
                    int skipNewlines;
                    int endPos;
                    int closeParenEnd = ((String)object).indexOf("  });\n", commentStart);
                    int nextComment = ((String)object).indexOf("  // ", commentStart + commentMarker.length());
                    if (closeParenEnd >= 0) {
                        endPos = closeParenEnd + "  });\n".length();
                    } else {
                        endPos = ((String)object).indexOf("\n})\n", commentStart);
                        if (endPos < 0) {
                            endPos = ((String)object).indexOf("})", commentStart);
                        }
                        if (endPos < 0) {
                            endPos = ((String)object).length();
                        }
                    }
                    if (nextComment >= 0 && nextComment < endPos) {
                        endPos = nextComment;
                    }
                    stringBuilder.append((String)object, 0, commentStart);
                    stringBuilder.append(string3);
                    for (skipNewlines = endPos; skipNewlines < ((String)object).length() && ((String)object).charAt(skipNewlines) == '\n'; ++skipNewlines) {
                    }
                    stringBuilder.append((String)object, skipNewlines, ((String)object).length());
                } else {
                    int lastEnd = ((String)object).lastIndexOf("\n})\n");
                    if (lastEnd < 0) {
                        lastEnd = ((String)object).lastIndexOf("})");
                    }
                    if (lastEnd >= 0) {
                        stringBuilder.append((String)object, 0, lastEnd);
                        if (!((String)object).substring(0, lastEnd).endsWith("\n")) {
                            stringBuilder.append("\n");
                        }
                        stringBuilder.append(string3);
                        stringBuilder.append((String)object, lastEnd, ((String)object).length());
                    } else {
                        stringBuilder.append((String)object);
                        if (!((String)object).endsWith("\n")) {
                            stringBuilder.append("\n");
                        }
                        stringBuilder.append(string3);
                    }
                }
            } else {
                stringBuilder.append(this.buildFileHeader());
                stringBuilder.append(this.buildItemEventsHeader());
                stringBuilder.append(string3);
                stringBuilder.append(this.buildItemEventsFooter());
            }
            JsonObject scriptContent = new JsonObject();
            scriptContent.addProperty("script", stringBuilder.toString());
            this.writePending(MergeManager.TYPE_STARTUP_SCRIPTS, "visualcrafting_food.js", scriptContent);
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.saved", new Object[]{string}).getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());
        }
        catch (Exception exception) {
            VisualCraftingScreen.logWarn("Generate script failed: " + exception.getMessage(), exception);
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.generate_failed", new Object[]{exception.getMessage()}).getString());
        }
    }
    private void onMode5DeleteRecipe(Button button) {
        ItemStack itemStack = this.menu.slots.get(0).getItem();
        if (itemStack.isEmpty()) {
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.need_item").getString());
            return;
        }
        String string = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        try {
            Object object;
            int closeBraceIdx;
            String string2;
            File file = new File(this.minecraft.gameDirectory, "kubejs/startup_scripts/visualcrafting_food.js");
            if (!file.exists()) {
                this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.file_not_found").getString());
                return;
            }
            String string3 = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String commentMarker = "  // " + string + "\n";
            int commentStart = string3.indexOf(commentMarker);
            if (commentStart < 0) {
                this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.entry_not_found", new Object[]{string}).getString());
                return;
            }
            int nextCommentIdx = string3.indexOf("  // ", commentStart + commentMarker.length());
            int endMarkerIdx = string3.indexOf("\n})\n", commentStart);
            if (endMarkerIdx < 0) {
                endMarkerIdx = string3.indexOf("})\n", commentStart);
            }
            int endPos = nextCommentIdx >= 0 && (endMarkerIdx < 0 || nextCommentIdx < endMarkerIdx) ? nextCommentIdx : (endMarkerIdx >= 0 ? endMarkerIdx : string3.length());
            int closeParenIdx = string3.indexOf("  });\n", commentStart);
            if (closeParenIdx >= 0 && closeParenIdx < endPos) {
                endPos = closeParenIdx + "  });\n".length();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(string3, 0, commentStart);
            stringBuilder.append(string3, endPos, string3.length());
            String string4 = stringBuilder.toString();
            int modEventIdx = string4.indexOf("ItemEvents.modification(function(event) {");
            if (modEventIdx >= 0 && (closeBraceIdx = string4.indexOf("})\n", modEventIdx)) >= 0) {
                object = string4.substring(modEventIdx + "ItemEvents.modification(function(event) {".length(), closeBraceIdx);
                boolean bl = false;
                for (String string5 : ((String)object).split("\n")) {
                    String string6 = string5.trim();
                    if (!string6.startsWith("event.modify(") && (!string6.startsWith("//") || string6.startsWith("// ===") || !string5.contains("  // "))) continue;
                    bl = true;
                    break;
                }
                if (!bl) {
                    int skipPos;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(string4, 0, modEventIdx);
                    for (skipPos = closeBraceIdx + "})\n".length(); skipPos < string4.length() && string4.charAt(skipPos) == '\n'; ++skipPos) {
                    }
                    stringBuilder2.append(string4, skipPos, string4.length());
                    string4 = stringBuilder2.toString();
                }
            }
            string4 = string4.replaceAll("\\n{3,}", "\n\n");
            JsonObject scriptContent = new JsonObject();
            scriptContent.addProperty("script", string4);
            this.writePending(MergeManager.TYPE_STARTUP_SCRIPTS, "visualcrafting_food.js", scriptContent);
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.deleted", new Object[]{string}).getString() + " | " + Component.translatable("gui.visualcrafting.status.reload_hint").getString());
        }
        catch (Exception exception) {
            VisualCraftingScreen.logWarn("Delete recipe failed: " + exception.getMessage(), exception);
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.delete_failed", new Object[]{exception.getMessage()}).getString());
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
    }

    private class DropdownWidget
        extends AbstractWidget {
        private final List<String> options;
        private int selectedIdx;
        private int scrollOffset;
        private boolean expanded;
        private boolean multiselect;
        private final Set<Integer> selectedIndices;
        private IntConsumer onSelect;
        private static final int ROW_HEIGHT = 14;
        private static final int MAX_VISIBLE = 10;
        private static final int BUTTON_HEIGHT = 16;
        public DropdownWidget(int x, int y, int width) {
        super(x, y, width, BUTTON_HEIGHT, Component.empty());
        this.options = new ArrayList<String>();
        this.selectedIdx = 0;
        this.scrollOffset = 0;
        this.expanded = false;
        this.multiselect = false;
        this.selectedIndices = new LinkedHashSet<Integer>();
        this.onSelect = null;
        }
        public void setOptions(List<String> optionsList, int defaultIdx) {
        this.options.clear();
        this.options.addAll(optionsList);
        this.selectedIdx = Math.clamp(defaultIdx, 0, Math.max(0, optionsList.size() - 1));
        this.updateMessage();
        }
        public void setSelected(int index) {
        this.selectedIdx = Math.clamp(index, 0, Math.max(0, this.options.size() - 1));
        this.updateMessage();
        }
        public int getSelectedIdx() {
        return this.selectedIdx;
        }
        public boolean isExpanded() {
        return this.expanded;
        }
        public void collapse() {
        this.expanded = false;
        this.scrollOffset = 0;
        }
        public void setMultiselect(boolean enabled) {
        this.multiselect = enabled;
        }
        public boolean isMultiselect() {
        return this.multiselect;
        }
        public Set<Integer> getSelectedIndices() {
        return this.selectedIndices;
        }
        public void setOnSelect(IntConsumer callback) {
        this.onSelect = callback;
        }
        public void setSelectedIndices(Set<Integer> indices) {
        this.selectedIndices.clear();
        if (indices != null) {
        this.selectedIndices.addAll(indices);
        }
        this.updateMultiMessage();
        }
        private void updateMessage() {
        if (this.selectedIdx >= 0 && this.selectedIdx < this.options.size()) {
        this.setMessage(Component.literal(this.options.get(this.selectedIdx)));
        }
        }
        private void updateMultiMessage() {
        if (this.multiselect) {
        if (this.selectedIndices.isEmpty()) {
        this.setMessage(Component.translatable("gui.visualcrafting.label.unselected"));
        } else if (this.selectedIndices.size() == 1) {
        int singleIdx = this.selectedIndices.iterator().next();
        if (singleIdx >= 0 && singleIdx < this.options.size()) {
        this.setMessage(Component.literal(this.options.get(singleIdx)));
        } else {
        this.setMessage(Component.translatable("gui.visualcrafting.label.unselected"));
        }
        } else {
        this.setMessage(Component.translatable("gui.visualcrafting.label.selected_count", this.selectedIndices.size()));
        }
        }
        }
        // Returns Y coordinate of the dropdown panel, flipping upward if it would overflow the GUI bottom
        private int getDropdownY() {
        int screenBottom = VisualCraftingScreen.this.topPos + VisualCraftingScreen.this.imageHeight;
        int dropdownHeight = this.getDropdownHeight();
        int dropdownY = this.getY() + BUTTON_HEIGHT;
        if (dropdownY + dropdownHeight > screenBottom) {
        dropdownY = this.getY() - dropdownHeight;
        }
        if (dropdownY < VisualCraftingScreen.this.topPos) {
        dropdownY = VisualCraftingScreen.this.topPos;
        }
        return dropdownY;
        }
        private int getDropdownHeight() {
        return Math.min(this.options.size(), MAX_VISIBLE) * ROW_HEIGHT + 2;
        }
        private int getVisibleRows() {
        return Math.min(this.options.size(), MAX_VISIBLE);
        }
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
        return false;
        }
        if (!this.isMouseOver(mouseX, mouseY)) {
        if (this.expanded) {
        this.expanded = false;
        }
        return false;
        }
        if (mouseY >= (double)this.getY() && mouseY < (double)(this.getY() + BUTTON_HEIGHT)) {
        this.expanded = !this.expanded;
        this.scrollOffset = 0;
        return true;
        }
        if (this.expanded) {
        int dropdownY = this.getDropdownY();
        int localY = (int)(mouseY - (double)dropdownY - 1.0);
        int clickedOptionIdx = this.scrollOffset + localY / ROW_HEIGHT;
        if (localY >= 0 && clickedOptionIdx >= 0 && clickedOptionIdx < this.options.size()) {
        if (this.multiselect) {
        if (this.selectedIndices.contains(clickedOptionIdx)) {
        this.selectedIndices.remove(clickedOptionIdx);
        } else {
        this.selectedIndices.add(clickedOptionIdx);
        }
        this.updateMultiMessage();
        return true;
        }
        this.selectedIdx = clickedOptionIdx;
        this.updateMessage();
        this.expanded = false;
        if (this.onSelect != null) {
        this.onSelect.accept(clickedOptionIdx);
        }
        }
        return true;
        }
        return false;
        }
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.expanded && scrollY != 0.0) {
        int dropdownY = this.getDropdownY();
        int dropdownHeight = this.getDropdownHeight();
        if (mouseX >= (double)this.getX() && mouseX < (double)(this.getX() + this.width)
                && mouseY >= (double)dropdownY && mouseY < (double)(dropdownY + dropdownHeight)) {
        int maxScroll = Math.max(0, this.options.size() - this.getVisibleRows());
        this.scrollOffset = Math.clamp(this.scrollOffset - ((int)Math.signum(scrollY)), 0, maxScroll);
        return true;
        }
        return false;
        }
        return false;
        }
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
        return true;
        }
        if (!this.expanded) {
        return false;
        }
        int dropdownY = this.getDropdownY();
        int dropdownHeight = this.getDropdownHeight();
        return mouseX >= (double)this.getX() && mouseX < (double)(this.getX() + this.width)
                && mouseY >= (double)dropdownY && mouseY < (double)(dropdownY + dropdownHeight);
        }
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bgColor = this.isHovered ? 0xFF555555 : 0xFF333333;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + BUTTON_HEIGHT, bgColor);
        graphics.renderOutline(this.getX(), this.getY(), this.width, BUTTON_HEIGHT, -1);
        int textColor = this.active ? 0xFFFFFF : 0xA0A0A0;
        graphics.drawString(VisualCraftingScreen.this.font, this.getMessage(), this.getX() + 4, this.getY() + 4, textColor, false);
        if (this.expanded && !this.options.isEmpty()) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 500.0f);
        int dropdownY = this.getDropdownY();
        int dropdownHeight = this.getDropdownHeight();
        graphics.enableScissor(this.getX(), dropdownY, this.getX() + this.width, dropdownY + dropdownHeight);
        graphics.fill(this.getX(), dropdownY, this.getX() + this.width, dropdownY + dropdownHeight, 0xFF000000);
        graphics.renderOutline(this.getX(), dropdownY, this.width, dropdownHeight, -1);
        int visibleRows = this.getVisibleRows();
        int rowIndex;
        for (rowIndex = 0; rowIndex < visibleRows && (this.scrollOffset + rowIndex) < this.options.size(); ++rowIndex) {
        int optionIdx = this.scrollOffset + rowIndex;
        int rowY = dropdownY + 1 + rowIndex * ROW_HEIGHT;
        if (this.multiselect) {
        boolean isSelected = this.selectedIndices.contains(optionIdx);
        if (isSelected) {
        graphics.fill(this.getX() + 1, rowY, this.getX() + this.width - 1, rowY + ROW_HEIGHT, 0x40FFFFFF);
        }
        String checkmark = isSelected ? "\u2611" : "\u25A1";
        graphics.drawString(VisualCraftingScreen.this.font, checkmark, this.getX() + 4, rowY + 2, isSelected ? 0x55FF55 : 0x808080, false);
        graphics.drawString(VisualCraftingScreen.this.font, this.options.get(optionIdx), this.getX() + 20, rowY + 2, 0xFFFFFF, false);
        continue;
        }
        if (optionIdx == this.selectedIdx) {
        graphics.fill(this.getX() + 1, rowY, this.getX() + this.width - 1, rowY + ROW_HEIGHT, 0x40FFFFFF);
        }
        graphics.drawString(VisualCraftingScreen.this.font, this.options.get(optionIdx), this.getX() + 4, rowY + 2, 0xFFFFFF, false);
        }
        if (this.options.size() > visibleRows) {
        int totalPages = Math.max(0, this.options.size() - visibleRows);
        String scrollText = (this.scrollOffset + 1) + "/" + (totalPages + 1);
        int scrollTextWidth = VisualCraftingScreen.this.font.width(scrollText);
        graphics.drawString(VisualCraftingScreen.this.font, scrollText,
                this.getX() + this.width - scrollTextWidth - 4, dropdownY + dropdownHeight - 11, 0x808080, false);
        }
        graphics.disableScissor();
        graphics.pose().popPose();
        }
        }
        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
        }
        }

    private static class WrappableButton
        extends Button {
        public WrappableButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }
        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
        Component message = this.getMessage();
        int maxTextWidth = this.getWidth() - 6;
        if (font.width(message) <= maxTextWidth) {
        graphics.drawCenteredString(font, message, this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2, color);
        } else {
        List<FormattedCharSequence> lines = font.split(message, maxTextWidth);
        int lineHeight = 9;
        int totalTextHeight = lines.size() * lineHeight;
        int startY = this.getY() + (this.getHeight() - totalTextHeight) / 2;
        for (int i = 0; i < lines.size(); ++i) {
        graphics.drawCenteredString(font, lines.get(i), this.getX() + this.getWidth() / 2,
                startY + i * lineHeight, color);
        }
        }
        }
        }
}