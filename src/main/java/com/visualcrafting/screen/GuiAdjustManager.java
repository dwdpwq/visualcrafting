package com.visualcrafting.screen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * GUI 调试调整管理器：通过反射读取/修改 {@link VisualCraftingScreen} 的偏移量字段，
 * 支持键盘微调（方向键移动、Shift 切换分组、Ctrl 加速）、叠加层显示当前偏移、
 * 以及 Ctrl+S 将偏移写入 jar 内的 gui_offsets.properties。
 */
public class GuiAdjustManager {
    public static final int GROUP_BORDER = 0;
    public static final int GROUP_SLOT = 1;
    public static final int GROUP_BUTTON = 2;
    public static final int GROUP_TEXT = 3;
    public static final int MAX_GROUPS = 4;
    private static final String[] GROUP_NAMES = new String[]{"Borders", "Slots", "Buttons", "Text"};

    /** 合成界面可调整项：[分组][项索引] = {显示名, X 偏移字段, Y 偏移字段}。 */
    private static final String[][][] CRAFTING_FIELDS = {
            {
                    {"Grid Border", "gridOffsetX", "gridOffsetY"},
                    {"Output Border", "outSlotLineOffsetX", "outSlotLineOffsetY"},
                    {"Inventory Border", "invLineOffsetX", "invLineOffsetY"},
                    {"Recipes List", "recipesOffsetX", "recipesOffsetY"}
            },
            {
                    {"Grid Slots", "gridSlotOffsetX", "gridSlotOffsetY"},
                    {"Output Slot", "outSlotSlotOffsetX", "outSlotSlotOffsetY"},
                    {"Inventory Slots", "invSlotSlotOffsetX", "invSlotSlotOffsetY"}
            },
            {
                    {"Function Buttons", "btnOffsetX", "btnOffsetY"},
                    {"Tier Buttons Y", "tierOffsetY", "tierOffsetY"}
            },
            {
                    {"Inventory Label", "invLabelOffsetX", "invLabelOffsetY"}
            }
    };

    static final String INT_INPUT_LABEL_X = "v:inputLabelX";
    static final String INT_INPUT_LABEL_Y = "v:inputLabelY";
    static final String INT_OUTPUT_LABEL_X = "v:outputLabelX";
    static final String INT_OUTPUT_LABEL_Y = "v:outputLabelY";
    static final String INT_CHEM_LABEL_X = "v:chemLabelX";
    static final String INT_CHEM_LABEL_Y = "v:chemLabelY";
    static final String INT_CHEM_LINE_X = "v:chemLineX";
    static final String INT_CHEM_LINE_Y = "v:chemLineY";

    /** 灌注界面可调整项，结构同 {@link #CRAFTING_FIELDS}。 */
    private static final String[][][] INFUSING_FIELDS = {
            {
                    {"物品槽边框", "infInputSlotOffsetX", "infInputSlotOffsetY"},
                    {"输出物品槽边框", "infOutSlotLineOffsetX", "infOutSlotLineOffsetY"},
                    {"化学品槽边框", "v:chemLineX", "v:chemLineY"},
                    {"物品栏边框", "infInvLineOffsetX", "infInvLineOffsetY"},
                    {"配方列表", "recipesOffsetX", "recipesOffsetY"}
            },
            {
                    {"物品槽 (Input Item)", "infInputSlotSlotOffsetX", "infInputSlotSlotOffsetY"},
                    {"输出物品槽 (Output Item)", "infOutSlotSlotOffsetX", "infOutSlotSlotOffsetY"},
                    {"化学品槽 (Chemical)", "infChemSlotOffsetX", "infChemSlotOffsetY"},
                    {"物品栏 (Inventory)", "infInvSlotSlotOffsetX", "infInvSlotSlotOffsetY"},
                    {"化学品数量 (Chemical Amount)", "infEditBoxOffsetX", "infEditBoxOffsetY"}
            },
            {
                    {"灌注按钮", "infButtonsOffsetX", "infButtonsOffsetY"},
                    {"等级按钮Y", "tierOffsetY", "tierOffsetY"}
            },
            {
                    {"物品槽文字", "v:inputLabelX", "v:inputLabelY"},
                    {"输出物品槽文字", "v:outputLabelX", "v:outputLabelY"},
                    {"化学品文字", "v:chemLabelX", "v:chemLabelY"},
                    {"化学品数量文字", "infAmountLabelOffsetX", "infAmountLabelOffsetY"}
            }
    };

