package com.noozy.missionodyssey.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public class TitaniumBlastFurnaceScreen extends AbstractContainerScreen<TitaniumBlastFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/gui/titanium_blast_furnace_gui.png");

    public TitaniumBlastFurnaceScreen(TitaniumBlastFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, x, y);
        renderEnergyBar(guiGraphics, x, y);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if(menu.getScaledProgress() > 0) {
            guiGraphics.blit(TEXTURE, x + 79, y + 35, 176, 0, menu.getScaledProgress(), 15);
        }
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int energyHeight = menu.getScaledEnergy();


        if (energyHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 162, y + 6 + (72 - energyHeight), 176, 16 + (72 - energyHeight), 8, energyHeight);
        }
    }

    private boolean isHoveringEnergyBar(int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        return mouseX >= x + 162 && mouseX <= x + 162 + 8 && mouseY >= y + 6 && mouseY <= y + 6 + 72;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHoveringEnergyBar(mouseX, mouseY)) {
            int energy = menu.getEnergy();
            int maxEnergy = menu.getMaxEnergy();
            guiGraphics.renderTooltip(font, List.of(Component.literal(energy + " / " + maxEnergy + " FE")), java.util.Optional.empty(), mouseX, mouseY);
        }
    }
}
