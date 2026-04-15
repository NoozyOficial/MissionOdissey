package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.registry.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Atmosfera do Sol — Corona Solar.
 *
 * Diferente dos outros planetas, o Sol:
 *  - É SUA PRÓPRIA fonte de luz, então não há "sunDir" externo.
 *  - A corona emite luz em todas as direções (isotrópica).
 *  - Camadas: cromosfera interna (amarelo/laranja quente),
 *             corona externa (branco-azulado com fade), e
 *             um halo aditivo brilhante.
 *
 * O shader usa APENAS o ângulo do normal em relação ao observador (rim lighting
 * puro), produzindo o halo característico da corona solar real.
 *
 * O uTime é passado para animar a "pulsação" da corona.
 */
public class SunAtmosphereRenderer {

    // A corona solar se estende muito além da fotosfera (~1–2× o raio)
    // Usamos 1.35 para um halo vistoso mas contido.
    private static final float CORONA_RADIUS = (float) (ModDimensions.SUN_MODEL_RADIUS * 1.45);
    private static final int   CUBE_VERT_COUNT = 36;

    private static int shaderProgram = 0;
    private static int vaoId = 0;
    private static int vboId = 0;
    private static int uModelViewLoc;
    private static int uProjLoc;
    private static int uTimeLoc;

    // ---------------------------------------------------------
    //  Vertex Shader
    // ---------------------------------------------------------
    private static final String VERT_SRC =
            "#version 150 core\n" +
            "in vec3 Position;\n" +
            "uniform mat4 ModelViewMat;\n" +
            "uniform mat4 ProjMat;\n" +
            "out vec3 vNormalView;\n" +
            "out vec3 vViewPos;\n" +
            "void main() {\n" +
            "    vNormalView = mat3(ModelViewMat) * normalize(Position);\n" +
            "    vec4 vp = ModelViewMat * vec4(Position, 1.0);\n" +
            "    gl_Position = ProjMat * vp;\n" +
            "    vViewPos = vp.xyz;\n" +
            "}\n";

    // ---------------------------------------------------------
    //  Fragment Shader — Corona Solar
    //
    //  A atmosfera do Sol real tem:
    //   • Fotosfera: amarelo-branco (~5778 K)
    //   • Cromosfera: vermelho-rosacea (Ha line)
    //   • Corona: branco-azulado extremamente tênue mas muito quente
    //
    //  Aqui renderizamos o halo aditivo que vai POR CIMA do modelo GeckoLib,
    //  exibindo: borda laranja-dourada interna → branco quente → fade para
    //  corona azulada → transparência no espaço.
    //
    //  A pulsação solar (uTime) é adicionada como variação de opacidade
    //  para imitar a natureza dinâmica do campo magnético solar.
    // ---------------------------------------------------------
    // ---------------------------------------------------------
    //  Fragment Shader — Corona Solar (Ajustado para tons quentes)
    // ---------------------------------------------------------
    private static final String FRAG_SRC =
            "#version 150 core\n" +
                    "in vec3 vNormalView;\n" +
                    "in vec3 vViewPos;\n" +
                    "uniform float uTime;\n" +
                    "out vec4 fragColor;\n" +
                    "\n" +
                    "// hash simples para variação pseudo-rand por fragmento\n" +
                    "float hash(vec3 p) {\n" +
                    "    p = fract(p * vec3(0.1031, 0.1030, 0.0973));\n" +
                    "    p += dot(p, p.yxz + 33.33);\n" +
                    "    return fract((p.x + p.y) * p.z);\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec3 vNorm = normalize(vNormalView);\n" +
                    "    vec3 viewDir = normalize(-vViewPos);\n" +
                    "\n" +
                    "    // NdotV: 1.0 = centro do disco, 0.0 = borda extrema\n" +
                    "    float NdotV = max(dot(vNorm, viewDir), 0.0);\n" +
                    "    float rim   = 1.0 - NdotV; // 0.0 = centro → 1.0 = borda\n" +
                    "\n" +
                    "    // ── Pulsação da corona ──────────────────────────────────────\n" +
                    "    float pulse = 0.88 + 0.12 * sin(uTime * 0.42 + rim * 8.0);\n" +
                    "    float microFlicker = 0.97 + 0.03 * sin(uTime * 3.1 + vNorm.x * 17.0 + vNorm.y * 11.3);\n" +
                    "\n" +
                    "    // ── Camadas de cor da corona (Nova Paleta Quente) ───────────\n" +
                    "    // innerColor: núcleo da corona — amarelo quase branco e incandescente\n" +
                    "    vec3 innerColor = vec3(1.00, 0.90, 0.40);\n" +
                    "    // midColor: corona interna — laranja forte e vibrante\n" +
                    "    vec3 midColor   = vec3(1.00, 0.50, 0.05);\n" +
                    "    // outerColor: corona externa — vermelho escuro/dourado que esvanece no espaço\n" +
                    "    vec3 outerColor = vec3(0.80, 0.15, 0.00);\n" +
                    "\n" +
                    "    // Gradiente: centro (inner) → borda (mid) → far rim (outer)\n" +
                    "    vec3 color = mix(innerColor, midColor, smoothstep(0.0, 0.55, rim));\n" +
                    "    color      = mix(color, outerColor, smoothstep(0.55, 1.0, rim));\n" +
                    "\n" +
                    "    // ── Opacidade ───────────────────────────────────────────────\n" +
                    "    float coreBright  = pow(max(1.0 - rim / 0.35, 0.0), 1.8);\n" +
                    "    float coronaGlow  = pow(max(rim, 0.0), 2.5) * (1.0 - smoothstep(0.55, 1.0, rim));\n" +
                    "    float outerHaze   = pow(max(rim - 0.5, 0.0) / 0.5, 1.5) * (1.0 - smoothstep(0.85, 1.0, rim));\n" +
                    "\n" +
                    "    float alpha = (coreBright * 0.90 + coronaGlow * 0.70 + outerHaze * 0.30)\n" +
                    "                  * pulse * microFlicker;\n" +
                    "    alpha = clamp(alpha, 0.0, 1.0);\n" +
                    "\n" +
                    "    fragColor = vec4(color, alpha);\n" +
                    "}\n";

