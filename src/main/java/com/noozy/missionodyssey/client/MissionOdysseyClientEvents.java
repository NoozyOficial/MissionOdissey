package com.noozy.missionodyssey.client;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.client.dimension.SpaceDimensionEffects;
import com.noozy.missionodyssey.client.entity.renderer.SpaceshipRenderer;
import com.noozy.missionodyssey.registry.ModDimensions;
import com.noozy.missionodyssey.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = MissionOdyssey.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MissionOdysseyClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPACESHIP.get(), SpaceshipRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "space"), new SpaceDimensionEffects());
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeybindings.TEMPORAL_JUMP);
    }
}
