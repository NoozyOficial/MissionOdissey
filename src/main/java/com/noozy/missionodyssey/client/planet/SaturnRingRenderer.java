package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModDimensions;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;


public class SaturnRingRenderer {

    private static final float MODEL_INNER_R = 1.40f  * (float) ModDimensions.SATURN_MODEL_RADIUS; // ≈ 2.10
    private static final float MODEL_OUTER_R = 2.375f * (float) ModDimensions.SATURN_MODEL_RADIUS; // ≈ 3.5625

    private static final float RING_INNER_REAL = (float) (ModDimensions.SATURN_RADIUS_BLOCKS * 1.25);
    private static final float RING_OUTER_REAL = (float) (ModDimensions.SATURN_RADIUS_BLOCKS * 2.375);

    public static final double CLOSE_THRESHOLD = RING_OUTER_REAL + 500.0;

    private static final double BLOCK_RENDER_RADIUS = 400.0;

    private static final ResourceLocation RING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/planets/saturn_rings.png");

    private static final int RING_BLOCK_COUNT = 10000;

    private static final float[] RING_BASE_X   = new float[RING_BLOCK_COUNT];
    private static final float[] RING_BASE_Z   = new float[RING_BLOCK_COUNT];

    private static final float[] RING_VERT_OFF = new float[RING_BLOCK_COUNT];

    private static final float[] RING_RADIUS   = new float[RING_BLOCK_COUNT];

    private static final float[] ROT_AXIS_X    = new float[RING_BLOCK_COUNT];
    private static final float[] ROT_AXIS_Y    = new float[RING_BLOCK_COUNT];
    private static final float[] ROT_AXIS_Z    = new float[RING_BLOCK_COUNT];
    private static final float[] ROT_SPEED     = new float[RING_BLOCK_COUNT];
    private static final float[] ROT_PHASE     = new float[RING_BLOCK_COUNT];
    private static final float[] BLOCK_SCALE   = new float[RING_BLOCK_COUNT];

    private static final float ORBITAL_BASE_PERIOD = 3000f;

    static {
        precomputeRingBlocks();
    }

    private static void precomputeRingBlocks() {
        java.util.Random rand = new java.util.Random(0xDA5A7000L);

        for (int i = 0; i < RING_BLOCK_COUNT; i++) {

            float bx, bz;
            do {
                bx = (float) (rand.nextFloat() * 2 * RING_OUTER_REAL - RING_OUTER_REAL);
                bz = (float) (rand.nextFloat() * 2 * RING_OUTER_REAL - RING_OUTER_REAL);
            } while (Math.max(Math.abs(bx), Math.abs(bz)) < RING_INNER_REAL ||
                     Math.max(Math.abs(bx), Math.abs(bz)) > RING_OUTER_REAL);

            RING_BASE_X[i]   = bx;
            RING_BASE_Z[i]   = bz;
            RING_RADIUS[i]   = (float) Math.sqrt(bx * bx + bz * bz);
            RING_VERT_OFF[i] = (float) (rand.nextFloat() * 10f - 5f);

            float ax = (float) (rand.nextFloat() * 2 - 1);
            float ay = (float) (rand.nextFloat() * 2 - 1);
            float az = (float) (rand.nextFloat() * 2 - 1);
            float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (len < 1e-4f) { ax = 1; ay = 0; az = 0; len = 1; }
            ROT_AXIS_X[i] = ax / len;
            ROT_AXIS_Y[i] = ay / len;
            ROT_AXIS_Z[i] = az / len;
            ROT_SPEED[i]  = 0.5f + rand.nextFloat() * 2.5f;
            ROT_PHASE[i]  = rand.nextFloat() * 360f;

            BLOCK_SCALE[i] = 2.5f + rand.nextFloat() * 1.5f;
        }
    }

