package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItemGroups {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MissionOdyssey.MODID);

    public static final Supplier<CreativeModeTab> BISMUTH_ITEMS_TAB = CREATIVE_MODE_TAB.register("bismuth_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.SPACESHIP_ITEM.get()))
                    .title(Component.translatable("itemGroup.missionodyssey"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.SPACESHIP_ITEM.get());
                        output.accept(ModBlocks.MARS_SAND.get());
                        output.accept(ModBlocks.MARS_STONE.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
