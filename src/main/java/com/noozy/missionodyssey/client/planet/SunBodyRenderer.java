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


public class SunBodyRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/stars/sun.png");


    private static final float R = 1.5f;
    private static final int   VERT_COUNT = 36;

    private static int shaderProgram = 0;
    private static int vaoId = 0;
    private static int vboId = 0;
    private static int uModelViewLoc;
    private static int uProjLoc;


    private static final int STRIDE = 5 * Float.BYTES;




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









    private static float[] buildCubeVerts(float r) {
        return new float[]{

                -r,-r, r,  0.5625f, 0.375f,   r,-r, r,  0.75f, 0.375f,  -r, r, r,  0.5625f, 0.1875f,
                r,-r, r,  0.75f,   0.375f,   r, r, r,  0.75f, 0.1875f, -r, r, r,  0.5625f, 0.1875f,


                r,-r,-r,  0.1875f, 0.375f,  -r,-r,-r,  0.375f, 0.375f,  r, r,-r,  0.1875f, 0.1875f,
                -r,-r,-r,  0.375f,  0.375f,  -r, r,-r,  0.375f, 0.1875f,  r, r,-r,  0.1875f, 0.1875f,


                -r, r, r,  0.1875f, 0.1875f,  r, r, r,  0.375f, 0.1875f, -r, r,-r,  0.1875f, 0.0f,
                r, r, r,  0.375f,  0.1875f,  r, r,-r,  0.375f, 0.0f,    -r, r,-r,  0.1875f, 0.0f,


                -r,-r,-r,  0.375f,  0.0f,     r,-r,-r,  0.5625f, 0.0f,   -r,-r, r,  0.375f, 0.1875f,
                r,-r,-r,  0.5625f, 0.0f,     r,-r, r,  0.5625f, 0.1875f,-r,-r, r,  0.375f, 0.1875f,


                r,-r, r,  0.0f,    0.375f,   r,-r,-r,  0.1875f, 0.375f,  r, r, r,  0.0f,    0.1875f,
                r,-r,-r,  0.1875f, 0.375f,   r, r,-r,  0.1875f, 0.1875f, r, r, r,  0.0f,    0.1875f,


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

        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1i(GL20.glGetUniformLocation(shaderProgram, "Sampler0"), 0);
        GL20.glUseProgram(0);

        float[] verts = buildCubeVerts(R);
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, STRIDE, 0L);
        GL20.glEnableVertexAttribArray(0);

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


    public static void render(PoseStack matrices) {
        initIfNeeded();


        int textureId = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TEXTURE)
                .getId();

        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrices.last().pose());
        Matrix4f proj      = RenderSystem.getProjectionMatrix();


        int     prevProgram  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int     prevVao      = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int     prevTex      = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean depthMask    = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blendEnabled = GL11.glGetBoolean(GL11.GL_BLEND);
        boolean cullEnabled  = GL11.glGetBoolean(GL11.GL_CULL_FACE);


        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL20.glUseProgram(shaderProgram);


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


        GL20.glUseProgram(prevProgram);
        GL30.glBindVertexArray(prevVao);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        GL11.glDepthMask(depthMask);
        if (blendEnabled) GL11.glEnable(GL11.GL_BLEND);
        if (!cullEnabled)  GL11.glDisable(GL11.GL_CULL_FACE);
    }
}
