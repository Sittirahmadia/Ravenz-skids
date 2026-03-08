package com.raven.ravenz.utils.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.raven.ravenz.utils.render.font.util.BufferUtils;

public final class TextureRenderer {
    private TextureRenderer() {
    }

    public static void enableLinearFiltering() {
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    public static void disableLinearFiltering() {
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    public static void enableMask() {
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
        GlStateManager._depthMask(false);
        GlStateManager._colorMask(false, false, false, false);
    }

    public static void applyMask() {
        GlStateManager._depthFunc(GL11.GL_EQUAL);
        GlStateManager._depthMask(false);
        GlStateManager._colorMask(true, true, true, true);
    }

    public static void disableMask() {
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
    }

    public static void drawCenteredQuad(MatrixStack matrices, Identifier texture, float width, float height, int color) {
        drawCenteredQuad(matrices, texture, width, height, color, false);
    }

    public static void drawCenteredQuad(MatrixStack matrices, Identifier texture, float width, float height, int color,
                                        boolean linearFilter) {
        if (matrices == null || texture == null) {
            return;
        }

        if (linearFilter) {
            enableLinearFiltering();
        }

        float a = ((color >>> 24) & 0xFF) / 255.0f;
        float r = ((color >>> 16) & 0xFF) / 255.0f;
        float g = ((color >>> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -halfWidth, halfHeight, 0.0f).texture(0.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(matrix, halfWidth, halfHeight, 0.0f).texture(1.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(matrix, halfWidth, -halfHeight, 0.0f).texture(1.0f, 0.0f).color(r, g, b, a);
        buffer.vertex(matrix, -halfWidth, -halfHeight, 0.0f).texture(0.0f, 0.0f).color(r, g, b, a);
        BufferUtils.draw(buffer, RenderLayers.entityTranslucent(texture));

        if (linearFilter) {
            disableLinearFiltering();
        }
    }

    public static void drawCenteredQuad(DrawContext context, Identifier texture, float x, float y, float width,
                                        float height, float rotationDeg, int color) {
        drawCenteredQuad(context, texture, x, y, width, height, rotationDeg, color, false);
    }

    public static void drawCenteredQuad(DrawContext context, Identifier texture, float x, float y, float width,
                                        float height, float rotationDeg, int color, boolean linearFilter) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        if (rotationDeg != 0.0f) {
            matrices.rotate((float) Math.toRadians(rotationDeg));
        }
        int w = Math.max(1, Math.round(width));
        int h = Math.max(1, Math.round(height));
        int left = Math.round(-width * 0.5f);
        int top = Math.round(-height * 0.5f);

        if (linearFilter) {
            enableLinearFiltering();
        }

        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, left, top, 0f, 0f, w, h, w, h);

        if (linearFilter) {
            disableLinearFiltering();
        }
        matrices.popMatrix();
    }

    public static void drawMaskedQuad(DrawContext context, Identifier texture, float x, float y, float width,
                                      float height, int color) {
        drawMaskedQuad(context, texture, x, y, width, height, 0.0f, color, false);
    }

    public static void drawMaskedQuad(DrawContext context, Identifier texture, float x, float y, float width,
                                      float height, float rotationDeg, int color, boolean linearFilter) {
        drawCenteredQuad(context, texture, x, y, width, height, rotationDeg, color, linearFilter);
    }
}
