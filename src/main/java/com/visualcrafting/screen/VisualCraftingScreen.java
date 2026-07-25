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

     static final String[] TIER_LABELS = new String[]{"\u57fa\u7840", "\u8fdb\u9636", "\u9ad8\u7ea7", "\u7ec8\u6781"};

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

     static final int WORLD_HEIGHT = 319;

     static final Field SLOT_X = null;

     static final Field SLOT_Y = null;

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

            this.mode = visualCraftingBlockEntity.getMode();

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

                PacketDistributor.sendToServer(new ModMessages.AddRecipePacket(this.menu.blockPos, shaped, slot.getItem().copy(), arrayList), new CustomPacketPayload[0]);

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

            this.showStatus("\u5316\u5b66\u54c1\u4e0d\u80fd\u4e3a\u7a7a\u2014\u2014\u8bf7\u5148\u4ece JEI \u62d6\u62fd\u9009\u62e9");

        } else if (itemStack2.isEmpty()) {

            this.showStatus("\u7269\u54c1\u8f93\u5165\u4e0d\u80fd\u4e3a\u7a7a\u2014\u2014\u8bf7\u5148\u653e\u5165\u7269\u54c1");

        } else if (itemStack.isEmpty()) {

            this.showStatus("\u8f93\u51fa\u4e0d\u80fd\u4e3a\u7a7a\u2014\u2014\u8bf7\u5148\u653e\u5165\u8f93\u51fa\u7269\u54c1");

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

                this.showStatus("\u5316\u5b66\u54c1\u6570\u636e\u65e0\u6548");

            } else {

                PacketDistributor.sendToServer(new ModMessages.AddInfusingRecipePacket(visualCraftingMenu.blockPos, itemStack3.copy(), itemStack2.copy(), itemStack.copy(), chemAmount), new CustomPacketPayload[0]);

                this.showStatus("\u704c\u6ce8\u914d\u65b9\u5df2\u751f\u6210");

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

                arrayList.add("\u5168\u90e8\u7fa4\u7cfb");

            }

            if (this.mode2DataPending) {

                arrayList.clear();

                arrayList.add("\u52a0\u8f7d\u4e2d...");

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

                arrayList.add("\u52a0\u8f7d\u4e2d...");

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

                this.mode2BtnMineralGen.setMessage(Component.literal("\u66f4\u65b0\u77ff\u7269"));

            } else {

                this.mode2BtnMineralGen.setMessage(Component.literal("\u77ff\u7269\u751f\u6210"));

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

            this.showStatus("\u77ff\u7269\u69fd\u4e3a\u7a7a");

        } else {

            this.buildAndWriteOreGenFiles(string);

            this.showStatus("\u5df2\u751f\u6210\u77ff\u7269\u751f\u6210\u914d\u7f6e");

            this.updateMode2ButtonLabels();

        }

    }

    private void onMode2BanGen(Button button) {

        String string = this.getItemBlockId(this.menu.slots.get(0).getItem());

        if (string.isEmpty()) {

            this.showStatus("\u77ff\u7269\u69fd\u4e3a\u7a7a");

        } else {

            this.removeOreGenFiles();

            this.showStatus("\u5df2\u79fb\u9664\u77ff\u7269\u751f\u6210\u914d\u7f6e");

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

            this.showStatus("\u914d\u7f6e\u5df2\u4fdd\u5b58\uff0c\u751f\u6210\u914d\u7f6e\u5df2\u66f4\u65b0");

            this.updateMode2ButtonLabels();

        } else {

            this.showStatus("\u914d\u7f6e\u5df2\u4fdd\u5b58\uff08\u77ff\u7269\u69fd\u4e3a\u7a7a\uff0c\u672a\u751f\u6210\u4e16\u754c\u751f\u6210\u6587\u4ef6\uff09");

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

                    this.showStatus("\u6ce8\u610f\uff1a\u591a\u4eba\u670d\u52a1\u5668\u4e0a\u6570\u636e\u5305\u5c06\u5199\u5165\u5ba2\u6237\u7aef\u672c\u5730\uff0c\u8bf7\u5728\u670d\u52a1\u7aef\u624b\u52a8\u90e8\u7f72");

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

        this.ensureDatapackExists();

        File file = this.getDatapackDataDir();

        String string2 = this.getItemBlockId(this.menu.slots.get(1).getItem());

        List<String> list = this.gatherSelectedBiomeIds();

        String string3 = string.contains(":") ? string.substring(string.indexOf(58) + 1) : string;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {

            int veinSize = Math.max(1, (int)((double)this.mode2MineralPct * 0.15));

            String string4 = "visualcrafting:visualcrafting_ore_mineral_" + string3;

            this.writeConfiguredFeature(file, gson, string4, string, veinSize, "visualcrafting_ore_mineral_" + string3);

            Object object = "";

            if (!string2.isEmpty()) {

                int byproductVein = Math.max(1, (int)((double)this.mode2ByproductPct * 0.08));

                object = "visualcrafting:visualcrafting_ore_byproduct_" + string3;

                this.writeConfiguredFeature(file, gson, (String)object, string2, byproductVein, "visualcrafting_ore_byproduct_" + string3);

            }

            this.writePlacedFeature(file, gson, string4, this.mode2MineralCountMin, this.mode2MineralCountMax, this.mode2MinY, this.mode2MaxY, "visualcrafting_ore_mineral_" + string3);

            if (!string2.isEmpty()) {

                this.writePlacedFeature(file, gson, (String)object, this.mode2ByproductCountMin, this.mode2ByproductCountMax, this.mode2MinY, this.mode2MaxY, "visualcrafting_ore_byproduct_" + string3);

            }

            this.mergeBiomeModifier(file, gson, list, string4, (String)object);

            this.writeBiomeModifierTag(file, gson);

        }

        catch (Exception exception) {

            this.showStatus("\u751f\u6210\u5931\u8d25: " + exception.getMessage());

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

    public void writeConfiguredFeature(File file, Gson gson, String modid, String blockId, int veinSize, String targetBlock) throws IOException {

        File file2 = new File(file, "worldgen/configured_feature");

        file2.mkdirs();

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

        File file3 = new File(file2, targetBlock + ".json");

        try (FileWriter fileWriter = new FileWriter(file3, StandardCharsets.UTF_8);){

            gson.toJson(jsonObject, fileWriter);

        }

    }

    public void writePlacedFeature(File file, Gson gson, String modid, int veinSize, int veinsPerChunk, int minY, int maxY, String targetBlock) throws IOException {

        File file2 = new File(file, "worldgen/placed_feature");

        file2.mkdirs();

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

        File file3 = new File(file2, modid + ".json");

        try (FileWriter fileWriter = new FileWriter(file3, StandardCharsets.UTF_8);){

            gson.toJson(jsonObject, fileWriter);

        }

    }


    public void mergeBiomeModifier(File file, Gson gson, List<String> list, String string, String string2) throws IOException {

        JsonArray jsonArray;

        JsonObject jsonObject;

        File file2 = new File(file, "neoforge/biome_modifier");

        file2.mkdirs();

        File file3 = new File(file2, "add_visualcrafting_ore.json");

        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();

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

        try (FileWriter fileWriter = new FileWriter(file3, StandardCharsets.UTF_8);){

            gson.toJson(jsonObject, fileWriter);

        }

    }

    public void writeBiomeModifierTag(File file, Gson gson) throws IOException {

        File file2 = new File(file.getParentFile(), "neoforge/tags/worldgen/biome_modifier");

        file2.mkdirs();

        File file3 = new File(file2, "visualcrafting_ore.json");

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("replace", Boolean.valueOf(false));

        JsonArray jsonArray = new JsonArray();

        jsonArray.add("visualcrafting:add_visualcrafting_ore");

        jsonObject.add("values", jsonArray);

        try (FileWriter fileWriter = new FileWriter(file3, StandardCharsets.UTF_8);){

            gson.toJson(jsonObject, fileWriter);

        }

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
                try (FileWriter writer = new FileWriter(cfgFile, StandardCharsets.UTF_8)) {
                    gson.toJson(root, writer);
                }
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

        this.mode = newMode;

        PacketDistributor.sendToServer(new ModMessages.ModeUpdatePacket(this.menu.blockPos, newMode), new CustomPacketPayload[0]);

        this.markedContainer = ItemStack.EMPTY;

        this.readBEState();

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

        guiGraphics.drawString(this.font, "\u914d\u65b9 (" + this.recipes.size() + ")", listX, listY - 14, 0x404040, false);

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

        if (this.mode == 1 && button == 1) {

            int chemSlotX = this.getChemSlotX();

            int chemSlotY = this.getChemSlotY();

            if (mouseX >= (double)chemSlotX && mouseX < (double)(chemSlotX + 22) && mouseY >= (double)chemSlotY && mouseY < (double)(chemSlotY + 22)) {

                this.menu.chemSlotData = null;

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

        if (chemSlotData != null) {

            chemColor = chemSlotData.tintColor;

        } else {

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

        RenderSystem.enableBlend();

        guiGraphics.fill(chemX + 1, chemY + 1, chemX + 17, chemY + 17, Integer.MIN_VALUE | chemColor & 0xFFFFFF);

        RenderSystem.disableBlend();

        int currentChemAmount = this.menu.chemAmount;

        String amountText = currentChemAmount + "mB";

        guiGraphics.drawString(this.font, amountText, chemX + this.chemLabelX, chemY + this.chemLabelY, 0x404040, false);

        guiGraphics.drawString(this.font, "\u8f93\u5165", inputX + this.inputLabelX, inputY + this.inputLabelY, 0x404040, false);

        guiGraphics.drawString(this.font, "\u8f93\u51fa", outputX + this.outputLabelX, outputY + this.outputLabelY, 0x404040, false);

    }

    void renderMode2Extras(GuiGraphics guiGraphics) {
        // Table layout constants
        int tableY = this.topPos + 106;
        int rowH = 44, rowGap = 2;
        int slotSize = 36;
        int nameW = 60;
        int colNameX = this.leftPos + 8 + slotSize + 8;
        int nameYOff = (rowH - 9) / 2;
        int editW = 46;
        int colMinX = colNameX + nameW + 8;

        // Row 0: mineral
        guiGraphics.drawString(this.font, "矿石数量", colNameX, tableY + nameYOff, 0x404040, false);
        guiGraphics.drawString(this.font, "~", colMinX + editW + 2, tableY + nameYOff, 0xFFFFFF, false);

        // Row 1: byproduct
        int row1Y = tableY + rowH + rowGap;
        guiGraphics.drawString(this.font, "副产物", colNameX, row1Y + nameYOff, 0x404040, false);
        guiGraphics.drawString(this.font, "~", colMinX + editW + 2, row1Y + nameYOff, 0xFFFFFF, false);

        // Row 2: height range
        int row2Y = row1Y + rowH + rowGap;
        guiGraphics.drawString(this.font, "高度范围", colNameX, row2Y + nameYOff, 0x404040, false);
        guiGraphics.drawString(this.font, "~", colMinX + editW + 2, row2Y + nameYOff, 0xFFFFFF, false);
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
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.hunger"), gl + 116, gt + 19, 55, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.saturation"), gl + 172, gt + 19, 55, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.potion_effects"), gl + 56, gt + 68, 120, 4210752);
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.level"), gl + 180, gt + 68, 48, 4210752);

        // Return item indicator
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.return_item"), gl + 57, gt + 45, 58, 4210752);
        String returnItemLabel = Component.translatable("gui.visualcrafting.mode5.label.return_item").getString();
        String returnItemValue = this.mode5ReturnItem ? "\u2611" : "\u2610";
        int returnItemWidth = this.font.width(returnItemLabel);
        guiGraphics.drawString(this.font, returnItemValue, gl + 57 + returnItemWidth + 2, gt + 45, 4210752);

        // Duration indicator
        this.drawWrapped(guiGraphics, this.font, Component.translatable("gui.visualcrafting.mode5.label.duration"), gl + 116, gt + 44, 55, 4210752);
        String infiniteLabel = Component.translatable("gui.visualcrafting.mode5.label.infinite").getString();
        String durationValue = this.mode5DurationInfinite ? "\u2611" + infiniteLabel : "\u2610" + infiniteLabel;
        guiGraphics.drawString(this.font, durationValue, gl + 173, gt + 44, 4210752);

        // Eat time and always edible
        guiGraphics.drawString(this.font, "\u98df\u7528\u65f6\u95f4", gl + 51, gt + 91, 4210752);
        String alwaysEdibleText = this.mode5AlwaysEdible ? "\u2611\u5ffd\u89c6\u9971\u98df\u5ea6" : "\u2610\u5ffd\u89c6\u9971\u98df\u5ea6";
        guiGraphics.drawString(this.font, alwaysEdibleText, gl + 122, gt + 91, 4210752);
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

        Button button6 = Button.builder(Component.literal(TIER_LABELS[0]), button -> this.onTier(0)).pos(this.leftPos + this.imageWidth - 94, this.topPos + this.imageHeight - 44 + this.tierOffsetY).size(46, 16).build();

        Button button7 = Button.builder(Component.literal(TIER_LABELS[1]), button -> this.onTier(1)).pos(this.leftPos + this.imageWidth - 46, this.topPos + this.imageHeight - 44 + this.tierOffsetY).size(46, 16).build();

        Button button8 = Button.builder(Component.literal(TIER_LABELS[2]), button -> this.onTier(2)).pos(this.leftPos + this.imageWidth - 94, this.topPos + this.imageHeight - 26 + this.tierOffsetY).size(46, 16).build();

        Button button9 = Button.builder(Component.literal(TIER_LABELS[3]), button -> this.onTier(3)).pos(this.leftPos + this.imageWidth - 46, this.topPos + this.imageHeight - 26 + this.tierOffsetY).size(46, 16).build();

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

            VisualCraftingScreen.setSlotX(this.menu.slots.get(0), 80 + this.infInputSlotSlotOffsetX + this.infInputSlotOffsetX);

            VisualCraftingScreen.setSlotY(this.menu.slots.get(0), 35 + this.infInputSlotSlotOffsetY + this.infInputSlotOffsetY);

            VisualCraftingScreen.setSlotX(this.menu.slots.get(81), 148 + this.infOutSlotSlotOffsetX + this.infOutSlotLineOffsetX);

            VisualCraftingScreen.setSlotY(this.menu.slots.get(81), 45 + this.infOutSlotSlotOffsetY + this.infOutSlotLineOffsetY);

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

        // Top area: buttons (left) + dropdowns (right)
        int btnX = this.leftPos + 8;
        int btnW = 66, btnH = 24, btnGap = 4;

        this.mode2BtnMineralGen = new WrappableButton(btnX, this.topPos + 12, btnW, btnH, Component.translatable("gui.visualcrafting.mode2.mineral_gen"), this::onMode2MineralGen);
        this.mode2BtnBanGen = new WrappableButton(btnX, this.topPos + 12 + btnH + btnGap, btnW, btnH, Component.translatable("gui.visualcrafting.mode2.ban_gen"), this::onMode2BanGen);
        this.mode2BtnConfig = new WrappableButton(btnX, this.topPos + 12 + (btnH + btnGap) * 2, btnW, btnH, Component.translatable("gui.visualcrafting.config"), this::onMode2Config);
        this.funcButtons.add(this.addRenderableWidget(this.mode2BtnMineralGen));
        this.funcButtons.add(this.addRenderableWidget(this.mode2BtnBanGen));
        this.funcButtons.add(this.addRenderableWidget(this.mode2BtnConfig));

        int dropX = btnX + btnW + 8;
        int dropW = 160;
        int dropH = 24;
        int dropY1 = this.topPos + 12;
        int dropY2 = this.topPos + 12 + dropH + 4;

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
            arrayList.add("全部群系");
        }
        if (this.mode2DataPending) {
            arrayList.clear();
            arrayList.add("加载中...");
        }

        this.mode2Dropdown = new DropdownWidget(dropX, dropY1, dropW);
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
            arrayList2.add("加载中...");
        }

        this.mode2BiomeDropdown = new DropdownWidget(dropX, dropY2, dropW);
        this.mode2BiomeDropdown.setMultiselect(true);
        this.mode2BiomeDropdown.setOptions(arrayList2, 0);
        this.mode2BiomeDropdown.setSelectedIndices(this.mode2BiomeSelectedIndices);
        this.addRenderableWidget(this.mode2BiomeDropdown);

        // Table area: 3 rows (mineral, byproduct, height range)
        int tableY = this.topPos + 106;
        int rowH = 44, rowGap = 2;
        int slotSize = 36;
        int nameW = 60;
        int editW = 46, editH = 22;
        int colSlotX = this.leftPos + 8;
        int colNameX = colSlotX + slotSize + 8;
        int colMinX = colNameX + nameW + 8;
        int colMaxX = colMinX + editW + 6;

        // Row 0: mineral
        int row0Y = tableY + (rowH - editH) / 2;
        VisualCraftingScreen.setSlotX(this.menu.slots.get(0), colSlotX);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(0), tableY + (rowH - 18) / 2);

        this.mode2MineralCountMinEdit = new EditBox(this.font, colMinX, row0Y, editW, editH, Component.empty());
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

        this.mode2MineralCountMaxEdit = new EditBox(this.font, colMaxX, row0Y, editW, editH, Component.empty());
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

        // Row 1: byproduct
        int row1Y = tableY + rowH + rowGap;
        int row1EY = row1Y + (rowH - editH) / 2;
        VisualCraftingScreen.setSlotX(this.menu.slots.get(1), colSlotX);
        VisualCraftingScreen.setSlotY(this.menu.slots.get(1), row1Y + (rowH - 18) / 2);

        this.mode2ByproductCountMinEdit = new EditBox(this.font, colMinX, row1EY, editW, editH, Component.empty());
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

        this.mode2ByproductCountMaxEdit = new EditBox(this.font, colMaxX, row1EY, editW, editH, Component.empty());
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

        // Row 2: height range (no slot)
        int row2Y = row1Y + rowH + rowGap;
        int row2EY = row2Y + (rowH - editH) / 2;

        this.mode2MinYEdit = new EditBox(this.font, colMinX, row2EY, editW, editH, Component.empty());
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

        this.mode2MaxYEdit = new EditBox(this.font, colMaxX, row2EY, editW, editH, Component.empty());
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

        this.mode2WidgetsInited = true;
        this.reverseParseOreGenFiles();
        this.updateMode2ButtonLabels();
        this.updateMode2ButtonStates();
    }

    private boolean mode5ReturnItem = false;

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

        int mode5BtnWidth = 46;

        int BUTTON_HEIGHT = 16;

        this.mode5BtnSave = Button.builder(Component.literal("保存"), this::onMode5GenerateScript).pos(this.leftPos + 8, this.topPos + 12).size(mode5BtnWidth, BUTTON_HEIGHT).build();

        this.mode5BtnDelete = Button.builder(Component.literal("删除"), this::onMode5DeleteRecipe).pos(this.leftPos + 8, this.topPos + 31).size(mode5BtnWidth, BUTTON_HEIGHT).build();

        this.mode5BtnConfig = Button.builder(Component.literal("配置"), this::onMode5Config).pos(this.leftPos + 8, this.topPos + 50).size(mode5BtnWidth, BUTTON_HEIGHT).build();

        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnSave));

        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnDelete));

        this.funcButtons.add(this.addRenderableWidget(this.mode5BtnConfig));

        VisualCraftingScreen.setSlotX(this.menu.slots.get(0), 93);

        VisualCraftingScreen.setSlotY(this.menu.slots.get(0), 16);

        VisualCraftingScreen.setSlotX(this.menu.slots.get(1), 93);

        VisualCraftingScreen.setSlotY(this.menu.slots.get(1), 39);

        this.mode5HungerEdit = new EditBox(this.font, this.leftPos + 144, this.topPos + 16, 24, 16, Component.empty());

        this.mode5HungerEdit.setFilter(string -> string.isEmpty() || string.matches("\\d{0,2}"));

        this.mode5HungerEdit.setValue(String.valueOf(this.mode5Hunger));

        this.mode5HungerEdit.setResponder(string -> {

            if (string.isEmpty()) {

                this.mode5Hunger = 4;

            } else {

                try {

                    int val = Integer.parseInt(string);

                    if (val < 0 || val > 20) {

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

        this.mode5SaturationEdit = new EditBox(this.font, this.leftPos + 204, this.topPos + 16, 24, 16, Component.empty());

        this.mode5SaturationEdit.setFilter(string -> string.isEmpty() || string.matches("\\d*\\.?\\d{0,2}"));

        this.mode5SaturationEdit.setValue(String.valueOf(this.mode5Saturation));

        this.mode5SaturationEdit.setResponder(string -> {

            if (string.isEmpty()) {

                this.mode5Saturation = 0.3f;

            } else {

                try {

                    float f = Float.parseFloat(string);

                    if (f < 0.0f || f > 1.0f) {

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

        this.mode5DurationEdit = new EditBox(this.font, this.leftPos + 144, this.topPos + 38, 24, 16, Component.empty());

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

        if (this.mode5ReturnItem) {

            net.minecraft.world.item.ItemStack returnStack = this.menu.slots.get(1).getItem();

            if (!returnStack.isEmpty()) {

                String string3 = BuiltInRegistries.ITEM.getKey(returnStack.getItem()).toString();

                stringBuilder.append("    builder.usingConvertsTo(Item.of('").append(string3).append("'));\n");

            } else {

                stringBuilder.append("    builder.usingConvertsTo(Item.of('minecraft:air'));\n");

            }

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
            object = new FileWriter(file2, StandardCharsets.UTF_8);
            try {
                ((FileWriter)object).write(stringBuilder.toString());
            }
            finally {
                ((FileWriter)object).close();
            }
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.saved", new Object[]{string}).getString());
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
            object = new FileWriter(file, StandardCharsets.UTF_8);
            try {
                ((FileWriter)object).write(string4);
            }
            finally {
                ((FileWriter)object).close();
            }
            this.showStatus(Component.translatable("gui.visualcrafting.mode5.status.deleted", new Object[]{string}).getString());
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
        this.setMessage(Component.literal("\u672a\u9009\u62e9"));
        } else if (this.selectedIndices.size() == 1) {
        int singleIdx = this.selectedIndices.iterator().next();
        if (singleIdx >= 0 && singleIdx < this.options.size()) {
        this.setMessage(Component.literal(this.options.get(singleIdx)));
        } else {
        this.setMessage(Component.literal("\u672a\u9009\u62e9"));
        }
        } else {
        this.setMessage(Component.literal("\u5df2\u9009\u62e9 " + this.selectedIndices.size() + " \u9879"));
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
        String checkmark = isSelected ? "\u2611" : "\u2610";
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