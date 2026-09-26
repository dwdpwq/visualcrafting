package com.visualcrafting.screen;

import com.visualcrafting.VisualCraftingTable;
import com.visualcrafting.block.VisualCraftingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.Optional;

public class VisualCraftingMenu extends AbstractContainerMenu {

    public static final int MAX_GRID = 81;
    public static final int GRID_START = 0;
    public static final int OUTPUT_SLOT = MAX_GRID;       // 81
    public static final int PROFESSION_BLOCK_SLOT = OUTPUT_SLOT + 1; // 82
    public static final int PLAYER_START = PROFESSION_BLOCK_SLOT + 1; // 83
    public static final int PLAYER_END = PLAYER_START + 36; // 119

    public final BlockPos blockPos;
    public final VisualCraftingBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    public final Container craftSlots;
    public final Container resultSlot;
    public final Container professionBlockSlot;
    public ChemSlotData chemSlotData;
    public int chemAmount;

    private int tier = 1;
    private int currentMode = 0;

    /* chemGhostItems[0..8] for infusing chem slot ghosts */
    public final CompoundTag[] chemGhostItems = new CompoundTag[9];

    /* Cached reflection fields for Slot.x / Slot.y – matches benchmark */
    private static final Field SLOT_X_FIELD;
    private static final Field SLOT_Y_FIELD;

    static {
        Field fx = null;
        Field fy = null;
        try {
            fx = Slot.class.getDeclaredField("x");
            fy = Slot.class.getDeclaredField("y");
            fx.setAccessible(true);
            fy.setAccessible(true);
        } catch (NoSuchFieldException e) {
            logWarn("Failed to access Slot.x / Slot.y fields via reflection", e);
        }
        SLOT_X_FIELD = fx;
        SLOT_Y_FIELD = fy;
    }

    // ======================== Constructors ========================

    public VisualCraftingMenu(int id, Inventory playerInv) {
        this(id, playerInv, BlockPos.ZERO);
    }

    public VisualCraftingMenu(int id, Inventory playerInv, VisualCraftingBlockEntity blockEntity) {
        this(id, playerInv, ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
             blockEntity.getBlockPos());
        this.tier = blockEntity.getTier();
        updateSlotPositions(this.tier);
    }

    public VisualCraftingMenu(int id, Inventory playerInv, FriendlyByteBuf extra) {
        this(id, playerInv, extra != null ? extra.readBlockPos() : BlockPos.ZERO);
    }

