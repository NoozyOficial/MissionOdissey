package com.noozy.missionodyssey.client.planet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Renderiza o corpo da fotosfera do Sol em GL puro, FORA da pipeline do
 * GeckoLib e do Veil. Isso garante que nenhuma luz direcional (Veil sun,
 * ambient occlusion, etc.) afete a aparência do Sol.
 *
 * O shader apenas amostra a textura sun.png e a exibe com brilho total
 * (emissivo puro). Blending aditivo opcional faz o Sol "brilhar" mais
 * que qualquer planeta ao lado dele.
 */
public class SunBodyRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/stars/sun.png");

    // Raio do cubo unitário — o mesmo EARTH_MODEL_RADIUS (1.5) escalado no WorldRenderer
    private static final float R = 1.5f;
    private static final int   VERT_COUNT = 36;

    private static int shaderProgram = 0;
    private static int vaoId = 0;
    private static int vboId = 0;
    private static int uModelViewLoc;
    private static int uProjLoc;

    // Cada vértice: Position (3 floats) + UV (2 floats)
    private static final int STRIDE = 5 * Float.BYTES;

    // ---------------------------------------------------------
    //  Vertex shader — transforma posição, passa UV
    // ---------------------------------------------------------
    private static final String VERT_SRC =
            "#version 150 core\n" +
            "in vec3 Position;\n" +
            "in vec2 UV;\n" +
            "uniform mat4 ModelViewMat;\n" +
            "uniform mat4 ProjMat;\n" +
            "out vec2 vUV;\n" +
            "void main() {\n" +
            "    vUV = UV;\n" +
            "    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
            "}\n";

    // ---------------------------------------------------------
    //  Fragment shader — amostragem pura, sem lighting
    //  O Sol é emissivo: a textura sai exatamente como definida.
    //  Multiplicamos por um fator de brilho (>1.0) para que
    //  o tone-mapping da cena o deixe mais luminoso que os planetas.
    // ---------------------------------------------------------
    private static final String FRAG_SRC =
            "#version 150 core\n" +
            "in vec2 vUV;\n" +
            "uniform sampler2D Sampler0;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture(Sampler0, vUV);\n" +
            "    // Amplificar levemente para que fique mais brilhante que qualquer planeta\n" +
            "    fragColor = vec4(texColor.rgb * 2.2, texColor.a);\n" +
            "}\n";

    // ---------------------------------------------------------
    //  Geometria: cubo com UVs de face (6 faces, 2 triângulos cada)
    //  Cada face usa toda a textura (0→1 em U e V).
    //  Formato: x, y, z, u, v
    // ---------------------------------------------------------
