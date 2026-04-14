package com.noozy.missionodyssey.client.planet;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class SpaceSkyboxRenderer {

    private static final float SKY_RADIUS = 100.0f;
    private static final int   CUBE_VERT_COUNT = 36;

    private static int shaderProgram = 0;
    private static int vaoId         = 0;
    private static int vboId         = 0;

    private static int uCameraRotLoc;
    private static int uProjLoc;
    private static int uTimeLoc;

    private static final String VERT_SRC =
            "#version 150 core\n" +
                    "in vec3 Position;\n" +
                    "uniform mat3 CameraRot;\n" +
                    "uniform mat4 ProjMat;\n" +
                    "out vec3 vDir;\n" +
                    "void main() {\n" +
                    "    vDir = normalize(Position);\n" +
                    "    vec4 pos = ProjMat * vec4(CameraRot * Position, 1.0);\n" +
                    "    gl_Position = pos.xyww;\n" +
                    "}\n";

    private static final String FRAG_SRC =
            "#version 150 core\n" +
                    "in  vec3 vDir;\n" +
                    "uniform float uTime;\n" +
                    "out vec4 fragColor;\n" +
                    "\n" +
                    "// ── Hash functions ───────────────────────────────────────────────\n" +
                    "// IQ-style hash: boa distribuição, sem padrões visíveis\n" +
                    "float hash(vec3 p) {\n" +
                    "    p = fract(p * vec3(0.1031, 0.1030, 0.0973));\n" +
                    "    p += dot(p, p.yxz + 33.33);\n" +
                    "    return fract((p.x + p.y) * p.z);\n" +
                    "}\n" +
                    "// Segundo hash para separar brilho de cor (mesma célula, seed diferente)\n" +
                    "float hash2(vec3 p) {\n" +
                    "    p = fract(p * vec3(0.2031, 0.2030, 0.1973) + vec3(0.5));\n" +
                    "    p += dot(p, p.zyx + 31.41);\n" +
                    "    return fract((p.x + p.z) * p.y);\n" +
                    "}\n" +
                    "\n" +
                    "// ── Cor espectral ─────────────────────────────────────────────────\n" +
                    "// Baseado nas classes espectrais reais (O→B→A→F→G→K→M)\n" +
                    "// Distribuição: maioria K/M (laranjas/vermelhas), poucas O/B (azuis)\n" +
                    "vec3 starColor(float t) {\n" +
                    "    if (t < 0.04) return vec3(0.60, 0.72, 1.00);  // O — azul intenso\n" +
                    "    if (t < 0.12) return vec3(0.82, 0.90, 1.00);  // B — azul-branco\n" +
                    "    if (t < 0.28) return vec3(0.98, 0.99, 1.00);  // A — branco puro\n" +
                    "    if (t < 0.50) return vec3(1.00, 0.97, 0.75);  // F — branco-amarelo\n" +
                    "    if (t < 0.70) return vec3(1.00, 0.88, 0.48);  // G — amarelo (tipo Sol)\n" +
                    "    if (t < 0.87) return vec3(1.00, 0.70, 0.35);  // K — laranja\n" +
                    "    return             vec3(1.00, 0.48, 0.28);    // M — laranja-vermelho\n" +
                    "}\n" +
                    "\n" +
                    "// ── Via Láctea ────────────────────────────────────────────────────\n" +
                    "// Plano galáctico: band ao redor de um eixo ligeiramente inclinado.\n" +
                    "// Retorna [0, 1]: 1 = centro da banda, 0 = polos galácticos.\n" +
                    "float milkyWay(vec3 d) {\n" +
                    "    vec3 galAxis = normalize(vec3(0.0, 0.55, 0.835));\n" +
                    "    float h = abs(dot(d, galAxis));\n" +
                    "    return smoothstep(0.65, 0.0, h) * 1.0;\n" +
                    "}\n" +
                    "\n" +
                    "// ── Célula esférica estável ───────────────────────────────────────\n" +
                    "// Converte direção para coordenadas esféricas e discretiza em células.\n" +
                    "// floor() garante hash idêntico para qualquer pixel na mesma célula angular,\n" +
                    "// independente de FOV, perspectiva ou interpolação do cubo.\n" +
                    "vec2 sphereCell(vec3 dir, float scale) {\n" +
                    "    float theta = atan(dir.z, dir.x);\n" +
                    "    float phi   = asin(clamp(dir.y, -1.0, 1.0));\n" +
                    "    return floor(vec2(theta, phi) * scale);\n" +
                    "}\n" +
                    "\n" +
                    "// ── Campo de estrelas ─────────────────────────────────────────────\n" +
                    "// Hash calculado sobre célula esférica discreta — estrelas fixas no céu.\n" +
                    "vec3 starLayer(vec3 dir, float scale, float threshold, float maxBright, vec3 colorOff) {\n" +
                    "    vec2 cell   = sphereCell(dir, scale);\n" +
                    "    vec3 cellP  = vec3(cell.x, cell.y, 0.0);\n" +
                    "    float s     = hash(cellP);\n" +
                    "    float above = s - threshold;\n" +
                    "    if (above <= 0.0) return vec3(0.0);\n" +
                    "    float bright    = (above / (1.0 - threshold)) * maxBright;\n" +
                    "    float colorSeed = hash2(cellP + colorOff);\n" +
                    "    return starColor(colorSeed) * bright;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec3 dir = normalize(vDir);\n" +
                    "\n" +
                    "    // ── Via Láctea ─────────────────────────────────────────────────\n" +
                    "    float mw = milkyWay(dir);\n" +
                    "\n" +
                    "    // Fundo: vácuo quase negro com leve brilho azul-roxo na banda galáctica\n" +
                    "    vec3 col = vec3(0.000, 0.000, 0.003);\n" +
                    "    col += vec3(0.010, 0.014, 0.030) * mw * mw;     // nebulosidade difusa\n" +
                    "    col += vec3(0.005, 0.003, 0.012) * mw;           // glow violeta sutil\n" +
                    "\n" +
                    "    // ── Estrelas: camada 1 — fundo denso e tênue ─────────────────\n" +
                    "    col += starLayer(dir, 420.0, 0.9972, 0.40, vec3(1.7, 0.3, 2.5));\n" +
                    "\n" +
                    "    // Camada extra apenas na Via Láctea: estrelas extras na banda galáctica\n" +
                    "    if (mw > 0.1) {\n" +
                    "        col += starLayer(dir, 420.7, 0.9960, 0.25, vec3(0.9, 2.1, 0.6)) * mw;\n" +
                    "        col += starLayer(dir, 380.0, 0.9968, 0.20, vec3(3.1, 1.4, 0.8)) * mw;\n" +
                    "    }\n" +
                    "\n" +
                    "    // ── Estrelas: camada 2 — médias, mais espaçadas ───────────────\n" +
                    "    col += starLayer(dir, 210.0, 0.9986, 0.90, vec3(4.1, 0.7, 1.9));\n" +
                    "\n" +
                    "    // ── Estrelas: camada 3 — brilhantes com cintilação ───────────\n" +
                    "    vec2 cell3  = sphereCell(dir, 95.0);\n" +
                    "    vec3 cellP3 = vec3(cell3.x + 2.5, cell3.y + 1.1, 3.7);\n" +
                    "    float s3    = hash(cellP3);\n" +
                    "    float above3 = s3 - 0.9992;\n" +
                    "    if (above3 > 0.0) {\n" +
                    "        float twinkle = 0.82 + 0.18 * sin(uTime * 2.1 + s3 * 75.0);\n" +
                    "        float bright  = (above3 / 0.0008) * 3.5 * twinkle;\n" +
                    "        float cSeed   = hash2(cellP3 + vec3(1.3, 3.5, 0.7));\n" +
                    "        col += starColor(cSeed) * bright;\n" +
                    "    }\n" +
                    "\n" +
                    "    // ── Tone mapping Reinhard ─────────────────────────────────────\n" +
                    "    // Evita que estrelas muito brilhantes saturem brancos duros\n" +
                    "    col = col / (col + vec3(0.18));\n" +
                    "\n" +
                    "    fragColor = vec4(col, 1.0);\n" +
                    "}\n";

    private static void initIfNeeded() {
        if (shaderProgram != 0) return;

        int vert = compileShader(GL20.GL_VERTEX_SHADER,   VERT_SRC);
        int frag = compileShader(GL20.GL_FRAGMENT_SHADER, FRAG_SRC);

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vert);
        GL20.glAttachShader(shaderProgram, frag);
        GL20.glBindAttribLocation(shaderProgram, 0, "Position");
        GL20.glLinkProgram(shaderProgram);
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        uCameraRotLoc = GL20.glGetUniformLocation(shaderProgram, "CameraRot");
        uProjLoc      = GL20.glGetUniformLocation(shaderProgram, "ProjMat");
        uTimeLoc      = GL20.glGetUniformLocation(shaderProgram, "uTime");

        float[] verts = buildCubeVerts(SKY_RADIUS);

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
        return id;
    }

    private static float[] buildCubeVerts(float r) {
        return new float[]{
                -r, -r,  r,   r, -r,  r,  -r,  r,  r,   r, -r,  r,   r,  r,  r,  -r,  r,  r,
                -r, -r, -r,  -r,  r, -r,   r, -r, -r,   r, -r, -r,  -r,  r, -r,   r,  r, -r,
                -r,  r, -r,  -r,  r,  r,   r,  r, -r,   r,  r, -r,  -r,  r,  r,   r,  r,  r,
                -r, -r, -r,   r, -r, -r,  -r, -r,  r,   r, -r, -r,   r, -r,  r,  -r, -r,  r,
                -r, -r, -r,  -r, -r,  r,  -r,  r, -r,  -r, -r,  r,  -r,  r,  r,  -r,  r, -r,
                r, -r, -r,   r,  r, -r,   r, -r,  r,   r,  r, -r,   r,  r,  r,   r, -r,  r
        };
    }

    public static void render(float timeTicks, Camera camera) {
        initIfNeeded();

        float pitchRad = (float) Math.toRadians(camera.getXRot());
        float yawRad   = (float) Math.toRadians(camera.getYRot() + 180f);
        Matrix3f rotMat = new Matrix3f().rotateX(pitchRad).rotateY(yawRad);

        Matrix4f proj = RenderSystem.getProjectionMatrix();

        int     prevProgram   = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int     prevVao       = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boolean prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean prevCull      = GL11.glGetBoolean(GL11.GL_CULL_FACE);
        int     prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL20.glUseProgram(shaderProgram);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer rotBuf = stack.mallocFloat(9);
            rotMat.get(rotBuf);
            GL20.glUniformMatrix3fv(uCameraRotLoc, false, rotBuf);

            FloatBuffer projBuf = stack.mallocFloat(16);
            proj.get(projBuf);
            GL20.glUniformMatrix4fv(uProjLoc, false, projBuf);

            GL20.glUniform1f(uTimeLoc, timeTicks * 0.01f);
        }

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, CUBE_VERT_COUNT);

        // Restaura o VAO que estava antes — não deixar em 0, pois o MC usa
        // OpenGL core profile onde draw calls sem VAO ativo geram GL_INVALID_OPERATION.
        GL30.glBindVertexArray(prevVao);

        GL20.glUseProgram(prevProgram);
        GL11.glDepthMask(prevDepthMask);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(prevDepthFunc);
        if (prevCull) GL11.glEnable(GL11.GL_CULL_FACE);
    }
}