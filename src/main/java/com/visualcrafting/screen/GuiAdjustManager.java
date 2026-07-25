/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.world.item.ItemStack
 */
package com.visualcrafting.screen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
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

public class GuiAdjustManager {
    public static final int GROUP_BORDER = 0;
    public static final int GROUP_SLOT = 1;
    public static final int GROUP_BUTTON = 2;
    public static final int GROUP_TEXT = 3;
    public static final int MAX_GROUPS = 4;
    private static final String[] GROUP_NAMES = new String[]{"Borders", "Slots", "Buttons", "Text"};
    private static final String[][][] CRAFTING_FIELDS = new String[][][]{{{"Grid Border", "gridOffsetX", "gridOffsetY"}, {"Output Border", "outSlotLineOffsetX", "outSlotLineOffsetY"}, {"Inventory Border", "invLineOffsetX", "invLineOffsetY"}, {"Recipes List", "recipesOffsetX", "recipesOffsetY"}}, {{"Grid Slots", "gridSlotOffsetX", "gridSlotOffsetY"}, {"Output Slot", "outSlotSlotOffsetX", "outSlotSlotOffsetY"}, {"Inventory Slots", "invSlotSlotOffsetX", "invSlotSlotOffsetY"}}, {{"Function Buttons", "btnOffsetX", "btnOffsetY"}, {"Tier Buttons Y", "tierOffsetY", "tierOffsetY"}}, {{"Inventory Label", "invLabelOffsetX", "invLabelOffsetY"}}};
    static final String INT_INPUT_LABEL_X = "v:inputLabelX";
    static final String INT_INPUT_LABEL_Y = "v:inputLabelY";
    static final String INT_OUTPUT_LABEL_X = "v:outputLabelX";
    static final String INT_OUTPUT_LABEL_Y = "v:outputLabelY";
    static final String INT_CHEM_LABEL_X = "v:chemLabelX";
    static final String INT_CHEM_LABEL_Y = "v:chemLabelY";
    static final String INT_CHEM_LINE_X = "v:chemLineX";
    static final String INT_CHEM_LINE_Y = "v:chemLineY";
    private static final String[][][] INFUSING_FIELDS = new String[][][]{{{"\u7269\u54c1\u69fd\u8fb9\u6846", "infInputSlotOffsetX", "infInputSlotOffsetY"}, {"\u8f93\u51fa\u7269\u54c1\u69fd\u8fb9\u6846", "infOutSlotLineOffsetX", "infOutSlotLineOffsetY"}, {"\u5316\u5b66\u54c1\u69fd\u8fb9\u6846", "v:chemLineX", "v:chemLineY"}, {"\u7269\u54c1\u680f\u8fb9\u6846", "infInvLineOffsetX", "infInvLineOffsetY"}, {"\u914d\u65b9\u5217\u8868", "recipesOffsetX", "recipesOffsetY"}}, {{"\u7269\u54c1\u69fd (Input Item)", "infInputSlotSlotOffsetX", "infInputSlotSlotOffsetY"}, {"\u8f93\u51fa\u7269\u54c1\u69fd (Output Item)", "infOutSlotSlotOffsetX", "infOutSlotSlotOffsetY"}, {"\u5316\u5b66\u54c1\u69fd (Chemical)", "infChemSlotOffsetX", "infChemSlotOffsetY"}, {"\u7269\u54c1\u680f (Inventory)", "infInvSlotSlotOffsetX", "infInvSlotSlotOffsetY"}, {"\u5316\u5b66\u54c1\u6570\u91cf (Chemical Amount)", "infEditBoxOffsetX", "infEditBoxOffsetY"}}, {{"\u704c\u6ce8\u6309\u94ae", "infButtonsOffsetX", "infButtonsOffsetY"}, {"\u7b49\u7ea7\u6309\u94aeY", "tierOffsetY", "tierOffsetY"}}, {{"\u7269\u54c1\u69fd\u6587\u5b57", "v:inputLabelX", "v:inputLabelY"}, {"\u8f93\u51fa\u7269\u54c1\u69fd\u6587\u5b57", "v:outputLabelX", "v:outputLabelY"}, {"\u5316\u5b66\u54c1\u6587\u5b57", "v:chemLabelX", "v:chemLabelY"}, {"\u5316\u5b66\u54c1\u6570\u91cf\u6587\u5b57", "infAmountLabelOffsetX", "infAmountLabelOffsetY"}}};
    private static final String[][] TAB_LABEL = new String[][]{{"Crafting"}, {"Infusing"}};
    private final Object screen;
    private boolean enabled = true;
    private int focusGroup = 0;
    private int activeIndex = 0;
    private boolean ctrlDown = false;
    private int lastTab = -1;
    private final HashMap<String, Integer> internalOffsets = new HashMap();
    private transient Field rf_leftPos;
    private transient Field rf_topPos;
    private transient Field rf_imageWidth;
    private transient Field rf_imageHeight;
    private transient Field rf_font;
    private transient Field rf_infInputSlotOffsetX;
    private transient Field rf_infInputSlotOffsetY;
    private transient Field rf_infOutSlotLineOffsetX;
    private transient Field rf_infOutSlotLineOffsetY;
    private transient Field rf_infAmountLabelOffsetX;
    private transient Field rf_infAmountLabelOffsetY;
    private transient Field rf_selectedChemical;
    private transient Field rf_menu;
    private transient Field rf_infEditBoxOffsetX;
    private transient Field rf_infEditBoxOffsetY;
    private transient Method rm_getChemSlotX;
    private transient Method rm_getChemSlotY;
    private transient Method rm_infInputAbsX;
    private transient Method rm_infInputAbsY;
    private transient Method rm_infOutAbsX;
    private transient Method rm_infOutAbsY;
    private transient Method rm_slotAbsX;
    private transient Method rm_slotAbsY;
    private boolean reflectionCached = false;