    private static final String[][] TAB_LABEL = new String[][]{{"Crafting"}, {"Infusing"}};

    private final Object screen;
    private boolean enabled = true;
    private int focusGroup = 0;
    private int activeIndex = 0;
    private boolean ctrlDown = false;
    private int lastTab = -1;
    private final HashMap<String, Integer> internalOffsets = new HashMap<String, Integer>();

    private Field rf_leftPos;
    private Field rf_topPos;
    private Field rf_imageWidth;
    private Field rf_imageHeight;
    private Field rf_font;
    private Field rf_infInputSlotOffsetX;
    private Field rf_infInputSlotOffsetY;
    private Field rf_infOutSlotLineOffsetX;
    private Field rf_infOutSlotLineOffsetY;
    private Field rf_infAmountLabelOffsetX;
    private Field rf_infAmountLabelOffsetY;
    private Field rf_selectedChemical;
    private Field rf_menu;
    private Field rf_infEditBoxOffsetX;
    private Field rf_infEditBoxOffsetY;

    private Method rm_getChemSlotX;
    private Method rm_getChemSlotY;
    private Method rm_infInputAbsX;
    private Method rm_infInputAbsY;
    private Method rm_infOutAbsX;
    private Method rm_infOutAbsY;
    private Method rm_slotAbsX;
    private Method rm_slotAbsY;

    private boolean reflectionCached = false;

