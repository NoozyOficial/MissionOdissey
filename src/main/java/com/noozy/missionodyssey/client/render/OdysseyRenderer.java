package com.noozy.missionodyssey.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class OdysseyRenderer {

    public static void render(OdysseyModel model, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float animTime, String currentAnim) {
        if (model == null) return;

        poseStack.pushPose();


        poseStack.pushPose();


        String animToSearch = currentAnim.toLowerCase();
        if (animToSearch.equals("pre_assemble")) animToSearch = "pré_assemble";

        Map<String, OdysseyModel.Bone> bones = model.bones;
        for (OdysseyModel.Bone bone : bones.values()) {
            if (bone.parentName == null || !bones.containsKey(bone.parentName)) {
                renderBone(model, bone, poseStack, buffer, packedLight, packedOverlay, animTime, animToSearch);
            }
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderBone(OdysseyModel model, OdysseyModel.Bone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float animTime, String currentAnim) {
        poseStack.pushPose();


        poseStack.translate(bone.pivot.x() / 16f, bone.pivot.y() / 16f, bone.pivot.z() / 16f);


        if (currentAnim != null && model.animations.containsKey(currentAnim)) {
            OdysseyModel.Animation anim = model.animations.get(currentAnim);
            OdysseyModel.BoneAnimation boneAnim = anim.boneAnimations.get(bone.name);
            if (boneAnim != null) {
                applyAnimation(boneAnim, poseStack, animTime, anim.duration);
            }
        }


        poseStack.mulPose(Axis.ZP.rotationDegrees(bone.rotation.z()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bone.rotation.y()));
        poseStack.mulPose(Axis.XP.rotationDegrees(bone.rotation.x()));


        for (OdysseyModel.Cube cube : bone.cubes) {
            poseStack.pushPose();


            poseStack.translate(-bone.pivot.x() / 16f, -bone.pivot.y() / 16f, -bone.pivot.z() / 16f);
            renderCube(cube, poseStack, buffer, packedLight, packedOverlay, model.textureSize);
            poseStack.popPose();
        }


        for (OdysseyModel.Bone child : model.bones.values()) {
            if (bone.name.equals(child.parentName)) {


                poseStack.pushPose();
                poseStack.translate(-bone.pivot.x() / 16f, -bone.pivot.y() / 16f, -bone.pivot.z() / 16f);
                renderBone(model, child, poseStack, buffer, packedLight, packedOverlay, animTime, currentAnim);
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static void applyAnimation(OdysseyModel.BoneAnimation anim, PoseStack poseStack, float time, float duration) {
        if (duration <= 0) {

            Vector3f pos = interpolate(anim.translateFrames, 0);
            if (pos != null) poseStack.translate(pos.x() / 16f, pos.y() / 16f, pos.z() / 16f);

            Vector3f rot = interpolate(anim.rotateFrames, 0);
            if (rot != null) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(rot.z()));
                poseStack.mulPose(Axis.YP.rotationDegrees(rot.y()));
                poseStack.mulPose(Axis.XP.rotationDegrees(rot.x()));
            }

            Vector3f scale = interpolate(anim.scaleFrames, 0);
            if (scale != null) poseStack.scale(scale.x(), scale.y(), scale.z());
            return;
        }



        float t = time;
        if (duration > 0) {
            t = time % duration;


            if (time >= duration) {
                 t = duration;
            }
        }

        Vector3f pos = interpolate(anim.translateFrames, t);
        if (pos != null) poseStack.translate(pos.x() / 16f, pos.y() / 16f, pos.z() / 16f);

        Vector3f rot = interpolate(anim.rotateFrames, t);
        if (rot != null) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rot.z()));
            poseStack.mulPose(Axis.YP.rotationDegrees(rot.y()));
            poseStack.mulPose(Axis.XP.rotationDegrees(rot.x()));
        }

        Vector3f scale = interpolate(anim.scaleFrames, t);
        if (scale != null) poseStack.scale(scale.x(), scale.y(), scale.z());
    }

    private static Vector3f interpolate(List<OdysseyModel.Keyframe> frames, float time) {
        if (frames.isEmpty()) return null;
        if (frames.size() == 1) return frames.get(0).value;

        OdysseyModel.Keyframe first = frames.get(0);
        if (time <= first.time) return first.value;

        OdysseyModel.Keyframe last = frames.get(frames.size() - 1);
        if (time >= last.time) return last.value;

        for (int i = 0; i < frames.size() - 1; i++) {
            OdysseyModel.Keyframe k1 = frames.get(i);
            OdysseyModel.Keyframe k2 = frames.get(i + 1);
            if (time >= k1.time && time <= k2.time) {
                float f = (time - k1.time) / (k2.time - k1.time);

                return new Vector3f(k1.value).lerp(k2.value, f);
            }
        }
        return last.value;
    }

    private static void renderCube(OdysseyModel.Cube cube, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, Vector3f texSize) {
        float x1 = cube.from.x() / 16f;
        float y1 = cube.from.y() / 16f;
        float z1 = cube.from.z() / 16f;
        float x2 = (cube.from.x() + cube.size.x()) / 16f;
        float y2 = (cube.from.y() + cube.size.y()) / 16f;
        float z2 = (cube.from.z() + cube.size.z()) / 16f;

        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();


        if (cube.uvs.containsKey("bottom")) {
            float[] uv = cube.uvs.get("bottom");
            addQuad(matrix4f, matrix3f, buffer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, uv, texSize, 0, -1, 0, packedLight, packedOverlay);
        }

        if (cube.uvs.containsKey("top")) {
            float[] uv = cube.uvs.get("top");
            addQuad(matrix4f, matrix3f, buffer, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, uv, texSize, 0, 1, 0, packedLight, packedOverlay);
        }

        if (cube.uvs.containsKey("north")) {
            float[] uv = cube.uvs.get("north");

            addQuad(matrix4f, matrix3f, buffer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, uv, texSize, 0, 0, 1, packedLight, packedOverlay);
        }

        if (cube.uvs.containsKey("south")) {
            float[] uv = cube.uvs.get("south");

            addQuad(matrix4f, matrix3f, buffer, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, uv, texSize, 0, 0, -1, packedLight, packedOverlay);
        }

        if (cube.uvs.containsKey("west")) {
            float[] uv = cube.uvs.get("west");
            addQuad(matrix4f, matrix3f, buffer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, uv, texSize, -1, 0, 0, packedLight, packedOverlay);
        }

        if (cube.uvs.containsKey("east")) {
            float[] uv = cube.uvs.get("east");
            addQuad(matrix4f, matrix3f, buffer, x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2, uv, texSize, 1, 0, 0, packedLight, packedOverlay);
        }
    }

    private static void addQuad(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float[] uv, Vector3f texSize, float nx, float ny, float nz, int packedLight, int packedOverlay) {

        Vector3f normal = new Vector3f(nx, ny, nz).mul(matrix3f);

        float uMin = uv[0] / texSize.x();
        float vMin = uv[1] / texSize.y();
        float uMax = uv[2] / texSize.x();
        float vMax = uv[3] / texSize.y();

        buffer.addVertex(matrix4f, x1, y1, z1).setColor(1f, 1f, 1f, 1f).setUv(uMin, vMax).setOverlay(packedOverlay).setLight(packedLight).setNormal(normal.x(), normal.y(), normal.z());
        buffer.addVertex(matrix4f, x2, y2, z2).setColor(1f, 1f, 1f, 1f).setUv(uMax, vMax).setOverlay(packedOverlay).setLight(packedLight).setNormal(normal.x(), normal.y(), normal.z());
        buffer.addVertex(matrix4f, x3, y3, z3).setColor(1f, 1f, 1f, 1f).setUv(uMax, vMin).setOverlay(packedOverlay).setLight(packedLight).setNormal(normal.x(), normal.y(), normal.z());
        buffer.addVertex(matrix4f, x4, y4, z4).setColor(1f, 1f, 1f, 1f).setUv(uMin, vMin).setOverlay(packedOverlay).setLight(packedLight).setNormal(normal.x(), normal.y(), normal.z());
    }
}
