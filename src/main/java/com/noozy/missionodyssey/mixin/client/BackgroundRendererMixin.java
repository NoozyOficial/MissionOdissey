package com.noozy.missionodyssey.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.noozy.missionodyssey.registry.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remove completamente a fog na dimensão do espaço.
 *
 * Saturno é renderizado via fake-distance (máx 2000 blocos), então a fog padrão
 * do Minecraft corta o planeta até o limite de renderização configurado (render distance).
 * No espaço, a visibilidade deve ser infinita — sem fog.
 */
@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {

    @Inject(
            method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
            at = @At("RETURN")
    )
    private static void spaceDimensionNoFog(Camera camera, net.minecraft.client.renderer.FogRenderer.FogMode fogType,
                                             float farPlaneDistance, boolean shouldCreateFog, float partialTick,
                                             CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension().equals(ModDimensions.SPACE_KEY)) {
            // Empurra o início e fim da fog para "infinito" — efetivamente desativa a fog
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        }
    }
}
