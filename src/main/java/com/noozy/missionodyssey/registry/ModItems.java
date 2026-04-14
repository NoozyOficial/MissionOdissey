package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.item.SpaceshipItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MissionOdyssey.MODID);

    public static final DeferredItem<Item> SPACESHIP_ITEM = ITEMS.register("spaceship",
            () -> new SpaceshipItem(new Item.Properties().stacksTo(1)));

    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> MARS_STONE_ITEM = ITEMS.registerSimpleBlockItem("mars_stone", ModBlocks.MARS_STONE);
    
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> MARS_SAND_ITEM = ITEMS.registerSimpleBlockItem("mars_sand", ModBlocks.MARS_SAND);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
    
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SPACESHIP_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(MARS_STONE_ITEM);
            event.accept(MARS_SAND_ITEM);
        }
    }
}
