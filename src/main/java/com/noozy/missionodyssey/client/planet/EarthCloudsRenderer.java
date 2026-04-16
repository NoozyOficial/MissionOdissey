package com.noozy.missionodyssey.client.planet;

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

public class EarthCloudsRenderer {




    private static final float minX = -1.5625f;
    private static final float maxX =  1.5625f;
    private static final float minY = -1.5625f;
    private static final float maxY =  1.5625f;
    private static final float minZ = -1.5625f;
    private static final float maxZ =  1.5f;

    private static final int CUBE_VERT_COUNT = 36;


    private static int shaderProgram = 0;
    private static int vaoId = 0;
    private static int vboId = 0;
    private static int uModelViewLoc;
    private static int uProjLoc;
    private static int uSunDirViewLoc;
    private static int uTimeLoc;

    private static final String VERT_SRC =
            "#version 150 core\n" +
            "in vec3 Position;\n" +
            "uniform mat4 ModelViewMat;\n" +
            "uniform mat4 ProjMat;\n" +
            "out vec3 vLocalPos;\n" +
            "out vec3 vNormalView;\n" +
            "void main() {\n" +
            "    vLocalPos = Position;\n" +
            "    vNormalView = normalize(mat3(ModelViewMat) * Position);\n" +
            "    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
            "}\n";

    private static final String FRAG_SRC =
            "#version 150 core\n" +
            "in vec3 vLocalPos;\n" +
            "in vec3 vNormalView;\n" +
            "uniform vec3 SunDirView;\n" +
            "uniform float uTime;\n" +
            "out vec4 fragColor;\n" +
            "\n" +
            "// Simplex 3D Noise simplificado\n" +
            "float hash(vec3 p) {\n" +
            "    p = fract(p * 0.3183099 + 0.1);\n" +
            "    p *= 17.0;\n" +
            "    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));\n" +
            "}\n" +
            "float noise(vec3 x) {\n" +
            "    vec3 i = floor(x);\n" +
            "    vec3 f = fract(x);\n" +
            "    f = f * f * (3.0 - 2.0 * f);\n" +
            "    return mix(mix(mix(hash(i + vec3(0,0,0)), hash(i + vec3(1,0,0)), f.x),\n" +
            "                   mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),\n" +
            "               mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),\n" +
            "                   mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);\n" +
            "}\n" +
            "float fbm(vec3 x) {\n" +
            "    float v = 0.0;\n" +
            "    float a = 0.5;\n" +
            "    vec3 shift = vec3(100.0);\n" +
            "    for (int i = 0; i < 4; ++i) {\n" +
            "        v += a * noise(x);\n" +
            "        x = x * 2.0 + shift;\n" +
            "        a *= 0.5;\n" +
            "    }\n" +
            "    return v;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    // Criar quadrados/pixels gigantes para ficar \"pixelado\"\n" +
            "    float pixels = 24.0; // resolução do grid que cria a textura pixelada (pixels 2x maiores)\n" +
            "    vec3 pixelatedPos = floor(vLocalPos * pixels) / pixels;\n" +
            "    \n" +
            "    // Adiciona tempo para os pixels morpharem/evoluirem e se transladarem\n" +
            "    vec3 noisePos = pixelatedPos * 2.5 + vec3(uTime * 0.025, uTime * -0.005, uTime * 0.015);\n" +
            "    \n" +
            "    float n = fbm(noisePos);\n" +
            "    \n" +
            "    // Filtra bordas para criar bolos de nuvens isolados como na realidade\n" +
            "    n = smoothstep(0.48, 0.65, n);\n" +
            "    \n" +
            "    if (n < 0.01) {\n" +
            "        discard; // pixels transparentes somem, reduz custo\n" +
            "    }\n" +
            "    \n" +
            "    // Adiciona shading de iluminação simples em vez de branco chapado\n" +
            "    vec3 vNormal = normalize(vNormalView);\n" +
            "    float NdotS = max(dot(vNormal, SunDirView), 0.0);\n" +
            "    vec3 cloudColor = mix(vec3(0.7, 0.75, 0.8), vec3(1.0, 1.0, 1.0), NdotS);\n" +
            "    \n" +
            "    // Seta a cor com opacidade da nuvem\n" +
            "    fragColor = vec4(cloudColor, n * 0.9);\n" +
            "}\n";

    private static float[] buildCloudCube() {

        float x1 = minX, x2 = maxX;
        float y1 = minY, y2 = maxY;
        float z1 = minZ, z2 = maxZ;

        return new float[]{

            x1, y1, z2,  x2, y1, z2,  x1, y2, z2,
            x2, y1, z2,  x2, y2, z2,  x1, y2, z2,

            x1, y1, z1,  x1, y2, z1,  x2, y1, z1,
            x2, y1, z1,  x1, y2, z1,  x2, y2, z1,

            x1, y2, z1,  x1, y2, z2,  x2, y2, z1,
            x2, y2, z1,  x1, y2, z2,  x2, y2, z2,

            x1, y1, z1,  x2, y1, z1,  x1, y1, z2,
            x2, y1, z1,  x2, y1, z2,  x1, y1, z2,

            x1, y1, z1,  x1, y1, z2,  x1, y2, z1,
            x1, y1, z2,  x1, y2, z2,  x1, y2, z1,

            x2, y1, z1,  x2, y2, z1,  x2, y1, z2,
            x2, y1, z2,  x2, y2, z1,  x2, y2, z2
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
            throw new RuntimeException("[EarthClouds] Link error: " + GL20.glGetProgramInfoLog(shaderProgram));
        }

        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        uModelViewLoc = GL20.glGetUniformLocation(shaderProgram, "ModelViewMat");
        uProjLoc = GL20.glGetUniformLocation(shaderProgram, "ProjMat");
        uSunDirViewLoc = GL20.glGetUniformLocation(shaderProgram, "SunDirView");
        uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "uTime");

        float[] verts = buildCloudCube();
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
            throw new RuntimeException("[EarthClouds] Compile error:\n" + GL20.glGetShaderInfoLog(id));
        }
        return id;
    }

    public static void render(PoseStack matrices, Vector3f sunDirView, float time) {
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
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
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
            GL20.glUniform1f(uTimeLoc, time);
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
