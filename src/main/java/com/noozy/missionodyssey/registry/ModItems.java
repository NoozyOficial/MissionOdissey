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

    public static final DeferredItem<Item> RAW_TITANIUM = ITEMS.register("raw_titanium",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.register("titanium_ingot",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