    public GuiAdjustManager(Object screen) {
        this.screen = screen;
        this.internalOffsets.put(INT_INPUT_LABEL_X, 22);
        this.internalOffsets.put(INT_INPUT_LABEL_Y, 3);
        this.internalOffsets.put(INT_OUTPUT_LABEL_X, 22);
        this.internalOffsets.put(INT_OUTPUT_LABEL_Y, 3);
        this.internalOffsets.put(INT_CHEM_LABEL_X, 20);
        this.internalOffsets.put(INT_CHEM_LABEL_Y, 5);
        this.internalOffsets.put(INT_CHEM_LINE_X, 0);
        this.internalOffsets.put(INT_CHEM_LINE_Y, 0);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    /** 读取当前界面的页签模式（0=合成，1=灌注），失败时按合成处理。 */
    private int detectTab() {
        try {
            Method getModeMethod = this.screen.getClass().getMethod("getMode");
            return (Integer)getModeMethod.invoke(this.screen);
        }
        catch (Exception e) {
            return 0;
        }
    }

    /** 返回当前页签对应的可调整项表；切换页签时重置选中项。 */
    private String[][][] currentFields() {
        int tab = this.detectTab();
        if (tab != this.lastTab) {
            this.lastTab = tab;
            this.activeIndex = 0;
        }
        return tab == 1 ? INFUSING_FIELDS : CRAFTING_FIELDS;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 341 || keyCode == 345) {
            this.ctrlDown = true;
            return false;
        }
        if (this.ctrlDown && keyCode == 83) {
            this.saveOffsetsToJar();
            return true;
        }
        if (keyCode == 340 || keyCode == 344) {
            this.focusGroup = (this.focusGroup + 1) % MAX_GROUPS;
            this.activeIndex = 0;
            return true;
        }
        int step = this.ctrlDown ? 5 : 1;
        String[][][] fields = this.currentFields();
        String[][] groupFields = fields[this.focusGroup];
        if (this.activeIndex >= groupFields.length) {
            this.activeIndex = 0;
        }
        String[] fieldEntry = groupFields[this.activeIndex];
        try {
            switch (keyCode) {
                case 263: {
                    this.adjustOffset(fieldEntry[1], -step);
                    return true;
                }
                case 262: {
                    this.adjustOffset(fieldEntry[1], step);
                    return true;
                }
                case 265: {
                    this.adjustOffset(fieldEntry[2], -step);
                    return true;
                }
                case 264: {
                    this.adjustOffset(fieldEntry[2], step);
                    return true;
                }
                case 258: {
                    this.activeIndex = (this.activeIndex + 1) % groupFields.length;
                    return true;
                }
                default:
                    break;
            }
        }
        catch (Exception e) {
            // 字段不存在或反射调用失败时忽略，保持原有布局
        }
        return false;
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 341 || keyCode == 345) {
            this.ctrlDown = false;
            return true;
        }
        return false;
    }

    /**
     * 调整指定字段的偏移量。字段名以 "v:" 开头表示内部虚拟偏移（存于 {@link #internalOffsets}），
     * 否则按反射字段名处理。调整后立即重建界面控件使改动生效。
     */
    private void adjustOffset(String fieldName, int delta) throws Exception {
        if (fieldName.startsWith("v:")) {
            Integer currentValue = this.internalOffsets.get(fieldName);
            if (currentValue == null) {
                currentValue = 0;
            }
            this.internalOffsets.put(fieldName, currentValue + delta);
            this.screen.getClass().getMethod("rebuildWidgetsPublic").invoke(this.screen);
            return;
        }
        Field field = this.screen.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        int value = field.getInt(this.screen);
        field.setInt(this.screen, value + delta);
        this.screen.getClass().getMethod("rebuildWidgetsPublic").invoke(this.screen);
    }

    /** 在界面右侧绘制调试叠加层，列出当前分组的可调整项及其偏移值。 */
    public void renderOverlay(GuiGraphics guiGraphics, Font font) {
        if (!this.enabled) {
            return;
        }
        int tab = this.detectTab();
        if (tab != this.lastTab) {
            this.lastTab = tab;
            this.activeIndex = 0;
        }
        String[][][] fields = tab == 1 ? INFUSING_FIELDS : CRAFTING_FIELDS;
        String[][] groupFields = fields[this.focusGroup];
        if (this.activeIndex >= groupFields.length) {
            this.activeIndex = 0;
        }
        try {
            int screenWidth = this.screen.getClass().getField("width").getInt(this.screen);
            int panelX = screenWidth - 220;
            int panelY = 10;
            int panelWidth = 210;
            int panelHeight = 20 + groupFields.length * 14 + 60;
            guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, -872415232);
            guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, -1);
            guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, -1);
            guiGraphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, -1);
            guiGraphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, -1);
            int textY = panelY + 6;
            String tabLabel = tab >= 0 && tab < TAB_LABEL.length ? TAB_LABEL[tab][0] : "?";
            guiGraphics.drawString(font, "GUI Adjust [" + tabLabel + "]: " + GROUP_NAMES[this.focusGroup], panelX + 6, textY, -11141291, false);
            guiGraphics.drawString(font, "Tab=Next  Arrow=Move  Shift=Group  Ctrl+S=Save", panelX + 6, textY += 14, -5592406, false);
            textY += 14;
            for (int i = 0; i < groupFields.length; ++i) {
                int valueY;
                int valueX;
                Object valueSource;
                int rowColor = i == this.activeIndex ? -256 : -5592406;
                String prefix = i == this.activeIndex ? "> " : "  ";
                String fieldX = groupFields[i][1];
                String fieldY = groupFields[i][2];
                if (fieldX.startsWith("v:")) {
                    valueSource = this.internalOffsets.get(fieldX);
                    valueX = valueSource != null ? (Integer)valueSource : 0;
                } else {
                    valueSource = this.screen.getClass().getDeclaredField(fieldX);
                    ((Field)valueSource).setAccessible(true);
                    valueX = ((Field)valueSource).getInt(this.screen);
                }
                if (fieldY.startsWith("v:")) {
                    valueSource = this.internalOffsets.get(fieldY);
                    valueY = valueSource != null ? (Integer)valueSource : 0;
                } else {
                    valueSource = this.screen.getClass().getDeclaredField(fieldY);
                    ((Field)valueSource).setAccessible(true);
                    valueY = ((Field)valueSource).getInt(this.screen);
                }
                String valueText = fieldX.equals(fieldY) ? "(" + valueX + ")" : "(" + valueX + "," + valueY + ")";
                guiGraphics.drawString(font, prefix + groupFields[i][0] + " " + valueText, panelX + 6, textY, rowColor, false);
                textY += 14;
            }
            guiGraphics.drawString(font, "Ctrl+Arrows = 5px step", panelX + 6, textY += 4, -7829249, false);
        }
        catch (Exception e) {
            // 反射读取失败时跳过本次叠加层绘制
        }
    }

    /** 首次调用时缓存灌注界面所需的反射字段与方法。 */
    private void cacheInfusingReflection() {
        if (this.reflectionCached) {
            return;
        }
        try {
            Class<?> screenClass = this.screen.getClass();
            Class<?> superClass = screenClass.getSuperclass();
            this.rf_leftPos = superClass.getDeclaredField("leftPos");
            this.rf_leftPos.setAccessible(true);
            this.rf_topPos = superClass.getDeclaredField("topPos");
            this.rf_topPos.setAccessible(true);
            this.rf_imageWidth = superClass.getDeclaredField("imageWidth");
            this.rf_imageWidth.setAccessible(true);
            this.rf_imageHeight = superClass.getDeclaredField("imageHeight");
            this.rf_imageHeight.setAccessible(true);
            this.rf_font = superClass.getDeclaredField("font");
            this.rf_font.setAccessible(true);
            this.rf_infInputSlotOffsetX = screenClass.getDeclaredField("infInputSlotOffsetX");
            this.rf_infInputSlotOffsetX.setAccessible(true);
            this.rf_infInputSlotOffsetY = screenClass.getDeclaredField("infInputSlotOffsetY");
            this.rf_infInputSlotOffsetY.setAccessible(true);
            this.rf_infOutSlotLineOffsetX = screenClass.getDeclaredField("infOutSlotLineOffsetX");
            this.rf_infOutSlotLineOffsetX.setAccessible(true);
            this.rf_infOutSlotLineOffsetY = screenClass.getDeclaredField("infOutSlotLineOffsetY");
            this.rf_infOutSlotLineOffsetY.setAccessible(true);
            this.rf_infAmountLabelOffsetX = screenClass.getDeclaredField("infAmountLabelOffsetX");
            this.rf_infAmountLabelOffsetX.setAccessible(true);
            this.rf_infAmountLabelOffsetY = screenClass.getDeclaredField("infAmountLabelOffsetY");
            this.rf_infAmountLabelOffsetY.setAccessible(true);
            this.rf_selectedChemical = screenClass.getDeclaredField("selectedChemical");
            this.rf_selectedChemical.setAccessible(true);
            this.rf_menu = superClass.getDeclaredField("menu");
            this.rf_menu.setAccessible(true);
            this.rf_infEditBoxOffsetX = screenClass.getDeclaredField("infEditBoxOffsetX");
            this.rf_infEditBoxOffsetX.setAccessible(true);
            this.rf_infEditBoxOffsetY = screenClass.getDeclaredField("infEditBoxOffsetY");
            this.rf_infEditBoxOffsetY.setAccessible(true);
            this.rm_getChemSlotX = screenClass.getDeclaredMethod("getChemSlotX");
            this.rm_getChemSlotY = screenClass.getDeclaredMethod("getChemSlotY");
            this.rm_infInputAbsX = screenClass.getDeclaredMethod("infInputAbsX");
            this.rm_infInputAbsX.setAccessible(true);
            this.rm_infInputAbsY = screenClass.getDeclaredMethod("infInputAbsY");
            this.rm_infInputAbsY.setAccessible(true);
            this.rm_infOutAbsX = screenClass.getDeclaredMethod("infOutAbsX");
            this.rm_infOutAbsX.setAccessible(true);
            this.rm_infOutAbsY = screenClass.getDeclaredMethod("infOutAbsY");
            this.rm_infOutAbsY.setAccessible(true);
            this.rm_slotAbsX = screenClass.getDeclaredMethod("slotAbsX", Integer.TYPE);
            this.rm_slotAbsX.setAccessible(true);
            this.rm_slotAbsY = screenClass.getDeclaredMethod("slotAbsY", Integer.TYPE);
            this.rm_slotAbsY.setAccessible(true);
            this.reflectionCached = true;
        }
        catch (Exception e) {
            System.err.println("[GuiAdjust] cacheInfusingReflection failed: " + e.getMessage());
        }
    }

    public int getAmountSlotX() {
        this.cacheInfusingReflection();
        try {
            return (Integer)this.rm_infInputAbsX.invoke(this.screen);
        }
        catch (Exception e) {
            return 80;
        }
    }

    public int getAmountSlotY() {
        this.cacheInfusingReflection();
        try {
            return (Integer)this.rm_infInputAbsY.invoke(this.screen) + 22;
        }
        catch (Exception e) {
            return 72;
        }
    }

    /** 绘制灌注界面的额外轮廓与标签。 */
    public void renderInfusingExtrasV2(GuiGraphics guiGraphics) {
        this.cacheInfusingReflection();
        try {
            int leftPos = this.rf_leftPos.getInt(this.screen);
            int topPos = this.rf_topPos.getInt(this.screen);
            int imageWidth = this.rf_imageWidth.getInt(this.screen);
            int imageHeight = this.rf_imageHeight.getInt(this.screen);
            Font font = (Font)this.rf_font.get(this.screen);
            Object menu = this.rf_menu.get(this.screen);
            int chemSlotX = (Integer)this.rm_getChemSlotX.invoke(this.screen);
            int chemSlotY = (Integer)this.rm_getChemSlotY.invoke(this.screen);
            // 以下四个偏移量字段的读取同时起到校验反射可用性的作用
            int infInputOffsetX = this.rf_infInputSlotOffsetX.getInt(this.screen);
            int infInputOffsetY = this.rf_infInputSlotOffsetY.getInt(this.screen);
            int infOutLineOffsetX = this.rf_infOutSlotLineOffsetX.getInt(this.screen);
            int infOutLineOffsetY = this.rf_infOutSlotLineOffsetY.getInt(this.screen);
            int inputAbsX = (Integer)this.rm_infInputAbsX.invoke(this.screen);
            int inputAbsY = (Integer)this.rm_infInputAbsY.invoke(this.screen);
            int outAbsX = (Integer)this.rm_infOutAbsX.invoke(this.screen);
            int outAbsY = (Integer)this.rm_infOutAbsY.invoke(this.screen);
            guiGraphics.renderOutline(inputAbsX, inputAbsY, 18, 18, -1);
            guiGraphics.renderOutline(outAbsX, outAbsY, 18, 18, -1);
            guiGraphics.renderOutline(chemSlotX, chemSlotY, 20, 20, -1);
            Field chemSlotDataField = menu.getClass().getDeclaredField("chemSlotData");
            chemSlotDataField.setAccessible(true);
            Object chemSlotData = chemSlotDataField.get(menu);
            if (chemSlotData != null) {
                int tintColor = chemSlotDataField.getType().getDeclaredField("tintColor").getInt(chemSlotData);
                guiGraphics.fill(chemSlotX + 1, chemSlotY + 1, chemSlotX + 17, chemSlotY + 17, Integer.MIN_VALUE | tintColor);
                Class<?> mekIntegrationClass = Class.forName("com.visualcrafting.screen.MekanismIntegration");
                Method renderIconMethod = mekIntegrationClass.getDeclaredMethod("renderChemicalIcon", GuiGraphics.class, chemSlotDataField.getType(), Integer.TYPE, Integer.TYPE, Font.class);
                renderIconMethod.invoke(null, guiGraphics, chemSlotData, chemSlotX, chemSlotY, font);
            } else {
                ItemStack chemicalStack = (ItemStack)this.rf_selectedChemical.get(this.screen);
                if (!chemicalStack.isEmpty()) {
                    int fallbackTintColor = -7829368;
                    guiGraphics.fill(chemSlotX + 1, chemSlotY + 1, chemSlotX + 17, chemSlotY + 17, Integer.MIN_VALUE | fallbackTintColor);
                }
            }
            int amountX = this.getAmountSlotX();
            int amountY = this.getAmountSlotY();
            guiGraphics.renderOutline(amountX, amountY, 50, 14, -1);
            guiGraphics.drawString(font, "mb", amountX + 52, amountY + 3, 0x404040, false);
            int inputLabelOffsetX = this.getIntOffset(INT_INPUT_LABEL_X);
            int inputLabelOffsetY = this.getIntOffset(INT_INPUT_LABEL_Y);
            guiGraphics.drawString(font, "输入", inputAbsX + inputLabelOffsetX, inputAbsY + inputLabelOffsetY, 0x404040, false);
            int outputLabelOffsetX = this.getIntOffset(INT_OUTPUT_LABEL_X);
            int outputLabelOffsetY = this.getIntOffset(INT_OUTPUT_LABEL_Y);
            guiGraphics.drawString(font, "输出", outAbsX + outputLabelOffsetX, outAbsY + outputLabelOffsetY, 0x404040, false);
            int amountLabelOffsetX = this.rf_infAmountLabelOffsetX.getInt(this.screen);
            int amountLabelOffsetY = this.rf_infAmountLabelOffsetY.getInt(this.screen);
            guiGraphics.drawString(font, "灌注量:", inputAbsX - 52 + amountLabelOffsetX, inputAbsY + 40 + amountLabelOffsetY, 0x404040, false);
            guiGraphics.drawString(font, "KubeJS", leftPos + 4, topPos + imageHeight - 15, 0x404040, false);
            for (int slotIndex = 82; slotIndex <= 117; ++slotIndex) {
                int slotX = (Integer)this.rm_slotAbsX.invoke(this.screen, slotIndex);
                int slotY = (Integer)this.rm_slotAbsY.invoke(this.screen, slotIndex);
                guiGraphics.renderOutline(slotX, slotY, 18, 18, -1);
            }
        }
        catch (Exception e) {
            System.err.println("[GuiAdjust] renderInfusingExtrasV2 failed: " + e.getMessage());
        }
    }

    private int getIntOffset(String key) {
        Integer value = this.internalOffsets.get(key);
        return value != null ? value : 0;
    }

    /** 汇总所有 *OffsetX/*OffsetY 字段并写入 jar 内的 gui_offsets.properties。 */
    public void saveOffsetsToJar() {
        byte[] propsBytes;
        try {
            Properties props = new Properties();
            for (Field field : this.screen.getClass().getDeclaredFields()) {
                String fieldName = field.getName();
                if (!fieldName.endsWith("OffsetX") && !fieldName.endsWith("OffsetY") && !fieldName.equals("tierOffsetY")) continue;
                field.setAccessible(true);
                int value = field.getInt(this.screen);
                props.setProperty(fieldName, String.valueOf(value));
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            props.store(outputStream, "VisualCrafting GUI offsets - " + new Date());
            propsBytes = outputStream.toByteArray();
        }
        catch (Exception e) {
            System.err.println("[GuiAdjust] Build props failed: " + e.getMessage());
            this.tryStatus("保存失败");
            return;
        }
        try {
            File configDir = new File("config", "visualcrafting");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            Files.write(new File(configDir, "gui_offsets.properties").toPath(), propsBytes);
        }
        catch (Exception e) {
            System.err.println("[GuiAdjust] Config save failed: " + e.getMessage());
        }
        String statusMessage = null;
        String loadedJarPath = this.findLoadedJarPath();
        if (loadedJarPath != null && this.injectPropertiesIntoJar(loadedJarPath, propsBytes)) {
            statusMessage = "坐标已保存到 jar";
        }
        if (statusMessage == null) {
            String baseJarPath = this.findBaseJarInMods();
            if (baseJarPath != null && this.injectPropertiesIntoJar(baseJarPath, propsBytes)) {
                statusMessage = "已保存到基准 jar，下次加载时生效";
            }
        }
        String message = statusMessage != null ? statusMessage : "坐标已保存";
        this.tryStatus(message);
        System.out.println("[GuiAdjust] " + message);
    }

    private void tryStatus(String message) {
        try {
            this.screen.getClass().getMethod("showStatus", String.class).invoke(this.screen, message);
        }
        catch (Exception e) {
            // 界面未提供 showStatus 时忽略
        }
    }

    /** 将属性内容写入指定 jar；jar 内已有 gui_offsets.properties 时替换，否则追加。 */
    private boolean injectPropertiesIntoJar(String jarPath, byte[] propsBytes) {
        try {
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                return false;
            }
            File tempFile = new File(jarPath + ".tmpsave");
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tempFile));
            boolean replaced = false;
            byte[] buffer = new byte[4096];
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if (entry.getName().equals("gui_offsets.properties")) {
                    JarEntry propertiesEntry = new JarEntry("gui_offsets.properties");
                    propertiesEntry.setTime(System.currentTimeMillis());
                    jarOutputStream.putNextEntry(propertiesEntry);
                    jarOutputStream.write(propsBytes);
                    jarOutputStream.closeEntry();
                    replaced = true;
                    continue;
                }
                jarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                int readCount;
                while ((readCount = jarInputStream.read(buffer)) != -1) {
                    jarOutputStream.write(buffer, 0, readCount);
                }
                jarOutputStream.closeEntry();
            }
            if (!replaced) {
                jarOutputStream.putNextEntry(new JarEntry("gui_offsets.properties"));
                jarOutputStream.write(propsBytes);
                jarOutputStream.closeEntry();
            }
            jarInputStream.close();
            jarOutputStream.close();
            Files.move(tempFile.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        catch (Exception e) {
            System.err.println("[GuiAdjust] injectPropertiesIntoJar failed for " + jarPath + ": " + e.getMessage());
            return false;
        }
    }

    /** 返回当前类所在 jar 的路径；开发环境（目录加载）下返回 null。 */
    private String findLoadedJarPath() {
        try {
            URL resourceUrl = this.getClass().getResource("/com/visualcrafting/screen/GuiAdjustManager.class");
            if (resourceUrl == null) {
                return null;
            }
            String protocol = resourceUrl.getProtocol();
            if (protocol.equals("jar")) {
                String path = resourceUrl.getPath();
                int separatorIndex = path.indexOf(33); // '!' 分隔 jar 路径与内部条目路径
                if (separatorIndex > 0) {
                    path = path.substring(0, separatorIndex);
                }
                if (path.startsWith("file:")) {
                    path = path.substring(5);
                }
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                return URLDecoder.decode(path, StandardCharsets.UTF_8);
            }
        }
        catch (Exception e) {
            // 非 jar 环境（开发环境）下忽略
        }
        return null;
    }

    /** 在 mods 目录中查找基准 jar。 */
    private String findBaseJarInMods() {
        String modsDirPath = System.getProperty("minecraft.mods.dir");
        if (modsDirPath == null) {
            modsDirPath = System.getProperty("user.dir") + File.separator + "mods";
        }
        File modsDir = new File(modsDirPath);
        File baseJar = new File(modsDir, "visualcrafting-1.3.2.5.jar");
        if (modsDir.isDirectory() && baseJar.exists()) {
            return baseJar.getAbsolutePath();
        }
        return null;
    }
}