    public GuiAdjustManager(Object object) {
        this.screen = object;
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

    private int detectTab() {
        try {
            Method method = this.screen.getClass().getMethod("getMode", new Class[0]);
            return (Integer)method.invoke(this.screen, new Object[0]);
        }
        catch (Exception exception) {
            return 0;
        }
    }

    private String[][][] currentFields() {
        int n = this.detectTab();
        if (n != this.lastTab) {
            this.lastTab = n;
            this.activeIndex = 0;
        }
        return n == 1 ? INFUSING_FIELDS : CRAFTING_FIELDS;
    }

    public boolean keyPressed(int n, int n2, int n3) {
        if (n == 341 || n == 345) {
            this.ctrlDown = true;
            return false;
        }
        if (this.ctrlDown && n == 83) {
            this.saveOffsetsToJar();
            return true;
        }
        if (n == 340 || n == 344) {
            this.focusGroup = (this.focusGroup + 1) % 4;
            this.activeIndex = 0;
            return true;
        }
        int n4 = this.ctrlDown ? 5 : 1;
        String[][][] stringArray = this.currentFields();
        String[][] stringArray2 = stringArray[this.focusGroup];
        if (this.activeIndex >= stringArray2.length) {
            this.activeIndex = 0;
        }
        String[] stringArray3 = stringArray2[this.activeIndex];
        try {
            switch (n) {
                case 263: {
                    this.adjustOffset(stringArray3[1], -n4);
                    return true;
                }
                case 262: {
                    this.adjustOffset(stringArray3[1], n4);
                    return true;
                }
                case 265: {
                    this.adjustOffset(stringArray3[2], -n4);
                    return true;
                }
                case 264: {
                    this.adjustOffset(stringArray3[2], n4);
                    return true;
                }
                case 258: {
                    this.activeIndex = (this.activeIndex + 1) % stringArray2.length;
                    return true;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    public boolean keyReleased(int n, int n2, int n3) {
        if (n == 341 || n == 345) {
            this.ctrlDown = false;
            return true;
        }
        return false;
    }

    private void adjustOffset(String string, int n) throws Exception {
        if (string.startsWith("v:")) {
            Integer n2 = this.internalOffsets.get(string);
            if (n2 == null) {
                n2 = 0;
            }
            this.internalOffsets.put(string, n2 + n);
            this.screen.getClass().getMethod("rebuildWidgetsPublic", new Class[0]).invoke(this.screen, new Object[0]);
            return;
        }
        Field field = this.screen.getClass().getDeclaredField(string);
        field.setAccessible(true);
        int n3 = field.getInt(this.screen);
        field.setInt(this.screen, n3 + n);
        this.screen.getClass().getMethod("rebuildWidgetsPublic", new Class[0]).invoke(this.screen, new Object[0]);
    }

    public void renderOverlay(GuiGraphics guiGraphics, Font font) {
        String[][][] stringArray;
        String[][] stringArray2;
        if (!this.enabled) {
            return;
        }
        int n = this.detectTab();
        if (n != this.lastTab) {
            this.lastTab = n;
            this.activeIndex = 0;
        }
        if (this.activeIndex >= (stringArray2 = (stringArray = n == 1 ? INFUSING_FIELDS : CRAFTING_FIELDS)[this.focusGroup]).length) {
            this.activeIndex = 0;
        }
        try {
            int n2 = this.screen.getClass().getField("width").getInt(this.screen);
            int n3 = n2 - 220;
            int n4 = 10;
            int n5 = 210;
            int n6 = 20 + stringArray2.length * 14 + 60;
            guiGraphics.fill(n3, n4, n3 + n5, n4 + n6, -872415232);
            guiGraphics.fill(n3, n4, n3 + n5, n4 + 1, -1);
            guiGraphics.fill(n3, n4, n3 + 1, n4 + n6, -1);
            guiGraphics.fill(n3 + n5 - 1, n4, n3 + n5, n4 + n6, -1);
            guiGraphics.fill(n3, n4 + n6 - 1, n3 + n5, n4 + n6, -1);
            int n7 = n4 + 6;
            String string = n >= 0 && n < TAB_LABEL.length ? TAB_LABEL[n][0] : "?";
            guiGraphics.drawString(font, "GUI Adjust [" + string + "]: " + GROUP_NAMES[this.focusGroup], n3 + 6, n7, -11141291, false);
            guiGraphics.drawString(font, "Tab=Next  Arrow=Move  Shift=Group  Ctrl+S=Save", n3 + 6, n7 += 14, -5592406, false);
            n7 += 14;
            for (int i = 0; i < stringArray2.length; ++i) {
                int n8;
                int n9;
                Object object;
                int n10 = i == this.activeIndex ? -256 : -5592406;
                String string2 = i == this.activeIndex ? "> " : "  ";
                String string3 = stringArray2[i][1];
                String string4 = stringArray2[i][2];
                if (string3.startsWith("v:")) {
                    object = this.internalOffsets.get(string3);
                    n9 = object != null ? (Integer)object : 0;
                } else {
                    object = this.screen.getClass().getDeclaredField(string3);
                    ((Field)object).setAccessible(true);
                    n9 = ((Field)object).getInt(this.screen);
                }
                if (string4.startsWith("v:")) {
                    object = this.internalOffsets.get(string4);
                    n8 = object != null ? (Integer)object : 0;
                } else {
                    object = this.screen.getClass().getDeclaredField(string4);
                    ((Field)object).setAccessible(true);
                    n8 = ((Field)object).getInt(this.screen);
                }
                object = string3.equals(string4) ? "(" + n9 + ")" : "(" + n9 + "," + n8 + ")";
                guiGraphics.drawString(font, string2 + stringArray2[i][0] + " " + (String)object, n3 + 6, n7, n10, false);
                n7 += 14;
            }
            guiGraphics.drawString(font, "Ctrl+Arrows = 5px step", n3 + 6, n7 += 4, -7829249, false);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void cacheInfusingReflection() {
        if (this.reflectionCached) {
            return;
        }
        try {
            Class<?> clazz = this.screen.getClass();
            Class<?> clazz2 = clazz.getSuperclass();
            this.rf_leftPos = clazz2.getDeclaredField("leftPos");
            this.rf_leftPos.setAccessible(true);
            this.rf_topPos = clazz2.getDeclaredField("topPos");
            this.rf_topPos.setAccessible(true);
            this.rf_imageWidth = clazz2.getDeclaredField("imageWidth");
            this.rf_imageWidth.setAccessible(true);
            this.rf_imageHeight = clazz2.getDeclaredField("imageHeight");
            this.rf_imageHeight.setAccessible(true);
            this.rf_font = clazz2.getDeclaredField("font");
            this.rf_font.setAccessible(true);
            this.rf_infInputSlotOffsetX = clazz.getDeclaredField("infInputSlotOffsetX");
            this.rf_infInputSlotOffsetX.setAccessible(true);
            this.rf_infInputSlotOffsetY = clazz.getDeclaredField("infInputSlotOffsetY");
            this.rf_infInputSlotOffsetY.setAccessible(true);
            this.rf_infOutSlotLineOffsetX = clazz.getDeclaredField("infOutSlotLineOffsetX");
            this.rf_infOutSlotLineOffsetX.setAccessible(true);
            this.rf_infOutSlotLineOffsetY = clazz.getDeclaredField("infOutSlotLineOffsetY");
            this.rf_infOutSlotLineOffsetY.setAccessible(true);
            this.rf_infAmountLabelOffsetX = clazz.getDeclaredField("infAmountLabelOffsetX");
            this.rf_infAmountLabelOffsetX.setAccessible(true);
            this.rf_infAmountLabelOffsetY = clazz.getDeclaredField("infAmountLabelOffsetY");
            this.rf_infAmountLabelOffsetY.setAccessible(true);
            this.rf_selectedChemical = clazz.getDeclaredField("selectedChemical");
            this.rf_selectedChemical.setAccessible(true);
            this.rf_menu = clazz2.getDeclaredField("menu");
            this.rf_menu.setAccessible(true);
            this.rf_infEditBoxOffsetX = clazz.getDeclaredField("infEditBoxOffsetX");
            this.rf_infEditBoxOffsetX.setAccessible(true);
            this.rf_infEditBoxOffsetY = clazz.getDeclaredField("infEditBoxOffsetY");
            this.rf_infEditBoxOffsetY.setAccessible(true);
            this.rm_getChemSlotX = clazz.getDeclaredMethod("getChemSlotX", new Class[0]);
            this.rm_getChemSlotY = clazz.getDeclaredMethod("getChemSlotY", new Class[0]);
            this.rm_infInputAbsX = clazz.getDeclaredMethod("infInputAbsX", new Class[0]);
            this.rm_infInputAbsX.setAccessible(true);
            this.rm_infInputAbsY = clazz.getDeclaredMethod("infInputAbsY", new Class[0]);
            this.rm_infInputAbsY.setAccessible(true);
            this.rm_infOutAbsX = clazz.getDeclaredMethod("infOutAbsX", new Class[0]);
            this.rm_infOutAbsX.setAccessible(true);
            this.rm_infOutAbsY = clazz.getDeclaredMethod("infOutAbsY", new Class[0]);
            this.rm_infOutAbsY.setAccessible(true);
            this.rm_slotAbsX = clazz.getDeclaredMethod("slotAbsX", Integer.TYPE);
            this.rm_slotAbsX.setAccessible(true);
            this.rm_slotAbsY = clazz.getDeclaredMethod("slotAbsY", Integer.TYPE);
            this.rm_slotAbsY.setAccessible(true);
            this.reflectionCached = true;
        }
        catch (Exception exception) {
            System.err.println("[GuiAdjust] cacheInfusingReflection failed: " + exception.getMessage());
        }
    }

    public int getAmountSlotX() {
        this.cacheInfusingReflection();
        try {
            return (Integer)this.rm_infInputAbsX.invoke(this.screen, new Object[0]);
        }
        catch (Exception exception) {
            return 80;
        }
    }

    public int getAmountSlotY() {
        this.cacheInfusingReflection();
        try {
            return (Integer)this.rm_infInputAbsY.invoke(this.screen, new Object[0]) + 22;
        }
        catch (Exception exception) {
            return 72;
        }
    }

    public void renderInfusingExtrasV2(GuiGraphics guiGraphics) {
        this.cacheInfusingReflection();
        try {
            int n;
            int n2 = this.rf_leftPos.getInt(this.screen);
            int n3 = this.rf_topPos.getInt(this.screen);
            int n4 = this.rf_imageWidth.getInt(this.screen);
            int n5 = this.rf_imageHeight.getInt(this.screen);
            Font font = (Font)this.rf_font.get(this.screen);
            Object object = this.rf_menu.get(this.screen);
            int n6 = (Integer)this.rm_getChemSlotX.invoke(this.screen, new Object[0]);
            int n7 = (Integer)this.rm_getChemSlotY.invoke(this.screen, new Object[0]);
            int n8 = this.rf_infInputSlotOffsetX.getInt(this.screen);
            int n9 = this.rf_infInputSlotOffsetY.getInt(this.screen);
            int n10 = this.rf_infOutSlotLineOffsetX.getInt(this.screen);
            int n11 = this.rf_infOutSlotLineOffsetY.getInt(this.screen);
            int n12 = (Integer)this.rm_infInputAbsX.invoke(this.screen, new Object[0]);
            int n13 = (Integer)this.rm_infInputAbsY.invoke(this.screen, new Object[0]);
            int n14 = (Integer)this.rm_infOutAbsX.invoke(this.screen, new Object[0]);
            int n15 = (Integer)this.rm_infOutAbsY.invoke(this.screen, new Object[0]);
            guiGraphics.renderOutline(n12, n13, 18, 18, -1);
            guiGraphics.renderOutline(n14, n15, 18, 18, -1);
            guiGraphics.renderOutline(n6, n7, 20, 20, -1);
            Field field = object.getClass().getDeclaredField("chemSlotData");
            field.setAccessible(true);
            Object object2 = field.get(object);
            if (object2 != null) {
                n = field.getType().getDeclaredField("tintColor").getInt(object2);
                guiGraphics.fill(n6 + 1, n7 + 1, n6 + 17, n7 + 17, Integer.MIN_VALUE | n);
                Object mekIntegration = Class.forName("com.visualcrafting.screen.MekanismIntegration");
                Method method = ((Class)mekIntegration).getDeclaredMethod("renderChemicalIcon", GuiGraphics.class, field.getType(), Integer.TYPE, Integer.TYPE, Font.class);
                method.invoke(null, guiGraphics, object2, n6, n7, font);
            } else {
                ItemStack chemicalStack = (ItemStack)this.rf_selectedChemical.get(this.screen);
                if (!chemicalStack.isEmpty()) {
                    int n16 = -7829368;
                    guiGraphics.fill(n6 + 1, n7 + 1, n6 + 17, n7 + 17, Integer.MIN_VALUE | n16);
                }
            }
            n = this.getAmountSlotX();
            int n17 = this.getAmountSlotY();
            guiGraphics.renderOutline(n, n17, 50, 14, -1);
            guiGraphics.drawString(font, "mb", n + 52, n17 + 3, 0x404040, false);
            int n18 = this.getIntOffset(INT_INPUT_LABEL_X);
            int n19 = this.getIntOffset(INT_INPUT_LABEL_Y);
            guiGraphics.drawString(font, "\u8f93\u5165", n12 + n18, n13 + n19, 0x404040, false);
            int n20 = this.getIntOffset(INT_OUTPUT_LABEL_X);
            int n21 = this.getIntOffset(INT_OUTPUT_LABEL_Y);
            guiGraphics.drawString(font, "\u8f93\u51fa", n14 + n20, n15 + n21, 0x404040, false);
            int n22 = this.rf_infAmountLabelOffsetX.getInt(this.screen);
            int n23 = this.rf_infAmountLabelOffsetY.getInt(this.screen);
            guiGraphics.drawString(font, "\u704c\u6ce8\u91cf:", n12 - 52 + n22, n13 + 40 + n23, 0x404040, false);
            guiGraphics.drawString(font, "KubeJS", n2 + 4, n3 + n5 - 15, 0x404040, false);
            for (int i = 82; i <= 117; ++i) {
                int n24 = (Integer)this.rm_slotAbsX.invoke(this.screen, i);
                int n25 = (Integer)this.rm_slotAbsY.invoke(this.screen, i);
                guiGraphics.renderOutline(n24, n25, 18, 18, -1);
            }
        }
        catch (Exception exception) {
            System.err.println("[GuiAdjust] renderInfusingExtrasV2 failed: " + exception.getMessage());
        }
    }

    private int getIntOffset(String string) {
        Integer n = this.internalOffsets.get(string);
        return n != null ? n : 0;
    }

    public void saveOffsetsToJar() {
        Object object;
        Object object2;
        Object object3;
        byte[] byArray = null;
        try {
            object3 = new Properties();
            for (Field field : this.screen.getClass().getDeclaredFields()) {
                String string = field.getName();
                if (!string.endsWith("OffsetX") && !string.endsWith("OffsetY") && !string.equals("tierOffsetY")) continue;
                field.setAccessible(true);
                int n = field.getInt(this.screen);
                ((Properties)object3).setProperty(string, String.valueOf(n));
            }
            object = new ByteArrayOutputStream();
            ((Properties)object3).store((OutputStream)object, "VisualCrafting GUI offsets - " + String.valueOf(new Date()));
            byArray = ((ByteArrayOutputStream)object).toByteArray();
        }
        catch (Exception exception) {
            System.err.println("[GuiAdjust] Build props failed: " + exception.getMessage());
            this.tryStatus("\u4fdd\u5b58\u5931\u8d25");
            return;
        }
        try {
            object3 = new File("config", "visualcrafting");
            if (!((File)object3).exists()) {
                ((File)object3).mkdirs();
            }
            Files.write(new File((File)object3, "gui_offsets.properties").toPath(), byArray, new OpenOption[0]);
        }
        catch (Exception exception) {
            System.err.println("[GuiAdjust] Config save failed: " + exception.getMessage());
        }
        object3 = null;
        object2 = this.findLoadedJarPath();
        if (object2 != null && this.injectPropertiesIntoJar((String)object2, byArray)) {
            object3 = "\u5750\u6807\u5df2\u4fdd\u5b58\u5230 jar";
        }
        if (object3 == null && (object = this.findBaseJarInMods()) != null && this.injectPropertiesIntoJar((String)object, byArray)) {
            object3 = "\u5df2\u4fdd\u5b58\u5230\u57fa\u51c6 jar\uff0c\u4e0b\u6b21\u52a0\u8f7d\u65f6\u751f\u6548";
        }
        object = object3 != null ? object3 : "\u5750\u6807\u5df2\u4fdd\u5b58";
        this.tryStatus((String)object);
        System.out.println("[GuiAdjust] " + (String)object);
    }

    private void tryStatus(String string) {
        try {
            this.screen.getClass().getMethod("showStatus", String.class).invoke(this.screen, string);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private boolean injectPropertiesIntoJar(String string, byte[] byArray) {
        try {
            JarEntry jarEntry;
            File file = new File(string);
            if (!file.exists()) {
                return false;
            }
            File file2 = new File(string + ".tmpsave");
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file));
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file2));
            boolean bl = false;
            byte[] byArray2 = new byte[4096];
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                int n;
                if (jarEntry.getName().equals("gui_offsets.properties")) {
                    jarEntry = new JarEntry("gui_offsets.properties");
                    jarEntry.setTime(System.currentTimeMillis());
                    jarOutputStream.putNextEntry(jarEntry);
                    jarOutputStream.write(byArray);
                    jarOutputStream.closeEntry();
                    bl = true;
                    continue;
                }
                jarOutputStream.putNextEntry(new JarEntry(jarEntry.getName()));
                while ((n = jarInputStream.read(byArray2)) != -1) {
                    jarOutputStream.write(byArray2, 0, n);
                }
                jarOutputStream.closeEntry();
            }
            if (!bl) {
                jarOutputStream.putNextEntry(new JarEntry("gui_offsets.properties"));
                jarOutputStream.write(byArray);
                jarOutputStream.closeEntry();
            }
            jarInputStream.close();
            jarOutputStream.close();
            Files.move(file2.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        catch (Exception exception) {
            System.err.println("[GuiAdjust] injectPropertiesIntoJar failed for " + string + ": " + exception.getMessage());
            return false;
        }
    }

    private String findLoadedJarPath() {
        try {
            URL uRL = this.getClass().getResource("/com/visualcrafting/screen/GuiAdjustManager.class");
            if (uRL == null) {
                return null;
            }
            String string = uRL.getProtocol();
            if (string.equals("jar")) {
                String string2 = uRL.getPath();
                int n = string2.indexOf(33);
                if (n > 0) {
                    string2 = string2.substring(0, n);
                }
                if (string2.startsWith("file:")) {
                    string2 = string2.substring(5);
                }
                if (string2.startsWith("/")) {
                    string2 = string2.substring(1);
                }
                return URLDecoder.decode(string2, StandardCharsets.UTF_8);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private String findBaseJarInMods() {
        File file;
        File file2;
        Object object = System.getProperty("minecraft.mods.dir");
        if (object == null) {
            object = System.getProperty("user.dir") + File.separator + "mods";
        }
        if ((file2 = new File((String)object)).isDirectory() && (file = new File(file2, "visualcrafting-1.3.2.5.jar")).exists()) {
            return file.getAbsolutePath();
        }
        return null;
    }
}

