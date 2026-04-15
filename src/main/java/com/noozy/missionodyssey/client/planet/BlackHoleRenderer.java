package com.noozy.missionodyssey.client.planet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.noozy.missionodyssey.registry.ModDimensions;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Renderizador realista de Buraco Negro em OpenGL puro.
 *
 * Três passes:
 *
 *  1. EVENT HORIZON — esfera opaca e absolutamente negra.
 *     Usa um cubo unit-sphere inscrito com depth-write ativo.
 *     O fragment shader descarta fragmentos que estão fora da esfera
 *     (via SDF na esfera unitária), garantindo silhueta circular perfeita.
 *
 *  2. GRAVITATIONAL LENSING HALO — anel de distorção óptica ao redor
 *     do event horizon. O shader simula lensing via banding concêntrico
 *     de alta intensidade. Blending aditivo, sem depth write.
 *
 *  3. ACCRETION DISK — disco de plasma superaquecido em anel.
 *     Gradiente de temperatura: interior branco-azulado (>20 000K) →
 *     amarelo-laranja (8 000K) → vermelho-escuro (2 500K).
 *     Animado com turbulência pseudo-noise em tempo real.
 *     Doppler shift: lado que avança em direção ao observador é mais azul/brilhante.
 */
public class BlackHoleRenderer {

    // Raio do cubo unitário: mesmo padrão dos outros planetas
    private static final float R  = (float) ModDimensions.BLACK_HOLE_MODEL_RADIUS;

    // O horizonte ocupa ~65% do cubo (para que o lensing halo tenha espaço)
    private static final float HORIZON_R = R * 0.65f;

    // O halo de lensing ocupa R inteiro (cubo)
    private static final float LENS_R    = R;

    // Disco de acreção: anel plano (Y=0) de raio interno→externo escalado no world renderer
    // Os vértices do disco são gerados em UV space [0..1]
    private static final int   DISK_SEGMENTS = 128;

    private static final int CUBE_VERT_COUNT = 36;

    // ── GL state ───────────────────────────────────────────────────────────────

    // Pass 1: Event Horizon
    private static int horizonProg = 0;
    private static int horizonVao  = 0;
    private static int horizonVbo  = 0;
    private static int hMV, hProj;

    // Pass 2: Lensing Halo
    private static int lensProg = 0;
    private static int lensVao  = 0;
    private static int lensVbo  = 0;
    private static int lMV, lProj, lTime;

    // Pass 3: Accretion Disk
    private static int diskProg = 0;
    private static int diskVao  = 0;
    private static int diskVbo  = 0;
    private static int dMV, dProj, dTime, dViewRight, dViewUp;

    // ── Shader sources ─────────────────────────────────────────────────────────