    public static void renderFarRings(PoseStack matrices, MultiBufferSource consumers,
                                      int packedLight, float ringAlpha, float time, org.joml.Vector3f sunWorldDir) {

        matrices.pushPose();

        float averageOrbitalOmega = (float) (2.0 * Math.PI / ORBITAL_BASE_PERIOD);

        float rotationAngle = time * averageOrbitalOmega;

        matrices.mulPose(new Quaternionf().rotateY(rotationAngle));

        VertexConsumer buf = consumers.getBuffer(RenderType.entityTranslucent(RING_TEXTURE));
        Matrix4f mat  = matrices.last().pose();
        PoseStack.Pose entry = matrices.last();

        Vector3f localSunDir = new Vector3f(sunWorldDir);
        localSunDir.rotateZ((float) Math.toRadians(26.73f));
        localSunDir.rotateY(-rotationAngle);
        
        float planetRadius = (float) ModDimensions.SATURN_MODEL_RADIUS;

        float eps = 0.003f;
        int GRID = 32;
        float step = (MODEL_OUTER_R * 2) / GRID;
        
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                float x0 = -MODEL_OUTER_R + i * step;
                float z0 = -MODEL_OUTER_R + j * step;
                float x1 = x0 + step;
                float z1 = z0 + step;
                
                float u0 = (float) i / GRID;
                float v0 = (float) j / GRID;
                float u1 = (float) (i + 1) / GRID;
                float v1 = (float) (j + 1) / GRID;

                addRingVertex(buf, mat, entry, x0, eps, z0, u0, v0, packedLight, 0, 1, 0, ringAlpha, localSunDir, planetRadius);
                addRingVertex(buf, mat, entry, x1, eps, z0, u1, v0, packedLight, 0, 1, 0, ringAlpha, localSunDir, planetRadius);
                addRingVertex(buf, mat, entry, x1, eps, z1, u1, v1, packedLight, 0, 1, 0, ringAlpha, localSunDir, planetRadius);
                addRingVertex(buf, mat, entry, x0, eps, z1, u0, v1, packedLight, 0, 1, 0, ringAlpha, localSunDir, planetRadius);

                addRingVertex(buf, mat, entry, x0, -eps, z1, u0, v1, packedLight, 0, -1, 0, ringAlpha, localSunDir, planetRadius);
                addRingVertex(buf, mat, entry, x1, -eps, z1, u1, v1, packedLight, 0, -1, 0, ringAlpha, localSunDir, planetRadius);
                addRingVertex(buf, mat, entry, x1, -eps, z0, u1, v0, packedLight, 0, -1, 0, ringAlpha, localSunDir, planetRadius);
                addRingVertex(buf, mat, entry, x0, -eps, z0, u0, v0, packedLight, 0, -1, 0, ringAlpha, localSunDir, planetRadius);
            }
        }

        matrices.popPose();
    }

    private static float calcShadow(float px, float py, float pz, org.joml.Vector3f lightDir, float R) {
        float b = px * lightDir.x + py * lightDir.y + pz * lightDir.z;
        float rSq = px * px + py * py + pz * pz;
        float c = rSq - R * R;
        if (b < 0 && b * b > c) {
            float distToCenterSq = rSq - b * b;
            if (distToCenterSq < R * R) {
                float distToCenter = (float) Math.sqrt((double) distToCenterSq);
                float shadowStr = (distToCenter - (R - 0.2f)) / 0.2f;
                return Math.max(0.0f, Math.min(1.0f, shadowStr));
            }
        }
        return 1.0f;
    }

    private static void addRingVertex(VertexConsumer buf, Matrix4f mat, PoseStack.Pose entry,
                                       float x, float y, float z, float u, float v,
                                       int light, float nx, float ny, float nz, float alpha,
                                       org.joml.Vector3f localSunDir, float planetRadius) {
        float shadow = calcShadow(x, y, z, localSunDir, planetRadius);
        float br = (shadow * 0.95f) + 0.05f;

        buf.addVertex(mat, x, y, z)
           .setColor(br, br, br, alpha)
           .setUv(u, v)
           .setOverlay(OverlayTexture.NO_OVERLAY)
           .setLight(light)
           .setNormal(entry, nx, ny, nz);
    }

    public static void renderCloseRings(PoseStack matrices, MultiBufferSource consumers,
                                         Vec3 saturnPos, Vec3 cameraPos,
                                         float time, int packedLight, org.joml.Vector3f sunWorldDir) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null) return;

        BlockRenderDispatcher brm = mc.getBlockRenderer();
        BlockState stone = Blocks.STONE.defaultBlockState();
        VertexConsumer solidBuf = consumers.getBuffer(RenderType.solid());
        RandomSource mcRandom = RandomSource.create(42L);

        float tiltRad = (float) Math.toRadians(26.73);
        float cosT = (float) Math.cos(tiltRad);
        float sinT = (float) Math.sin(tiltRad);

        for (int i = 0; i < RING_BLOCK_COUNT; i++) {
            float r = RING_RADIUS[i];

            float orbitalOmega = (float) (2.0 * Math.PI / ORBITAL_BASE_PERIOD
                    * Math.pow(RING_INNER_REAL / r, 1.5));
            float baseAngle = (float) Math.atan2(RING_BASE_Z[i], RING_BASE_X[i]);
            float orbAngle  = baseAngle + time * orbitalOmega;

            float ringX = r * (float) Math.cos(orbAngle);
            float ringZ = r * (float) Math.sin(orbAngle);
            float ringY = RING_VERT_OFF[i];

            float worldOffX =  ringX * cosT + ringY * sinT;
            float worldOffY = -ringX * sinT + ringY * cosT;
            float worldOffZ = ringZ;

            double worldX = saturnPos.x + worldOffX;
            double worldY = saturnPos.y + worldOffY;
            double worldZ = saturnPos.z + worldOffZ;

            double dx = worldX - cameraPos.x;
            double dy = worldY - cameraPos.y;
            double dz = worldZ - cameraPos.z;
            double camDist2 = dx * dx + dy * dy + dz * dz;
            if (camDist2 > BLOCK_RENDER_RADIUS * BLOCK_RENDER_RADIUS) continue;

            float planetRadiusReal = (float) ModDimensions.SATURN_RADIUS_BLOCKS;
            float shadowPct = calcShadow(worldOffX, worldOffY, worldOffZ, sunWorldDir, planetRadiusReal);

            matrices.pushPose();
            matrices.translate(dx, dy, dz);

            float selfAngleRad = (float) Math.toRadians((time * ROT_SPEED[i] + ROT_PHASE[i]) % 360f);
            matrices.mulPose(new Quaternionf().rotateAxis(selfAngleRad,
                    ROT_AXIS_X[i], ROT_AXIS_Y[i], ROT_AXIS_Z[i]));

            float s = BLOCK_SCALE[i];
            matrices.scale(s, s, s);

            matrices.translate(-0.5, -0.5, -0.5);

            VertexConsumer consumerToUse = solidBuf;
            if (shadowPct < 0.8f) {
                float br = (shadowPct * 0.9f) + 0.1f;
                consumerToUse = new ShadowVertexConsumer(solidBuf, br);
            }

            brm.renderBatched(stone, BlockPos.ZERO, world, matrices, consumerToUse, false, mcRandom);
            matrices.popPose();
        }
    }

    private static class ShadowVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float brightness;

        public ShadowVertexConsumer(VertexConsumer delegate, float brightness) {
            this.delegate = delegate;
            this.brightness = brightness;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { 
            delegate.setColor((int)(r * brightness), (int)(g * brightness), (int)(b * brightness), a); 
            return this; 
        }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    }
}
