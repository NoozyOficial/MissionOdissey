package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.screen.TitaniumBlastFurnaceMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, MissionOdyssey.MODID);

    public static final Supplier<MenuType<TitaniumBlastFurnaceMenu>> TITANIUM_BLAST_FURNACE_MENU = MENUS.register("titanium_blast_furnace_menu",
            () -> IMenuTypeExtension.create(TitaniumBlastFurnaceMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
