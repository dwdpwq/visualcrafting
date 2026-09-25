package com.visualcrafting.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public class FoodTabRenderer {
    private final VisualCraftingScreen owner;
    private final List<String> potionIds = new ArrayList<>();
    private final List<String> potionNames = new ArrayList<>();
    private final DropdownWidget potionDropdown;
    private EditBox nutritionField;
    private EditBox saturationField;
    private EditBox eatSecondsField;
    private EditBox durationField;
    private EditBox levelField;
    private Checkbox infiniteCheckbox;
    private int guiLeft;
    private int guiTop;

    public FoodTabRenderer(VisualCraftingScreen owner) {
        this.owner = owner;
        this.potionDropdown = new DropdownWidget(owner, 0, 0, 64);
        this.loadPotionData();
        this.potionDropdown.setOptions(this.potionNames, 0);
    }

    private void loadPotionData() {
        this.potionIds.clear();
        this.potionNames.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        Registry<MobEffect> registry = minecraft.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
        for (MobEffect mobEffect : registry) {
            ResourceLocation resourceLocation = BuiltInRegistries.MOB_EFFECT.getKey(mobEffect);
            if (resourceLocation == null) {
                continue;
            }
            this.potionIds.add(resourceLocation.toString());
            String translationKey = "effect." + resourceLocation.getNamespace() + "." + resourceLocation.getPath();
            String displayName = Language.getInstance().getOrDefault(translationKey);
            this.potionNames.add(displayName.equals(translationKey) ? resourceLocation.getPath() : displayName);
        }
    }

    public void updatePosition(int guiLeft, int guiTop) {
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        this.potionDropdown.setPosition(guiLeft + 93, guiTop + 64);
    }

    public void setWidgets(EditBox nutritionField, EditBox saturationField, EditBox eatSecondsField,
                           EditBox durationField, EditBox levelField, Checkbox infiniteCheckbox) {
        this.nutritionField = nutritionField;
        this.saturationField = saturationField;
        this.eatSecondsField = eatSecondsField;
        this.durationField = durationField;
        this.levelField = levelField;
        this.infiniteCheckbox = infiniteCheckbox;
    }

    public DropdownWidget getPotionDropdown() {
        return this.potionDropdown;
    }

    public String getSelectedPotionId() {
        int idx = this.potionDropdown.getSelectedIdx();
        return idx >= 0 && idx < this.potionIds.size() ? this.potionIds.get(idx) : "minecraft:speed";
    }

    public void drawBg(GuiGraphics guiGraphics) {
        int x = this.guiLeft;
        int y = this.guiTop;
        Font font = Minecraft.getInstance().font;
        int textColor = -2039584;
        guiGraphics.drawString(font, "Food Editor", x + 20, y + 4, 16766720);
        guiGraphics.renderOutline(x + 93, y + 16, 18, 18, -1);
        guiGraphics.renderOutline(x + 93, y + 39, 18, 18, -1);
        guiGraphics.drawString(font, Component.translatable("gui.visualcrafting.mode5.label.food").getString(), x + 96 - font.width(Component.translatable("gui.visualcrafting.mode5.label.food").getString()), y + 20, textColor);
        guiGraphics.drawString(font, Component.translatable("gui.visualcrafting.mode5.label.hunger").getString(), x + 140 - font.width(Component.translatable("gui.visualcrafting.mode5.label.hunger").getString()), y + 19, textColor);
        guiGraphics.drawString(font, Component.translatable("gui.visualcrafting.mode5.label.saturation").getString(), x + 199 - font.width(Component.translatable("gui.visualcrafting.mode5.label.saturation").getString()), y + 19, textColor);
        guiGraphics.drawString(font, Component.translatable("gui.visualcrafting.mode5.label.return_item").getString(), x + 84 - font.width(Component.translatable("gui.visualcrafting.mode5.label.return_item").getString()), y + 45, textColor);
        guiGraphics.drawString(font, Component.translatable("gui.visualcrafting.mode5.label.duration").getString(), x + 133 - font.width(Component.translatable("gui.visualcrafting.mode5.label.duration").getString()), y + 44, textColor);
        guiGraphics.drawString(font, Component.translatable("gui.visualcrafting.mode5.label.potion_effects").getString(), x + 73 - font.width(Component.translatable("gui.visualcrafting.mode5.label.potion_effects").getString()), y + 68, textColor);
        guiGraphics.drawString(font, Component.translatable("gui.visualcrafting.mode5.label.level").getString(), x + 197 - font.width(Component.translatable("gui.visualcrafting.mode5.label.level").getString()), y + 68, textColor);
        guiGraphics.drawString(font, Component.translatable("gui.visualcrafting.mode5.label.eat_time").getString(), x + 88 - font.width(Component.translatable("gui.visualcrafting.mode5.label.eat_time").getString()), y + 91, textColor);
        this.potionDropdown.render(guiGraphics, x + 93, y + 64, 0.0f);
    }
}