    public VisualCraftingMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, ContainerLevelAccess.NULL, pos);
        if (playerInv.player.level().getBlockEntity(pos) instanceof VisualCraftingBlockEntity be) {
            this.tier = be.getTier();
        }
        updateSlotPositions(this.tier);
    }

    /**
     * Primary constructor – stores access for stillValid().
     */
    public VisualCraftingMenu(int id, Inventory playerInv, ContainerLevelAccess access, BlockPos pos) {
        super(VisualCraftingTable.VISUAL_CRAFTING_MENU.get(), id);
        this.blockPos = pos;
        this.access = access;
        this.craftSlots = new SimpleContainer(MAX_GRID);
        this.resultSlot = new SimpleContainer(1);
        this.professionBlockSlot = new SimpleContainer(1);

        // Resolve block entity from access (best-effort)
        VisualCraftingBlockEntity resolved = null;
        try {
            resolved = access.evaluate((level, bp) -> {
                if (level.getBlockEntity(bp) instanceof VisualCraftingBlockEntity be) {
                    return be;
                }
                return null;
            }).orElse(null);
        } catch (Exception ignored) {}
        this.blockEntity = resolved;

        buildSlots(playerInv);

        // Try to sync tier from block entity
        access.execute((level, bp) -> {
            if (level.getBlockEntity(bp) instanceof VisualCraftingBlockEntity be) {
                this.tier = be.getTier();
            }
        });

        updateSlotPositions(this.tier);
    }

    // ======================== Slot building ========================

    private void buildSlots(Inventory playerInv) {
        // Crafting grid slots (0 to MAX_GRID-1)
        for (int i = 0; i < MAX_GRID; i++) {
            this.addSlot(new Slot(craftSlots, i, 0, 0));
        }
        // Output slot (index MAX_GRID = 81)
        this.addSlot(new ResultSlot(resultSlot, 0, 0, 0));

        // Villager profession block slot (82): accepts only block items.
        this.addSlot(new Slot(professionBlockSlot, 0, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof net.minecraft.world.item.BlockItem;
            }
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player inventory (slots PLAYER_START .. PLAYER_START+26)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 0, 0));
            }
        }
        // Hotbar (slots PLAYER_START+27 .. PLAYER_START+35)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 0, 0));
        }
    }

    // ======================== Tier management ========================

    public int getTier() {
        return this.tier;
    }

    public void setTier(int t) {
        if (this.tier != t) {
            this.tier = t;
            updateSlotPositions(t);
            // Sync tier to block entity on server side
            if (this.blockEntity != null && this.blockEntity.getLevel() != null
                && !this.blockEntity.getLevel().isClientSide) {
                this.blockEntity.setTier(t);
            }
        }
    }

    // ======================== updateSlotPositions ========================

    /**
     * Exact slot layout matching the benchmark jar (dynamic positioning per tier).
     *
     * tier → gridParam mapping:  1→5, 2→7, 3→9, 4→3
     * Grid base:    (52, 13) × 18 px per cell
     * Output slot:  dynamic: X = 52 + gridParam*18 + 20, Y = 13 + (gridParam*18 - 18) / 2
     * Player inv:   dynamic: Y = 13 + gridParam*18 + 8 + row*18
     * Hotbar:       dynamic: Y = playerInvBaseY + 58
     * Unused slots: (-2000, -2000) hidden
     */
    public void updateSlotPositions(int tier) {
        int gridParam = switch (tier) {
            case 1 -> 5;
            case 2 -> 7;
            case 3 -> 9;
            default -> 3;  // tier 4+ → 3×3
        };

        final int BASE_X = 52;
        final int BASE_Y = 13;

        // Dynamic positioning (matching jar slot6..slot10 local variables)
        int outSlotX  = BASE_X + gridParam * 18 + 20;
        int outSlotY  = BASE_Y + (gridParam * 18 - 18) / 2;
        int invBaseY  = BASE_Y + gridParam * 18 + 8;
        int hotbarY   = invBaseY + 54 + 4;

        try {
            Field fx = SLOT_X_FIELD;
            Field fy = SLOT_Y_FIELD;
            if (fx == null || fy == null) return;

            // Grid slots only positioned in crafting mode (currentMode == 0).
            // Non-crafting modes (1=infusing, 2=ore, 5=food) manage slot
            // positions via mode-specific init*Widgets methods in the Screen.
            if (this.currentMode == 0) {
                // Active grid slots (0 .. gridParam²-1)
                int idx = 0;
                for (int row = 0; row < gridParam; row++) {
                    for (int col = 0; col < gridParam; col++) {
                        Slot slot = this.slots.get(idx);
                        fx.setInt(slot, BASE_X + col * 18);
                        fy.setInt(slot, BASE_Y + row * 18);
                        idx++;
                    }
                }

                // Hide unused grid slots (gridParam² .. 80)
                for (int i = idx; i < MAX_GRID; i++) {
                    Slot slot = this.slots.get(i);
                    fx.setInt(slot, -2000);
                    fy.setInt(slot, -2000);
                }
            }

            // Villager profession block slot is only visible in mode 3.
            Slot professionSlot = this.slots.get(PROFESSION_BLOCK_SLOT);
            if (this.currentMode == 3) {
                fx.setInt(professionSlot, 8);
                fy.setInt(professionSlot, 24);
            } else {
                fx.setInt(professionSlot, -2000);
                fy.setInt(professionSlot, -2000);
            }

            // Output slot (index 81) – dynamic position
            Slot outSlot = this.slots.get(OUTPUT_SLOT);
            fx.setInt(outSlot, outSlotX);
            fy.setInt(outSlot, outSlotY);

            // Player inventory (82–108) – 3 rows × 9 columns
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    Slot slot = this.slots.get(PLAYER_START + row * 9 + col);
                    fx.setInt(slot, 8 + col * 18);
                    fy.setInt(slot, invBaseY + row * 18);
                }
            }

            // Hotbar (109–117)
            for (int col = 0; col < 9; col++) {
                Slot slot = this.slots.get(PLAYER_START + 27 + col);
                fx.setInt(slot, 8 + col * 18);
                fy.setInt(slot, hotbarY);
            }
        } catch (Exception ignored) {}
    }

    // ======================== Container methods ========================

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index == PROFESSION_BLOCK_SLOT) {
                if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= PLAYER_START) {
                // 村民交易页：玩家背包中的方块可快速放入职业方块槽。
                if (this.currentMode == 3 && stack.getItem() instanceof net.minecraft.world.item.BlockItem
                        && !this.slots.get(PROFESSION_BLOCK_SLOT).hasItem()) {
                    ItemStack copy = stack.copyWithCount(1);
                    this.slots.get(PROFESSION_BLOCK_SLOT).set(copy);
                    stack.shrink(1);
                    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
                    else slot.setChanged();
                    return result;
                }
                if (!this.moveItemStackTo(stack, 0, MAX_GRID, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
            (level, pos) -> level.getBlockState(pos).is(VisualCraftingTable.VISUAL_CRAFTING_BLOCK.get())
                ? Boolean.TRUE
                : Boolean.FALSE,
            true
        );
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Return items from grid slots to the player
        for (int i = GRID_START; i < MAX_GRID; i++) {
            Slot slot = this.slots.get(i);
            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                slot.set(ItemStack.EMPTY);
                player.drop(stack, false);
            }
        }

        // Return the custom villager profession block to the player.
        Slot professionSlot = this.slots.get(PROFESSION_BLOCK_SLOT);
        if (professionSlot.hasItem()) {
            ItemStack stack = professionSlot.getItem();
            professionSlot.set(ItemStack.EMPTY);
            player.drop(stack, false);
        }
    }

    // ======================== Utility ========================

    public Optional<VisualCraftingBlockEntity> getBlockEntity() {
        return Optional.ofNullable(this.blockEntity);
    }

    public int getCraftSlotCount() {
        return MAX_GRID;
    }

    /**
     * Returns grid size based on current tier.
     * Matches benchmark mapping: 1→5, 2→7, 3→9, 4+→3
     */
    public int getGridSize() {
        return switch (this.tier) {
            case 1 -> 5;
            case 2 -> 7;
            case 3 -> 9;
            default -> 3;
        };
    }

    public int getCurrentMode() {
        return this.currentMode;
    }

    public void setCurrentMode(int mode) {
        this.currentMode = mode;
    }

    // ======================== Chem ghost data ========================

    public CompoundTag getChemGhost(int slot) {
        if (slot < 0 || slot >= 9) return new CompoundTag();
        CompoundTag tag = chemGhostItems[slot];
        return tag != null ? tag : new CompoundTag();
    }

    public void setChemGhost(int slot, CompoundTag tag) {
        if (slot >= 0 && slot < 9) {
            chemGhostItems[slot] = tag;
        }
    }

    public void clearChemGhost(int slot) {
        if (slot >= 0 && slot < 9) {
            chemGhostItems[slot] = null;
        }
    }

    // ======================== Reflection helpers ========================

    private static void setSlotX(Slot slot, int x) {
        try {
            if (SLOT_X_FIELD != null) SLOT_X_FIELD.setInt(slot, x);
        } catch (Exception ignored) {}
    }

    private static void setSlotY(Slot slot, int y) {
        try {
            if (SLOT_Y_FIELD != null) SLOT_Y_FIELD.setInt(slot, y);
        } catch (Exception ignored) {}
    }

    private static void logWarn(String msg, Throwable t) {
        System.err.println("[VisualCrafting] " + msg);
        if (t != null) t.printStackTrace();
    }

    // ======================== ResultSlot (matches jar VisualCraftingMenu$1) ========================

    private class ResultSlot extends Slot {
        public ResultSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }
}
