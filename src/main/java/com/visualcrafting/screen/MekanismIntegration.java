package com.visualcrafting.screen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public class MekanismIntegration {

    private static final boolean MEKANISM_LOADED = ModList.get().isLoaded("mekanism");

    public static boolean isLoaded() {
        return MEKANISM_LOADED;
    }

    public static int getChemicalColorFromTag(CompoundTag tag) {
        if (!MEKANISM_LOADED || tag == null) return 0;
        String chemicalId = tag.getString("chemicalId");
        if (chemicalId.isEmpty()) return 0;
        try {
            Object chem = lookupChemical(ResourceLocation.parse(chemicalId));
            if (chem != null) {
                try {
                    return (int) chem.getClass().getMethod("getTint").invoke(chem);
                } catch (AbstractMethodError | NoSuchMethodError e) {
                    return 0;
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static Object lookupChemical(ResourceLocation rl) {
        try {
            Class<?> apiClass = Class.forName("mekanism.api.MekanismAPI");
            java.lang.reflect.Method regMethod = apiClass.getMethod("chemicalRegistry");
            Object registry = regMethod.invoke(null);
            java.lang.reflect.Method getValue = registry.getClass().getMethod("getValue", ResourceLocation.class);
            return getValue.invoke(registry, rl);
        } catch (Exception e) {
            return null;
        }
    }

    private static String detectChemicalType(Object chemical) {
        return "unknown";
    }

    public static <I> boolean isChemicalIngredient(I ingredient) {
        return false;
    }

    public static <I> CompoundTag convertChemicalToTag(I ingredient) {
        return new CompoundTag();
    }

    public static <I> net.minecraft.world.item.ItemStack createChemicalTagItem(CompoundTag tag) {
        return net.minecraft.world.item.Items.BARRIER.getDefaultInstance();
    }

    public static <I> ChemSlotData buildChemSlotData(I ingredient) {
        return null;
    }

    public static ChemSlotData buildChemSlotDataFromTag(CompoundTag tag) {
        return null;
    }

    public static CompoundTag getChemicalTagFromItem(net.minecraft.world.item.ItemStack stack) {
        return new CompoundTag();
    }

    public static void renderChemicalIcon(
            net.minecraft.client.gui.GuiGraphics graphics,
            ChemSlotData data, int x, int y,
            net.minecraft.client.gui.Font font) {
    }

    public static void renderChemSlotData(
            net.minecraft.client.gui.GuiGraphics graphics,
            ChemSlotData data, int x, int y,
            net.minecraft.client.gui.Font font) {
    }

    public static void renderChemicalTag(
            net.minecraft.client.gui.GuiGraphics graphics,
            CompoundTag tag, int x, int y,
            net.minecraft.client.gui.Font font) {
    }

    public static Object parseChemicalIngredient(String s) {
        return null;
    }

    public static boolean matchesIngredient(Object chem, Object ingredient) {
        return false;
    }

    public static boolean matchesTag(Object chem, String tag) {
        return false;
    }

    public static boolean matchesChemicalIngredient(Object ingredient, String s) {
        return false;
    }
}
