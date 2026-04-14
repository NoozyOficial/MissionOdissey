package com.noozy.missionodyssey.mixin.client;

import com.noozy.missionodyssey.entity.SpaceshipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Zoom out cinematográfico quando montado na nave.
 *
 * A câmera faz lerp suave de 4.0 (padrão) até SHIP_DISTANCE,
 * e volta suavemente quando desmonta. Dá aquele charme de cutscene.
 */
@Mixin(Camera.class)
public class CameraMixin {

    // ─── Configuração ──────────────────────────────────────────────
    @Unique
    private static final float SHIP_DISTANCE = 14.0f;      // Distância da câmera na nave
    @Unique
    private static final float DEFAULT_DISTANCE = 4.0f;     // Distância padrão do Minecraft
    @Unique
    private static final float ZOOM_OUT_SPEED = 0.035f;     // Velocidade do zoom out (mais baixo = mais cinematográfico)
    @Unique
    private static final float ZOOM_IN_SPEED = 0.06f;       // Velocidade do zoom in (volta mais rápido)

    // ─── Estado ────────────────────────────────────────────────────
    @Unique
    private static float currentDistance = DEFAULT_DISTANCE;

    /**
     * Intercepta o argumento de getMaxZoom(float) dentro de Camera.setup()
     * e substitui pela distância lerp'd quando está na nave.
     */
    @ModifyArg(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"
            )
    )
    private float missaoodisseia$modifyCameraDistance(float original) {
        Minecraft client = Minecraft.getInstance();

        boolean onShip = client.player != null
                && client.player.getVehicle() instanceof SpaceshipEntity;

        float target = onShip ? SHIP_DISTANCE : DEFAULT_DISTANCE;

        // Lerp diferente pra zoom in vs zoom out — sair é mais lento (cinematográfico)
        float speed = (target > currentDistance) ? ZOOM_OUT_SPEED : ZOOM_IN_SPEED;
        currentDistance = Mth.lerp(speed, currentDistance, target);

        // Snap quando chega perto o suficiente (evita lerp infinito)
        if (Math.abs(currentDistance - target) < 0.01f) {
            currentDistance = target;
        }

        return currentDistance;
    }
}
