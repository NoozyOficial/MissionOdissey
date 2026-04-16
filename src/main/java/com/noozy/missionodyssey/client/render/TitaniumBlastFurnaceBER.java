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

public class TitaniumBlastFurnaceBER implements BlockEntityRenderer<TitaniumBlastFurnaceBlockEntity> {
    private static final ResourceLocation MODEL_LOCATION = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "models/multiblock/titanium_blast_furnace.odyssey.json");
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/multiblock/titanium_blast_furnace.png");
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
