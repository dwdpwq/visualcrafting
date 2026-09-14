package com.visualcrafting;

import com.visualcrafting.block.ModBlocks;
import com.visualcrafting.block.VisualCraftingBlockEntity;
import com.visualcrafting.block.VisualCraftingBlockItem;
import com.visualcrafting.network.ModMessages;
import com.visualcrafting.screen.VisualCraftingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod("visualcrafting")
public class VisualCraftingTable {
    public static final String MOD_ID = "visualcrafting";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("visualcrafting");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("visualcrafting");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "visualcrafting");
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, "visualcrafting");

    public static final DeferredBlock<Block> VISUAL_CRAFTING_BLOCK =
            BLOCKS.register("visual_crafting_table", ModBlocks::createVisualCraftingBlock);

    public static final DeferredItem<Item> VISUAL_CRAFTING_ITEM =
            ITEMS.register("visual_crafting_table",
                    () -> new VisualCraftingBlockItem(VISUAL_CRAFTING_BLOCK.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<VisualCraftingBlockEntity>> VISUAL_CRAFTING_BE =
            BLOCK_ENTITIES.register("visual_crafting_table",
                    () -> BlockEntityType.Builder.of(VisualCraftingBlockEntity::new, VISUAL_CRAFTING_BLOCK.get()).build(null));

    public static final Supplier<MenuType<VisualCraftingMenu>> VISUAL_CRAFTING_MENU =
            MENU_TYPES.register("visual_crafting_table",
                    () -> IMenuTypeExtension.create(VisualCraftingMenu::new));

    public VisualCraftingTable(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        ModMessages.register(modEventBus);

        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().getPath().equals("functional_blocks")) {
            event.accept(VISUAL_CRAFTING_ITEM.get());
        }
    }
}
