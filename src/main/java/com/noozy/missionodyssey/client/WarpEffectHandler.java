package com.noozy.missionodyssey.client;

import com.mojang.math.Axis;
import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.entity.SpaceshipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Drives the client-side Star-Wars-style hyperspace jump animation.
 *
 * Timeline (ticks):
 *   0 – 19  : CHARGING    — engine revs, FOV slowly climbs, speed-lines fade in
 *  20 – 29  : STRETCHING  — FOV rockets up, lines at full intensity
 *  30 – 37  : FLASH       — max FOV, white screen (server fires teleport here)
 *  38 – 59  : RECOVERY    — FOV drops back, lines and overlay fade out
 */
@EventBusSubscriber(modid = MissionOdyssey.MODID, value = Dist.CLIENT)
public class WarpEffectHandler {

    // ── Phase lengths ────────────────────────────────────────────────
    private static final int T_CHARGE   = 20;
    private static final int T_STRETCH  = 10;
    private static final int T_FLASH    = 8;
    private static final int T_RECOVERY = 22;
    private static final int T_TOTAL    = T_CHARGE + T_STRETCH + T_FLASH + T_RECOVERY; // 60

    // ── State ────────────────────────────────────────────────────────
    private static int  warpTick          = -1;   // -1 = idle
    public  static int  clientCooldown    = 0;    // counts down to 0
    public  static int  maxCooldown       = 600;  // 30 s

    // ── Public API ───────────────────────────────────────────────────

    public static void startWarp() {
        warpTick = 0;
    }

    public static void setCooldown(int ticks) {
        clientCooldown = ticks;
    }

    public static boolean isActive() {
        return warpTick >= 0;
    }

    // ── Derived animation values (sampled each frame with partial tick) ──

    /**
     * FOV multiplier to apply on top of the player's base FOV.
     * 1.0 = no change, 1.85 = peak hyperspace stretch.
     */
    public static float getFovMultiplier(float partial) {
        if (warpTick < 0) return 1.0f;
        float t = warpTick + partial;

        if (t < T_CHARGE) {
            // Linear rise: 1.00 → 1.12
            return 1.0f + (t / T_CHARGE) * 0.12f;
        }
        t -= T_CHARGE;

        if (t < T_STRETCH) {
            // Quadratic ease-in: 1.12 → 1.85
            float p = t / T_STRETCH;
            return 1.12f + p * p * 0.73f;
        }
        t -= T_STRETCH;

        if (t < T_FLASH) {
            // Hold at peak
            return 1.85f;
        }
        t -= T_FLASH;

        // Quadratic ease-out back to 1.0
        float p = 1.0f - (t / T_RECOVERY);
        return 1.0f + p * p * 0.85f;
    }

    /**
     * Intensity of the radial speed-line overlay [0, 1].
     */
    public static float getLineIntensity(float partial) {
        if (warpTick < 0) return 0.0f;
        float t = warpTick + partial;

        if (t < T_CHARGE)
            return (t / T_CHARGE) * 0.55f;

        t -= T_CHARGE;
        if (t < T_STRETCH)
            return 0.55f + (t / T_STRETCH) * 0.45f;

        t -= T_STRETCH;
        if (t < T_FLASH)
            return 1.0f;

        t -= T_FLASH;
        return Mth.clamp(1.0f - (t / T_RECOVERY), 0, 1);
    }

    /**
     * White-flash screen overlay alpha [0, 1].
     */
    public static float getFlashAlpha(float partial) {
        if (warpTick < 0) return 0.0f;
        float t = warpTick + partial;
        float flashStart = T_CHARGE + T_STRETCH;
        float flashEnd   = flashStart + T_FLASH;
        float recovEnd   = flashEnd + T_RECOVERY;

        if (t < flashStart) return 0.0f;

        if (t < flashEnd) {
            float p = (t - flashStart) / T_FLASH;
            return Math.min(1.0f, p * 4.0f);   // fully white by 25% into flash phase
        }

        if (t < recovEnd) {
            float p = (t - flashEnd) / T_RECOVERY;
            return (1.0f - p) * (1.0f - p) * 0.85f;  // ease-out fade
        }

        return 0.0f;
    }

