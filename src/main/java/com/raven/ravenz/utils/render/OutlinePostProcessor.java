package com.raven.ravenz.utils.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.*;

public final class OutlinePostProcessor {
    private static final String SHADER_NAME = "entity_outline";
    private static final float[] QUAD_VERTICES = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };

    private static int vao = -1;
    private static int vbo = -1;
    private static boolean initialized = false;

    private OutlinePostProcessor() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        int shaderProgram = ShaderManager.loadShaderProgram(
                SHADER_NAME,
                "shaders/post/entity_outline.vsh",
                "shaders/post/entity_outline.fsh"
        );
        if (shaderProgram == 0) {
            return;
        }

        vao = GL30.glGenVertexArrays();
        vbo = GL20.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, QUAD_VERTICES, GL20.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        initialized = true;
    }

    public static void process(Framebuffer outlineBuffer, Color color, float width, float intensity) {
        process(outlineBuffer, color, width, intensity, false, 0.2f);
    }

    public static void process(Framebuffer outlineBuffer, Color color, float width, float intensity, boolean showFill,
                               float fillAlpha) {
        if (!initialized) {
            init();
        }
        if (!initialized || outlineBuffer == null) {
            return;
        }

        Integer shaderProgram = ShaderManager.getShaderProgram(SHADER_NAME);
        if (shaderProgram == null || shaderProgram == 0) {
            return;
        }

        int[] prevVao = new int[1];
        int[] prevProgram = new int[1];
        int[] prevFramebuffer = new int[1];
        GL11.glGetIntegerv(GL30.GL_VERTEX_ARRAY_BINDING, prevVao);
        GL11.glGetIntegerv(GL20.GL_CURRENT_PROGRAM, prevProgram);
        GL11.glGetIntegerv(GL30.GL_FRAMEBUFFER_BINDING, prevFramebuffer);

        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int screenWidth = viewport[2];
        int screenHeight = viewport[3];

        try {
            GlStateManager._disableDepthTest();
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(
                    GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE,
                    GL11.GL_ONE_MINUS_SRC_ALPHA
            );

            GL30.glBindVertexArray(vao);
            ShaderManager.useShader(SHADER_NAME);

            int textureId = resolveTextureId(outlineBuffer.getColorAttachment());
            if (textureId == -1) {
                return;
            }

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            ShaderManager.setUniform1i(SHADER_NAME, "DiffuseSampler", 0);
            ShaderManager.setUniform2f(SHADER_NAME, "ScreenSize", screenWidth, screenHeight);
            ShaderManager.setUniform4f(
                    SHADER_NAME,
                    "OutlineColor",
                    color.getRed() / 255.0f,
                    color.getGreen() / 255.0f,
                    color.getBlue() / 255.0f,
                    color.getAlpha() / 255.0f
            );
            ShaderManager.setUniform1f(SHADER_NAME, "OutlineWidth", width);
            ShaderManager.setUniform1f(SHADER_NAME, "Intensity", intensity);
            ShaderManager.setUniform1f(SHADER_NAME, "ShowFill", showFill ? 1.0f : 0.0f);
            ShaderManager.setUniform1f(SHADER_NAME, "FillAlpha", fillAlpha);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        } finally {
            GL30.glBindVertexArray(prevVao[0]);
            GL20.glUseProgram(prevProgram[0]);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer[0]);

            if (depthEnabled) {
                GlStateManager._enableDepthTest();
            } else {
                GlStateManager._disableDepthTest();
            }

            if (!blendEnabled) {
                GlStateManager._disableBlend();
            }
        }
    }

    public static void cleanup() {
        if (vao != -1) {
            GL30.glDeleteVertexArrays(vao);
            vao = -1;
        }
        if (vbo != -1) {
            GL20.glDeleteBuffers(vbo);
            vbo = -1;
        }
        initialized = false;
    }

    private static int resolveTextureId(Object colorAttachment) {
        if (colorAttachment == null) {
            return -1;
        }

        for (String methodName : new String[]{"getGlId", "glId", "getId", "id", "getHandle", "handle"}) {
            try {
                Object value = colorAttachment.getClass().getMethod(methodName).invoke(colorAttachment);
                if (value instanceof Integer id) {
                    return id;
                }
            } catch (Throwable ignored) {
            }
        }

        for (String fieldName : new String[]{"glId", "id", "handle"}) {
            try {
                var field = colorAttachment.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(colorAttachment);
                if (value instanceof Integer id) {
                    return id;
                }
            } catch (Throwable ignored) {
            }
        }

        return -1;
    }
}
