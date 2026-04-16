package com.noozy.missionodyssey.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.block.TitaniumBlastFurnaceBlock;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnaceBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class TitaniumBlastFurnaceBER implements BlockEntityRenderer<TitaniumBlastFurnaceBlockEntity> {
    private static final ResourceLocation MODEL_LOCATION = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "models/multiblock/titanium_blast_furnace.odyssey.json");
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/multiblock/titanium_blast_furnace.png");

    private static final ResourceLocation INPUT_PORT = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/multiblock/titanium_blast_furnace_input_port.png");
    private static final ResourceLocation OUTPUT_PORT = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/multiblock/titanium_blast_furnace_output_port.png");
    private static final ResourceLocation ENERGY_PORT = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/multiblock/titanium_blast_furnace_energy_port.png");

    private OdysseyModel model;

    public TitaniumBlastFurnaceBER(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TitaniumBlastFurnaceBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!be.getBlockState().getValue(TitaniumBlastFurnaceBlock.ASSEMBLED)) {
            return;
        }

        if (model == null) {
            model = OdysseyModel.load(MODEL_LOCATION);
        }
        if (model == null) return;

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        Direction facing = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        float rotation = facing.toYRot();

        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation + 180));

        poseStack.translate(0, -0.5, 1.0);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE_LOCATION));

        String currentAnim = be.getCurrentState().name().toLowerCase();
        float animTime = be.getAnimTime(partialTicks);

        int lightAtCenter = net.minecraft.client.renderer.LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing, 1).above());

        OdysseyRenderer.render(model, poseStack, buffer, lightAtCenter, packedOverlay, animTime, currentAnim);

        poseStack.popPose();

        renderPorts(be, poseStack, bufferSource, packedLight);
    }

    private void renderPorts(TitaniumBlastFurnaceBlockEntity be, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        be.getPorts().forEach((pos, map) -> {
            map.forEach((dir, type) -> {
                if (type != TitaniumBlastFurnaceBlockEntity.PortType.NONE) {
                    int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(be.getLevel(), pos.relative(dir));
                    renderPortOverlay(be, pos, dir, type, poseStack, bufferSource, light);
                }
            });
        });
    }

    private void renderPortOverlay(TitaniumBlastFurnaceBlockEntity be, net.minecraft.core.BlockPos pos, Direction dir, TitaniumBlastFurnaceBlockEntity.PortType type, PoseStack poseStack, MultiBufferSource bufferSource, int portLight) {
        ResourceLocation texture = switch (type) {
            case ITEM_INPUT -> INPUT_PORT;
            case ITEM_OUTPUT -> OUTPUT_PORT;
            case ENERGY_INPUT -> ENERGY_PORT;
            default -> null;
        };
        if (texture == null) return;

        poseStack.pushPose();

        net.minecraft.core.BlockPos relativePos = pos.subtract(be.getBlockPos());
        poseStack.translate(relativePos.getX(), relativePos.getY(), relativePos.getZ());

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(texture));

        switch (dir) {
            case NORTH -> renderFace(poseStack, buffer, 180, 0, portLight);
            case SOUTH -> renderFace(poseStack, buffer, 0, 0, portLight);
            case EAST -> renderFace(poseStack, buffer, 90, 0, portLight);
            case WEST -> renderFace(poseStack, buffer, 270, 0, portLight);
            case UP -> renderFace(poseStack, buffer, 0, -90, portLight);
            case DOWN -> renderFace(poseStack, buffer, 0, 90, portLight);
        }

        poseStack.popPose();
    }

    private void renderFace(PoseStack poseStack, VertexConsumer buffer, float rotY, float rotX, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        if (rotY != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        if (rotX != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.translate(-0.5, -0.5, 0.501);

        buffer.addVertex(poseStack.last().pose(), 0, 0, 0).setColor(1f, 1f, 1f, 1f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 0, 1);
        buffer.addVertex(poseStack.last().pose(), 1, 0, 0).setColor(1f, 1f, 1f, 1f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 0, 1);
        buffer.addVertex(poseStack.last().pose(), 1, 1, 0).setColor(1f, 1f, 1f, 1f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 0, 1);
        buffer.addVertex(poseStack.last().pose(), 0, 1, 0).setColor(1f, 1f, 1f, 1f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 0, 1);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TitaniumBlastFurnaceBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