// ---------------------------------------------------------
    //  Geometria mapeada com UVs exatos do Blockbench (128x128)
    // ---------------------------------------------------------
    private static float[] buildCubeVerts(float r) {
        return new float[]{
                // +Z (frente / South no Blockbench) -> uv: [72, 24]
                -r,-r, r,  0.5625f, 0.375f,   r,-r, r,  0.75f, 0.375f,  -r, r, r,  0.5625f, 0.1875f,
                r,-r, r,  0.75f,   0.375f,   r, r, r,  0.75f, 0.1875f, -r, r, r,  0.5625f, 0.1875f,

                // -Z (atrás / North no Blockbench) -> uv: [24, 24]
                r,-r,-r,  0.1875f, 0.375f,  -r,-r,-r,  0.375f, 0.375f,  r, r,-r,  0.1875f, 0.1875f,
                -r,-r,-r,  0.375f,  0.375f,  -r, r,-r,  0.375f, 0.1875f,  r, r,-r,  0.1875f, 0.1875f,

                // +Y (topo / Up no Blockbench) -> uv: [24, 0]
                -r, r, r,  0.1875f, 0.1875f,  r, r, r,  0.375f, 0.1875f, -r, r,-r,  0.1875f, 0.0f,
                r, r, r,  0.375f,  0.1875f,  r, r,-r,  0.375f, 0.0f,    -r, r,-r,  0.1875f, 0.0f,

                // -Y (base / Down no Blockbench) -> uv: [48, 24], size: [24, -24] (invertido no V)
                -r,-r,-r,  0.375f,  0.0f,     r,-r,-r,  0.5625f, 0.0f,   -r,-r, r,  0.375f, 0.1875f,
                r,-r,-r,  0.5625f, 0.0f,     r,-r, r,  0.5625f, 0.1875f,-r,-r, r,  0.375f, 0.1875f,

                // +X (direita / East no Blockbench) -> uv: [0, 24]
                r,-r, r,  0.0f,    0.375f,   r,-r,-r,  0.1875f, 0.375f,  r, r, r,  0.0f,    0.1875f,
                r,-r,-r,  0.1875f, 0.375f,   r, r,-r,  0.1875f, 0.1875f, r, r, r,  0.0f,    0.1875f,

                // -X (esquerda / West no Blockbench) -> uv: [48, 24]
                -r,-r,-r,  0.375f,  0.375f,  -r,-r, r,  0.5625f, 0.375f, -r, r,-r,  0.375f,  0.1875f,
                -r,-r, r,  0.5625f, 0.375f,  -r, r, r,  0.5625f, 0.1875f,-r, r,-r,  0.375f,  0.1875f,
        };
    }

    private static void initIfNeeded() {
        if (shaderProgram != 0) return;

        int vert = compileShader(GL20.GL_VERTEX_SHADER,   VERT_SRC);
        int frag = compileShader(GL20.GL_FRAGMENT_SHADER, FRAG_SRC);
        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vert);
        GL20.glAttachShader(shaderProgram, frag);
        GL20.glBindAttribLocation(shaderProgram, 0, "Position");
        GL20.glBindAttribLocation(shaderProgram, 1, "UV");
        GL20.glLinkProgram(shaderProgram);

        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("[SunBody] Link error: " + GL20.glGetProgramInfoLog(shaderProgram));
        }
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        uModelViewLoc = GL20.glGetUniformLocation(shaderProgram, "ModelViewMat");
        uProjLoc      = GL20.glGetUniformLocation(shaderProgram, "ProjMat");
        // Sampler0 no slot 0
        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1i(GL20.glGetUniformLocation(shaderProgram, "Sampler0"), 0);
        GL20.glUseProgram(0);

        float[] verts = buildCubeVerts(R);
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);
        // Position (location 0): 3 floats
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, STRIDE, 0L);
        GL20.glEnableVertexAttribArray(0);
        // UV (location 1): 2 floats, offset 12 bytes
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, STRIDE, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private static int compileShader(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("[SunBody] Compile error:\n" + GL20.glGetShaderInfoLog(id));
        }
        return id;
    }

    /**
     * Renderiza a fotosfera do Sol em GL puro — completamente emissiva,
     * sem influência de luz do Veil ou do sistema de iluminação do Minecraft.
     *
     * @param matrices PoseStack já posicionada e escalada no centro do Sol.
     */
    public static void render(PoseStack matrices) {
        initIfNeeded();

        // Resolve o ResourceLocation para o ID de textura OpenGL
        int textureId = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TEXTURE)
                .getId();

        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrices.last().pose());
        Matrix4f proj      = RenderSystem.getProjectionMatrix();

        // Salva estado GL
        int     prevProgram  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int     prevVao      = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int     prevTex      = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean depthMask    = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blendEnabled = GL11.glGetBoolean(GL11.GL_BLEND);
        boolean cullEnabled  = GL11.glGetBoolean(GL11.GL_CULL_FACE);

        // Sem blend opaco — o sol é um disco sólido brilhante
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL20.glUseProgram(shaderProgram);

        // Bind da textura no slot 0
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer mvBuf = stack.mallocFloat(16);
            modelView.get(mvBuf);
            GL20.glUniformMatrix4fv(uModelViewLoc, false, mvBuf);

            FloatBuffer pBuf = stack.mallocFloat(16);
            proj.get(pBuf);
            GL20.glUniformMatrix4fv(uProjLoc, false, pBuf);
        }

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, VERT_COUNT);

        // Restaura estado
        GL20.glUseProgram(prevProgram);
        GL30.glBindVertexArray(prevVao);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        GL11.glDepthMask(depthMask);
        if (blendEnabled) GL11.glEnable(GL11.GL_BLEND);
        if (!cullEnabled)  GL11.glDisable(GL11.GL_CULL_FACE);
    }
}