    // ---------------------------------------------------------
    private static float[] buildCubeVerts(float r) {
        return new float[] {
                -r,-r, r,  r,-r, r, -r, r, r,  r,-r, r,  r, r, r, -r, r, r,
                -r,-r,-r, -r, r,-r,  r,-r,-r,  r,-r,-r, -r, r,-r,  r, r,-r,
                -r, r,-r, -r, r, r,  r, r,-r,  r, r,-r, -r, r, r,  r, r, r,
                -r,-r,-r,  r,-r,-r, -r,-r, r,  r,-r,-r,  r,-r, r, -r,-r, r,
                -r,-r,-r, -r,-r, r, -r, r,-r, -r,-r, r, -r, r, r, -r, r,-r,
                 r,-r,-r,  r, r,-r,  r,-r, r,  r, r,-r,  r, r, r,  r,-r, r
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
        GL20.glLinkProgram(shaderProgram);

        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("[SunAtmosphere] Link error: " + GL20.glGetProgramInfoLog(shaderProgram));
        }

        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        uModelViewLoc = GL20.glGetUniformLocation(shaderProgram, "ModelViewMat");
        uProjLoc      = GL20.glGetUniformLocation(shaderProgram, "ProjMat");
        uTimeLoc      = GL20.glGetUniformLocation(shaderProgram, "uTime");

        float[] verts = buildCubeVerts(CORONA_RADIUS);
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
            throw new RuntimeException("[SunAtmosphere] Compile error:\n" + GL20.glGetShaderInfoLog(id));
        }
        return id;
    }

    /**
     * @param matrices PoseStack já posicionada/escalada no centro do Sol.
     * @param timeTicks Tempo de jogo em ticks para animação da corona.
     */
    public static void render(PoseStack matrices, float timeTicks) {
        initIfNeeded();

        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrices.last().pose());
        Matrix4f proj      = RenderSystem.getProjectionMatrix();

        int     prevProgram  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int     prevVao      = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int     prevSrcRgb   = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int     prevDstRgb   = GL11.glGetInteger(GL11.GL_BLEND_DST);
        boolean blendEnabled = GL11.glGetBoolean(GL11.GL_BLEND);
        boolean depthMask    = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullEnabled  = GL11.glGetBoolean(GL11.GL_CULL_FACE);

        // Blending ADITIVO: a corona soma brilho na cena (igual a estrelas reais)
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        // Sem culling: renderizar tanto a face interna quanto a externa
        // da casca para o efeito de halo funcionar em todos os ângulos.
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL20.glUseProgram(shaderProgram);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer mvBuf = stack.mallocFloat(16);
            modelView.get(mvBuf);
            GL20.glUniformMatrix4fv(uModelViewLoc, false, mvBuf);

            FloatBuffer pBuf = stack.mallocFloat(16);
            proj.get(pBuf);
            GL20.glUniformMatrix4fv(uProjLoc, false, pBuf);

            // Normaliza o tempo para segundos-ish
            GL20.glUniform1f(uTimeLoc, timeTicks * 0.05f);
        }

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, CUBE_VERT_COUNT);

        // Restaurar estado anterior
        GL20.glUseProgram(prevProgram);
        GL30.glBindVertexArray(prevVao);
        GL11.glDepthMask(depthMask);
        if (!blendEnabled) GL11.glDisable(GL11.GL_BLEND);
        else               GL11.glBlendFunc(prevSrcRgb, prevDstRgb);
        if (cullEnabled)   GL11.glEnable(GL11.GL_CULL_FACE);
    }
}
