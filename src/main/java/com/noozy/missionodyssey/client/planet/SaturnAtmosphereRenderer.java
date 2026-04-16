package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.registry.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class SaturnAtmosphereRenderer {


    private static final float ATMO_RADIUS = (float) (ModDimensions.SATURN_MODEL_RADIUS * 1.2);
    private static final float SHADOW_RADIUS = (float) (ModDimensions.SATURN_MODEL_RADIUS * 1.05);
    private static final int CUBE_VERT_COUNT = 36;


    private static int shaderProgram = 0;
    private static int vaoId = 0;
    private static int vboId = 0;
    private static int uModelViewLoc;
    private static int uProjLoc;
    private static int uSunDirViewLoc;





    private static final String VERT_SRC =
            "#version 150 core\n" +
                    "in vec3 Position;\n" +
                    "uniform mat4 ModelViewMat;\n" +
                    "uniform mat4 ProjMat;\n" +
                    "out vec3 vNormalView;\n" +
                    "out vec3 vViewPos;\n" +
                    "void main() {\n" +
                    "    vNormalView = mat3(ModelViewMat) * Position;\n" +
                    "    vec4 vp = ModelViewMat * vec4(Position, 1.0);\n" +
                    "    gl_Position = ProjMat * vp;\n" +
                    "    vViewPos = vp.xyz;\n" +
                    "}\n";

    private static final String FRAG_SRC =
            "#version 150 core\n" +
                    "in vec3 vNormalView;\n" +
                    "in vec3 vViewPos;\n" +
                    "uniform vec3 SunDirView;\n" +
                    "out vec4 fragColor;\n" +
                    "void main() {\n" +
                    "    vec3 vNormal = normalize(vNormalView);\n" +
                    "    vec3 viewDir = normalize(-vViewPos);\n" +
                    "    float NdotV = max(dot(vNormal, viewDir), 0.0);\n" +
                    "    float rim = 1.0 - NdotV;\n" +
                    "    float NdotS = dot(vNormal, SunDirView);\n" +
                    "    float backScat = pow(max(-NdotS, 0.0), 1.5);\n" +
                    "    float fwdScat = pow(max( NdotS, 0.0), 3.0);\n" +
                    "    float atmo = pow(rim, 4.0) * (0.65 + 2.2 * backScat + 0.35 * fwdScat);\n" +
                    "    atmo = clamp(atmo, 0.0, 1.0);\n" +
                    "    vec3 sunColor = vec3(1.00, 0.88, 0.50);\n" +
                    "    vec3 innerColor = vec3(0.95, 0.80, 0.40);\n" +
                    "    vec3 outerColor = vec3(0.60, 0.38, 0.82);\n" +
                    "    vec3 backColor = vec3(0.30, 0.45, 1.00);\n" +
                    "    vec3 color = mix(innerColor, outerColor, pow(rim, 6.0));\n" +
                    "    color = mix(color, backColor, backScat * pow(rim, 2.0) * 0.85);\n" +
                    "    color = mix(color, sunColor, fwdScat * (1.0 - rim) * 0.45);\n" +
                    "    fragColor = vec4(color, atmo * 0.90);\n" +
                    "}\n";





    private static float[] buildCubeVerts(float r) {
        return new float[] {

                -r, -r,  r,  r, -r,  r, -r,  r,  r,  r, -r,  r,  r,  r,  r, -r,  r,  r,
                -r, -r, -r, -r,  r, -r,  r, -r, -r,  r, -r, -r, -r,  r, -r,  r,  r, -r,
                -r,  r, -r, -r,  r,  r,  r,  r, -r,  r,  r, -r, -r,  r,  r,  r,  r,  r,
                -r, -r, -r,  r, -r, -r, -r, -r,  r,  r, -r, -r,  r, -r,  r, -r, -r,  r,
                -r, -r, -r, -r, -r,  r, -r,  r, -r, -r, -r,  r, -r,  r,  r, -r,  r, -r,
                r, -r, -r,  r,  r, -r,  r, -r,  r,  r,  r, -r,  r,  r,  r,  r, -r,  r
        };
    }

    private static void initIfNeeded() {
        if (shaderProgram != 0) return;


        int vert = compileShader(GL20.GL_VERTEX_SHADER, VERT_SRC);
        int frag = compileShader(GL20.GL_FRAGMENT_SHADER, FRAG_SRC);
        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vert);
        GL20.glAttachShader(shaderProgram, frag);
        GL20.glBindAttribLocation(shaderProgram, 0, "Position");
        GL20.glLinkProgram(shaderProgram);

        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("[SaturnAtmosphere] Link error: " + GL20.glGetProgramInfoLog(shaderProgram));
        }

        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        uModelViewLoc = GL20.glGetUniformLocation(shaderProgram, "ModelViewMat");
        uProjLoc = GL20.glGetUniformLocation(shaderProgram, "ProjMat");
        uSunDirViewLoc = GL20.glGetUniformLocation(shaderProgram, "SunDirView");


        float[] verts = buildCubeVerts(ATMO_RADIUS);
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
        GL20.glEnableVertexAttribArray(0);


        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private static int compileShader(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("[SaturnAtmosphere] Compile error:\n" + GL20.glGetShaderInfoLog(id));
        }
        return id;
    }



    public static void render(PoseStack matrices, Vector3f sunDirView) {
        initIfNeeded();

        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrices.last().pose());
        Matrix4f proj = RenderSystem.getProjectionMatrix();


        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevSrcRgb = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int prevDstRgb = GL11.glGetInteger(GL11.GL_BLEND_DST);
        boolean blendEnabled = GL11.glGetBoolean(GL11.GL_BLEND);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullEnabled = GL11.glGetBoolean(GL11.GL_CULL_FACE);


        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL20.glUseProgram(shaderProgram);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer mvBuf = stack.mallocFloat(16);
            modelView.get(mvBuf);
            GL20.glUniformMatrix4fv(uModelViewLoc, false, mvBuf);

            FloatBuffer pBuf = stack.mallocFloat(16);
            proj.get(pBuf);
            GL20.glUniformMatrix4fv(uProjLoc, false, pBuf);

            GL20.glUniform3f(uSunDirViewLoc, sunDirView.x, sunDirView.y, sunDirView.z);
        }

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, CUBE_VERT_COUNT);


        cleanupState(prevProgram, prevVao, prevSrcRgb, prevDstRgb, blendEnabled, depthMask, cullEnabled);
    }



    private static void cleanupState(int prog, int vao, int src, int dst, boolean blend, boolean depth, boolean cull) {
        GL20.glUseProgram(prog);
        GL30.glBindVertexArray(vao);
        GL11.glDepthMask(depth);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        else GL11.glBlendFunc(src, dst);
        if (!cull) GL11.glDisable(GL11.GL_CULL_FACE);
    }
}