    // ---- PASS 1: EVENT HORIZON ----
    // Vértice simples, projeta o cubo
    private static final String HORIZON_VERT =
        "#version 150 core\n" +
        "in vec3 Position;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "out vec3 vLocal;\n" +
        "void main() {\n" +
        "    vLocal = Position;\n" +
        "    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "}\n";

    // Fragmento: SDF esfera — descarta o que está fora, pinta preto absoluto
    private static final String HORIZON_FRAG =
        "#version 150 core\n" +
        "in vec3 vLocal;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    // SDF: descarta fragmentos fora da esfera de raio HORIZON_R\n" +
        "    if (length(vLocal) > " + HORIZON_R + ") discard;\n" +
        "    // O event horizon é literalmente preto puro — zero emissão\n" +
        "    fragColor = vec4(0.0, 0.0, 0.0, 1.0);\n" +
        "}\n";

    // ---- PASS 2: GRAVITATIONAL LENSING HALO ----
    private static final String LENS_VERT =
        "#version 150 core\n" +
        "in vec3 Position;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "out vec3 vLocal;\n" +
        "out vec3 vViewPos;\n" +
        "void main() {\n" +
        "    vLocal = Position;\n" +
        "    vec4 vp = ModelViewMat * vec4(Position, 1.0);\n" +
        "    vViewPos = vp.xyz;\n" +
        "    gl_Position = ProjMat * vp;\n" +
        "}\n";

    // O lensing halo simula a distorção de Einstein ring e photon sphere:
    //  - Anel mais brilhante justo além do event horizon (photon sphere ~1.5× Schwarzschild)
    //  - Fade rápido para fora
    //  - Animação pulsante fraca
    private static final String LENS_FRAG =
        "#version 150 core\n" +
        "in vec3 vLocal;\n" +
        "in vec3 vViewPos;\n" +
        "uniform float uTime;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    float r = length(vLocal);\n" +
        "    float maxR = " + LENS_R + ";\n" +
        "    float minR = " + HORIZON_R + ";\n" +
        "\n" +
        "    // Dentro do event horizon: sem lensing visível\n" +
        "    if (r < minR) discard;\n" +
        "    // Fora do cubo: descarta\n" +
        "    if (r > maxR) discard;\n" +
        "\n" +
        "    // t=0 na borda do horizonte, t=1 na borda do cubo\n" +
        "    float t = (r - minR) / (maxR - minR);\n" +
        "\n" +
        "    // Photon sphere ring: pico de brilho em t~0.07 (logo além do horizonte)\n" +
        "    // Simulação da photon sphere de Schwarzschild (r=1.5*r_s)\n" +
        "    float photonRing = exp(-pow((t - 0.07) / 0.04, 2.0)) * 3.5;\n" +
        "\n" +
        "    // Einstein ring secundário, mais fraco, em t~0.18\n" +
        "    float einsteinRing = exp(-pow((t - 0.18) / 0.06, 2.0)) * 1.2;\n" +
        "\n" +
        "    // Halo de lensing difuso\n" +
        "    float diffuseHalo = pow(1.0 - t, 4.5) * 0.6;\n" +
        "\n" +
        "    // Animação: pulsação subtil do lensing\n" +
        "    float pulse = 1.0 + 0.04 * sin(uTime * 0.7 + t * 12.0);\n" +
        "\n" +
        "    float intensity = (photonRing + einsteinRing + diffuseHalo) * pulse;\n" +
        "    intensity = clamp(intensity, 0.0, 1.0);\n" +
        "\n" +
        "    // Cor: branco-azulado (luz real distorcida — nenhuma cor própria, apenas luz refratada)\n" +
        "    // Gradiente do innerring quente para externo frio\n" +
        "    vec3 innerCol = vec3(1.0, 0.95, 0.85);    // quase branco no photon ring\n" +
        "    vec3 outerCol = vec3(0.35, 0.55, 1.0);    // azulado no halo externo\n" +
        "    vec3 color = mix(innerCol, outerCol, smoothstep(0.0, 0.4, t));\n" +
        "\n" +
        "    fragColor = vec4(color, intensity);\n" +
        "}\n";

    // ---- PASS 3: ACCRETION DISK ----
    // Geometria: tristrip anel plano (Y=0) de segmentos, dois vértices por anel (inner/outer)
    // Atributo: Position(3) + UV(2) onde U=ângulo 0-1, V=0 inner / 1 outer

    private static final String DISK_VERT =
        "#version 150 core\n" +
        "in vec3 Position;\n" +
        "in vec2 UV;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "out vec2 vUV;\n" +
        "out vec3 vViewPos;\n" +
        "out vec3 vWorldPos;\n" +
        "void main() {\n" +
        "    vUV = UV;\n" +
        "    vWorldPos = Position;\n" +
        "    vec4 vp = ModelViewMat * vec4(Position, 1.0);\n" +
        "    vViewPos = vp.xyz;\n" +
        "    gl_Position = ProjMat * vp;\n" +
        "}\n";

    // Disco de acreção realista:
    //  Física: plasma em órbita kepleriana aquece por fricção viscosa.
    //  Interior é o mais quente (azul-branco), exterior mais frio (laranja-vermelho).
    //  Efeito Doppler: o lado que se move em direção ao observador é blue-shifted (mais azul/brilhante).
    //  Turbulência: noise temporal em aneis.
    private static final String DISK_FRAG =
        "#version 150 core\n" +
        "in vec2 vUV;\n" +
        "in vec3 vViewPos;\n" +
        "in vec3 vWorldPos;\n" +
        "uniform float uTime;\n" +
        "uniform vec3 uViewRight;\n" +   // right vector da câmera em world space
        "uniform vec3 uViewUp;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "// hash/noise simples para turbulência\n" +
        "float hash21(vec2 p) {\n" +
        "    p = fract(p * vec2(0.1031, 0.1030));\n" +
        "    p += dot(p, p.yx + 33.33);\n" +
        "    return fract((p.x + p.y) * p.x);\n" +
        "}\n" +
        "\n" +
        "float noise2(vec2 p) {\n" +
        "    vec2 i = floor(p);\n" +
        "    vec2 f = fract(p);\n" +
        "    f = f * f * (3.0 - 2.0 * f);\n" +
        "    return mix(mix(hash21(i), hash21(i + vec2(1,0)), f.x),\n" +
        "               mix(hash21(i + vec2(0,1)), hash21(i + vec2(1,1)), f.x), f.y);\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    float angle  = vUV.x;     // 0..1 ao redor do disco\n" +
        "    float radial = vUV.y;     // 0 = borda interna, 1 = borda externa\n" +
        "\n" +
        "    // ── Turbulência kepleriana ───────────────────────────────────────\n" +
        "    // Velocidade orbital v ~ 1/sqrt(r): interior orbita mais rápido\n" +
        "    float orbitalSpeed = 0.12 / (0.1 + radial);\n" +
        "    float theta = angle * 6.283185 - uTime * orbitalSpeed;\n" +
        "\n" +
        "    // Noise em coordenadas polares animadas\n" +
        "    vec2 noiseCoord = vec2(theta * 3.0 + radial * 8.0, radial * 5.0 + uTime * 0.08);\n" +
        "    float turb = noise2(noiseCoord * 4.0) * 0.5\n" +
        "               + noise2(noiseCoord * 8.0) * 0.25\n" +
        "               + noise2(noiseCoord * 16.0) * 0.125;\n" +
        "    turb = turb / 0.875; // normaliza\n" +
        "\n" +
        "    // ── Gradiente de temperatura radial ─────────────────────────────\n" +
        "    // Interior: branco-azulado puro (~25000K)\n" +
        "    vec3 t0 = vec3(0.72, 0.85, 1.00);  // inner edge: azul-branco quente\n" +
        "    // Mid-inner: branco-amarelado (~10000K)\n" +
        "    vec3 t1 = vec3(1.00, 0.96, 0.80);\n" +
        "    // Mid: amarelo-laranja (~6000K)\n" +
        "    vec3 t2 = vec3(1.00, 0.68, 0.20);\n" +
        "    // Outer: laranja-vermelho (~3000K)\n" +
        "    vec3 t3 = vec3(0.80, 0.20, 0.04);\n" +
        "    // Borda externa: vermelho escuro quase apagado\n" +
        "    vec3 t4 = vec3(0.30, 0.04, 0.01);\n" +
        "\n" +
        "    vec3 diskColor;\n" +
        "    if (radial < 0.25)       diskColor = mix(t0, t1, radial / 0.25);\n" +
        "    else if (radial < 0.5)   diskColor = mix(t1, t2, (radial - 0.25) / 0.25);\n" +
        "    else if (radial < 0.75)  diskColor = mix(t2, t3, (radial - 0.5)  / 0.25);\n" +
        "    else                      diskColor = mix(t3, t4, (radial - 0.75) / 0.25);\n" +
        "\n" +
        "    // ── Efeito Doppler ───────────────────────────────────────────────\n" +
        "    // v_radial = dot(velocidade orbital, direção para observador)\n" +
        "    // velocidade orbital é tangente ao anel => perpendicular à posição radial\n" +
        "    float ang = angle * 6.283185;\n" +
        "    vec2 tangent2D = vec2(-sin(ang), cos(ang)); // tangente do anel em XZ\n" +
        "    // Componente da tangente que vai em direção ao observador (projeção em XZ)\n" +
        "    vec3 orbitTangent = normalize(vec3(tangent2D.x, 0.0, tangent2D.y));\n" +
        "    // View direction approximation: normalize(-vViewPos)\n" +
        "    vec3 viewDir = normalize(-vViewPos);\n" +
        "    float doppler = dot(orbitTangent, viewDir);\n" +
        "    // doppler>0: side approaching = blue shifted (brighter, bluer)\n" +
        "    // doppler<0: side receding   = red shifted (dimmer, redder)\n" +
        "    float blueBoost = max(doppler, 0.0) * 0.55;\n" +
        "    float redDim    = max(-doppler, 0.0) * 0.40;\n" +
        "    diskColor.r -= redDim * 0.3;\n" +
        "    diskColor.b += blueBoost * 0.5;\n" +
        "    diskColor = clamp(diskColor + blueBoost * 0.15, 0.0, 3.0);\n" +
        "\n" +
        "    // ── Opacity (disk profile) ───────────────────────────────────────\n" +
        "    // O disco tem opacidade máxima no mid-ring, fade nas bordas\n" +
        "    float edgeFade = smoothstep(0.0, 0.12, radial) * smoothstep(1.0, 0.82, radial);\n" +
        "    float brightness = (0.6 + 0.4 * turb) * edgeFade;\n" +
        "    // Brilho muito alto no interior (corona)\n" +
        "    float innerGlow = (1.0 - radial) * (1.0 - radial) * 2.2;\n" +
        "    brightness = brightness * (1.0 + innerGlow);\n" +
        "\n" +
        "    // Aplica Doppler na opacidade também\n" +
        "    brightness *= (1.0 + blueBoost * 0.6) * (1.0 - redDim * 0.5);\n" +
        "\n" +
        "    // ── Streaks radiais (hot spots) ──────────────────────────────────\n" +
        "    // Manchas de plasma superaquecido girando na órbita\n" +
        "    float hotspot1 = exp(-pow(fract(angle * 3.0 - uTime * 0.03) - 0.5, 2.0) / 0.01) * 0.7;\n" +
        "    float hotspot2 = exp(-pow(fract(angle * 5.0 + uTime * 0.05 + 0.3) - 0.5, 2.0) / 0.008) * 0.5;\n" +
        "    brightness += hotspot1 * (1.0 - radial) + hotspot2 * (1.0 - radial * 0.8);\n" +
        "\n" +
        "    brightness = clamp(brightness, 0.0, 1.0);\n" +
        "\n" +
        "    fragColor = vec4(diskColor * brightness, brightness * 0.95);\n" +
        "}\n";

    // ── Geometry builders ──────────────────────────────────────────────────────

    /** Cubo unit-sphere inscribed para o event horizon e o lensing */
    private static float[] buildCubeVerts(float r) {
        return new float[]{
            -r,-r, r,  r,-r, r, -r, r, r,  r,-r, r,  r, r, r, -r, r, r,
            -r,-r,-r, -r, r,-r,  r,-r,-r,  r,-r,-r, -r, r,-r,  r, r,-r,
            -r, r,-r, -r, r, r,  r, r,-r,  r, r,-r, -r, r, r,  r, r, r,
            -r,-r,-r,  r,-r,-r, -r,-r, r,  r,-r,-r,  r,-r, r, -r,-r, r,
            -r,-r,-r, -r,-r, r, -r, r,-r, -r,-r, r, -r, r, r, -r, r,-r,
             r,-r,-r,  r, r,-r,  r,-r, r,  r, r,-r,  r, r, r,  r,-r, r
        };
    }

    /**
     * Gera o disco de acreção como um anel de triângulos em XZ.
     * Cada par de vértices: (inner_vertex, outer_vertex) por segmento.
     * Atributo: Position(3) + UV(2)
     * UV.x = ângulo normalizado (0..1), UV.y = 0 (inner) / 1 (outer)
     *
     * O disco é plano em Y=0 no espaço local do buraco negro.
     * O world renderer inclina o disco ~15° para dar perspectiva.
     */
    private static float[] buildDiskVerts(float innerR, float outerR) {
        int n = DISK_SEGMENTS;
        // n+1 segmentos para fechar o anel, 2 vértices por segmento (inner+outer)
        // Cada quad = 2 triângulos = 6 vértices
        float[] v = new float[n * 6 * 5]; // 5 floats por vértice: xyz + uv
        int idx = 0;
        for (int i = 0; i < n; i++) {
            float a0 = (float) i       / n;
            float a1 = (float)(i + 1)  / n;
            float theta0 = a0 * (float)(2.0 * Math.PI);
            float theta1 = a1 * (float)(2.0 * Math.PI);

            float cx0 = (float) Math.cos(theta0), sz0 = (float) Math.sin(theta0);
            float cx1 = (float) Math.cos(theta1), sz1 = (float) Math.sin(theta1);

            // inner0, inner1, outer0, outer1
            float ix0 = cx0 * innerR, iz0 = sz0 * innerR;
            float ix1 = cx1 * innerR, iz1 = sz1 * innerR;
            float ox0 = cx0 * outerR, oz0 = sz0 * outerR;
            float ox1 = cx1 * outerR, oz1 = sz1 * outerR;

            // Triangle 1: inner0, inner1, outer0
            idx = vert5(v, idx, ix0, 0, iz0, a0, 0f);
            idx = vert5(v, idx, ix1, 0, iz1, a1, 0f);
            idx = vert5(v, idx, ox0, 0, oz0, a0, 1f);
            // Triangle 2: inner1, outer1, outer0
            idx = vert5(v, idx, ix1, 0, iz1, a1, 0f);
            idx = vert5(v, idx, ox1, 0, oz1, a1, 1f);
            idx = vert5(v, idx, ox0, 0, oz0, a0, 1f);
        }
        return v;
    }

    private static int vert5(float[] v, int i, float x, float y, float z, float u, float vv) {
        v[i++] = x; v[i++] = y; v[i++] = z; v[i++] = u; v[i++] = vv;
        return i;
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private static void initIfNeeded() {
        if (horizonProg != 0) return;

        // ---- Pass 1: Event Horizon ----
        {
            int vert = compile(GL20.GL_VERTEX_SHADER,   HORIZON_VERT, "BlackHole-Horizon");
            int frag = compile(GL20.GL_FRAGMENT_SHADER, HORIZON_FRAG, "BlackHole-Horizon");
            horizonProg = link(vert, frag, "BlackHole-Horizon");

            hMV   = GL20.glGetUniformLocation(horizonProg, "ModelViewMat");
            hProj = GL20.glGetUniformLocation(horizonProg, "ProjMat");

            float[] verts = buildCubeVerts(HORIZON_R * 1.01f); // ligeiramente maior que o SDF
            horizonVao = GL30.glGenVertexArrays();
            horizonVbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(horizonVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, horizonVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
            GL20.glEnableVertexAttribArray(0);
            GL30.glBindVertexArray(0);
        }

        // ---- Pass 2: Lensing Halo ----
        {
            int vert = compile(GL20.GL_VERTEX_SHADER,   LENS_VERT, "BlackHole-Lens");
            int frag = compile(GL20.GL_FRAGMENT_SHADER, LENS_FRAG, "BlackHole-Lens");
            lensProg = link(vert, frag, "BlackHole-Lens");

            lMV   = GL20.glGetUniformLocation(lensProg, "ModelViewMat");
            lProj = GL20.glGetUniformLocation(lensProg, "ProjMat");
            lTime = GL20.glGetUniformLocation(lensProg, "uTime");

            float[] verts = buildCubeVerts(LENS_R);
            lensVao = GL30.glGenVertexArrays();
            lensVbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(lensVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lensVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
            GL20.glEnableVertexAttribArray(0);
            GL30.glBindVertexArray(0);
        }

        // ---- Pass 3: Accretion Disk ----
        {
            int vert = compile(GL20.GL_VERTEX_SHADER,   DISK_VERT, "BlackHole-Disk");
            int frag = compile(GL20.GL_FRAGMENT_SHADER, DISK_FRAG, "BlackHole-Disk");
            diskProg = link(vert, frag, "BlackHole-Disk");

            GL20.glBindAttribLocation(diskProg, 0, "Position");
            GL20.glBindAttribLocation(diskProg, 1, "UV");
            GL20.glLinkProgram(diskProg); // re-link para aplicar attrib bindings

            dMV        = GL20.glGetUniformLocation(diskProg, "ModelViewMat");
            dProj      = GL20.glGetUniformLocation(diskProg, "ProjMat");
            dTime      = GL20.glGetUniformLocation(diskProg, "uTime");
            dViewRight = GL20.glGetUniformLocation(diskProg, "uViewRight");
            dViewUp    = GL20.glGetUniformLocation(diskProg, "uViewUp");

            // inner radius = 30% do cubo, outer = 85%
            float[] verts = buildDiskVerts(R * 0.30f, R * 0.85f);
            diskVao = GL30.glGenVertexArrays();
            diskVbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(diskVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, diskVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);
            int stride = 5 * Float.BYTES;
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
            GL20.glEnableVertexAttribArray(1);
            GL30.glBindVertexArray(0);
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private static int compile(int type, String src, String label) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("[" + label + "] Compile error:\n" + GL20.glGetShaderInfoLog(id));
        }
        return id;
    }

    private static int link(int vert, int frag, String label) {
        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vert);
        GL20.glAttachShader(prog, frag);
        GL20.glBindAttribLocation(prog, 0, "Position");
        GL20.glLinkProgram(prog);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("[" + label + "] Link error: " + GL20.glGetProgramInfoLog(prog));
        }
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);
        return prog;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Renderiza o buraco negro completo.
     *
     * @param matrices  PoseStack já posicionada/escalada no centro do buraco negro.
     * @param timeTicks Tempo de jogo (ticks) para animação.
     */
    public static void render(PoseStack matrices, float timeTicks) {
        initIfNeeded();

        float time = timeTicks * 0.02f; // normaliza para ~segundos

        // ── Salva estado GL ────────────────────────────────────────────────────
        int     prevProg      = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int     prevVao       = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int     prevSrc       = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int     prevDst       = GL11.glGetInteger(GL11.GL_BLEND_DST);
        boolean prevBlend     = GL11.glGetBoolean(GL11.GL_BLEND);
        boolean prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean prevDepthTest = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        boolean prevCull      = GL11.glGetBoolean(GL11.GL_CULL_FACE);

        Matrix4f proj = RenderSystem.getProjectionMatrix();

        // ─────────────────────────────────────────────────────────────────────
        // PASS 1 — EVENT HORIZON (esfera absolutamente negra, depth write ON)
        // Renderiza antes de tudo: estabelece a máscara de profundidade para
        // que o lensing halo e o disco fiquem ATRÁS do event horizon.
        // ─────────────────────────────────────────────────────────────────────
        {
            Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrices.last().pose());

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);

            GL20.glUseProgram(horizonProg);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer mvBuf = stack.mallocFloat(16); mv.get(mvBuf);
                GL20.glUniformMatrix4fv(hMV, false, mvBuf);
                FloatBuffer pBuf  = stack.mallocFloat(16); proj.get(pBuf);
                GL20.glUniformMatrix4fv(hProj, false, pBuf);
            }
            GL30.glBindVertexArray(horizonVao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, CUBE_VERT_COUNT);
        }

        // ─────────────────────────────────────────────────────────────────────
        // PASS 2 — LENSING HALO (blending aditivo, depth write OFF)
        // Renderiza a "coroa" de distorção gravitacional como anel concêntrico.
        // ─────────────────────────────────────────────────────────────────────
        {
            Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrices.last().pose());

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);   // aditivo
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);                   // ver dos dois lados

            GL20.glUseProgram(lensProg);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer mvBuf = stack.mallocFloat(16); mv.get(mvBuf);
                GL20.glUniformMatrix4fv(lMV, false, mvBuf);
                FloatBuffer pBuf  = stack.mallocFloat(16); proj.get(pBuf);
                GL20.glUniformMatrix4fv(lProj, false, pBuf);
                GL20.glUniform1f(lTime, time);
            }
            GL30.glBindVertexArray(lensVao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, CUBE_VERT_COUNT);
        }

        // ─────────────────────────────────────────────────────────────────────
        // PASS 3 — ACCRETION DISK
        // Blending alpha normal, double-sided (sem culling), depth read ON.
        // O disco é renderizado em sua própria pose (inclinar 15° no world renderer).
        // ─────────────────────────────────────────────────────────────────────
        {
            Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrices.last().pose());

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);   // aditivo para brilho
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);

            // View right/up em world space (aproximation via MV matrix inverse)
            // Para o Doppler: extraímos a coluna X e Y da inversa da rotação da câmera
            // Como a MV é ortogonal na rotação, transposta = inversa para a rotação
            float viewRX = mv.m00(), viewRY = mv.m10(), viewRZ = mv.m20();
            float viewUX = mv.m01(), viewUY = mv.m11(), viewUZ = mv.m21();

            GL20.glUseProgram(diskProg);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer mvBuf = stack.mallocFloat(16); mv.get(mvBuf);
                GL20.glUniformMatrix4fv(dMV, false, mvBuf);
                FloatBuffer pBuf  = stack.mallocFloat(16); proj.get(pBuf);
                GL20.glUniformMatrix4fv(dProj, false, pBuf);
                GL20.glUniform1f(dTime, time);
                GL20.glUniform3f(dViewRight, viewRX, viewRY, viewRZ);
                GL20.glUniform3f(dViewUp,    viewUX, viewUY, viewUZ);
            }
            GL30.glBindVertexArray(diskVao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, DISK_SEGMENTS * 6);
        }

        // ── Restaura estado GL ─────────────────────────────────────────────────
        GL20.glUseProgram(prevProg);
        GL30.glBindVertexArray(prevVao);
        GL11.glDepthMask(prevDepthMask);
        if (!prevBlend)     GL11.glDisable(GL11.GL_BLEND);
        else                GL11.glBlendFunc(prevSrc, prevDst);
        if (!prevDepthTest) GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (prevCull)       GL11.glEnable(GL11.GL_CULL_FACE);
        else                GL11.glDisable(GL11.GL_CULL_FACE);
    }
}
