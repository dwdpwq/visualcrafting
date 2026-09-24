package com.visualcrafting.screen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class DropdownWidget
        extends AbstractWidget {
        private final VisualCraftingScreen owner;
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
        public DropdownWidget(VisualCraftingScreen owner, int x, int y, int width) {
        super(x, y, width, BUTTON_HEIGHT, Component.empty());
        this.owner = owner;
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
        int screenBottom = this.owner.topPos + this.owner.imageHeight;
        int dropdownHeight = this.getDropdownHeight();
        int dropdownY = this.getY() + BUTTON_HEIGHT;
        if (dropdownY + dropdownHeight > screenBottom) {
        dropdownY = this.getY() - dropdownHeight;
        }

        if (dropdownY < this.owner.topPos) {
        dropdownY = this.owner.topPos;
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
        if (this.onSelect != null) {
        this.onSelect.accept(clickedOptionIdx);
        }

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
        graphics.drawString(this.owner.font, this.getMessage(), this.getX() + 4, this.getY() + 4, textColor, false);
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

        String checkmark = isSelected ? "☑" : "☐";
        graphics.drawString(this.owner.font, checkmark, this.getX() + 4, rowY + 2, isSelected ? 0x55FF55 : 0x808080, false);
        graphics.drawString(this.owner.font, this.options.get(optionIdx), this.getX() + 20, rowY + 2, 0xFFFFFF, false);
        continue;
        }

        if (optionIdx == this.selectedIdx) {
        graphics.fill(this.getX() + 1, rowY, this.getX() + this.width - 1, rowY + ROW_HEIGHT, 0x40FFFFFF);
        }

        graphics.drawString(this.owner.font, this.options.get(optionIdx), this.getX() + 4, rowY + 2, 0xFFFFFF, false);
        }

        if (this.options.size() > visibleRows) {
        int totalPages = Math.max(0, this.options.size() - visibleRows);
        String scrollText = (this.scrollOffset + 1) + "/" + (totalPages + 1);
        int scrollTextWidth = this.owner.font.width(scrollText);
        graphics.drawString(this.owner.font, scrollText,
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