    // ── NeoForge event hooks ─────────────────────────────────────────

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (clientCooldown > 0) clientCooldown--;

        if (warpTick >= 0) {
            warpTick++;
            if (warpTick >= T_TOTAL) {
                warpTick = -1;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        float mult = getFovMultiplier((float) event.getPartialTick());
        if (mult != 1.0f) {
            event.setFOV(event.getFOV() * mult);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean inShip = mc.player.getVehicle() instanceof SpaceshipEntity;
        float partial  = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        float lines = getLineIntensity(partial);
        float flash = getFlashAlpha(partial);

        if (lines <= 0 && flash <= 0 && clientCooldown <= 0 && !inShip) return;

        GuiGraphics gui = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // ── Speed lines ──────────────────────────────────────────────
        if (lines > 0.01f && inShip) {
            renderSpeedLines(gui, w, h, lines);
        }

        // ── White flash overlay ──────────────────────────────────────
        if (flash > 0.01f) {
            int a = (int) (flash * 255);
            gui.fill(0, 0, w, h, (a << 24) | 0x00FFFFFF);
        }

        // ── Cooldown / ready indicator (only while riding) ───────────
        if (inShip) {
            renderCooldownHUD(gui, w, h);
        }
    }

    // ── Rendering helpers ────────────────────────────────────────────

    /**
     * 48 radial "hyperspace star-streak" lines emanating from the screen centre.
     * Uses PoseStack rotation so each line draws along the rotated X axis,
     * giving the classic Star-Wars tunnel-of-light effect.
     */
    private static void renderSpeedLines(GuiGraphics gui, int w, int h, float intensity) {
        int cx = w / 2;
        int cy = h / 2;
        float maxLen = Math.min(w, h) * 0.52f * intensity;

        var pose = gui.pose();

        for (int i = 0; i < 48; i++) {
            float angleDeg = i * (360f / 48f);

            // Deterministic per-line variation (no Random object allocation per frame)
            float lenFactor = 0.4f + 0.6f * ((i * 13 + 7) % 11 / 11f);
            float lineLen   = maxLen * lenFactor;
            float inner     = 8 + (i * 5 % 14);
            if (lineLen < 3) continue;

            float lineAlpha = intensity * (0.25f + 0.75f * intensity);
            int a     = (int) (lineAlpha * 220);
            int color = (a << 24) | 0x00FFFFFF;

            pose.pushPose();
            pose.translate(cx, cy, 50);                       // z=50 → on top of other GUI
            pose.mulPose(Axis.ZP.rotationDegrees(angleDeg)); // rotate so +X = outward direction
            // Thin 2-pixel-tall rectangle stretching from inner to inner+len
            gui.fill((int) inner, -1, (int) (inner + lineLen), 1, color);
            pose.popPose();
        }
    }

    /**
     * Small warp-drive status bar centred just below the crosshair.
     *
     * • Cooldown active → depleting blue bar
     * • Ready           → softly pulsing cyan bar (jump available)
     */
    private static void renderCooldownHUD(GuiGraphics gui, int w, int h) {
        final int BAR_W = 100;
        final int BAR_H = 4;
        int barX = w / 2 - BAR_W / 2;
        int barY = h / 2 + 22;   // just below the crosshair

        boolean ready = clientCooldown <= 0 && !isActive();

        if (ready) {
            // Soft pulse using system clock
            float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() * 0.004);
            int a = (int) (pulse * 200);

            gui.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, 0x40FFFFFF);
            gui.fill(barX, barY, barX + BAR_W, barY + BAR_H, (a << 24) | 0x0060FF);
        } else if (clientCooldown > 0) {
            float progress = 1f - (float) clientCooldown / maxCooldown;
            int fill = (int) (BAR_W * progress);

            // Background track
            gui.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, 0x40000000);
            gui.fill(barX, barY, barX + BAR_W, barY + BAR_H, 0x50111133);

            // Filled portion (dim blue → bright blue depending on charge)
            if (fill > 0) {
                gui.fill(barX, barY, barX + fill, barY + BAR_H, 0xC00050CC);
            }
        }
    }
}
