package com.noozy.missionodyssey;

import com.mojang.logging.LogUtils;
import com.noozy.missionodyssey.network.ModNetworking;
import com.noozy.missionodyssey.client.render.TitaniumBlastFurnaceBER;
import com.noozy.missionodyssey.registry.ModBlockEntities;
import com.noozy.missionodyssey.registry.ModCapabilities;
import com.noozy.missionodyssey.registry.ModMenuTypes;
import com.noozy.missionodyssey.registry.ModRecipes;
import com.noozy.missionodyssey.registry.ModBlocks;
import com.noozy.missionodyssey.screen.TitaniumBlastFurnaceScreen;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.noozy.missionodyssey.registry.ModEntities;
import com.noozy.missionodyssey.registry.ModItemGroups;
import com.noozy.missionodyssey.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(MissionOdyssey.MODID)
public class MissionOdyssey {
    public static final String MODID = "missionodyssey";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MissionOdyssey(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItemGroups.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCapabilities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetworking::register);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.TITANIUM_BLAST_FURNACE.get(), TitaniumBlastFurnaceBER::new);
        }

        @SubscribeEvent
        public static void registerMenus(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.TITANIUM_BLAST_FURNACE_MENU.get(), TitaniumBlastFurnaceScreen::new);
        }
    }
}
