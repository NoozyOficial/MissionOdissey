package com.noozy.missionodyssey.client;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.entity.SpaceshipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Drives the client-side warp-jump animation.
 *
 * Timeline (ticks):
 *   0 – 19  : CHARGING    — engine revs, FOV slowly climbs, speed-line overlay fades in
 *  20 – 29  : STRETCHING  — FOV rockets up, overlay at full opacity
 *  30 – 37  : FLASH       — max FOV, white screen (server fires teleport here)
 *  38 – 59  : RECOVERY    — FOV drops back, overlay fades out
 *
 * The speed-line effect is rendered as a full-screen texture blit of
 * {@code textures/misc/speed_lines.png}, mirroring the approach Minecraft
 * uses for the freezing / powder-snow overlay in {@code ScreenEffectRenderer}.
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

    /** Texture location for the speed-line overlay. */
    private static final ResourceLocation SPEED_LINES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/misc/speed_lines.png");

    /**
     * Renders the warp speed-line overlay by blitting {@code speed_lines.png}
     * across the full screen, exactly like Minecraft's powder-snow / freezing
     * overlay in {@code ScreenEffectRenderer}.
     *
     * @param gui       current GuiGraphics context
     * @param w         GUI scaled screen width
     * @param h         GUI scaled screen height
     * @param intensity overlay opacity in [0, 1]
     */
    private static void renderSpeedLines(GuiGraphics gui, int w, int h, float intensity) {
        // Quadratic ease so the overlay punches in quickly at higher intensities.
        float alpha = Mth.clamp(intensity * (0.25f + 0.75f * intensity), 0.0f, 1.0f);

        // Tint via RenderSystem — identical to how ScreenEffectRenderer handles
        // the powder-snow / pumpkin overlays in vanilla.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // Stretch the texture across the full screen (UV 0→w, 0→h).
        gui.blit(SPEED_LINES_TEXTURE, 0, 0, 0.0f, 0.0f, w, h, w, h);

        // Always restore colour & blend state after custom rendering.
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
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
