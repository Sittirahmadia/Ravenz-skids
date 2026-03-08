package com.raven.ravenz.utils.render.blur;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raven.ravenz.utils.render.CompatShaders;
import com.raven.ravenz.utils.render.font.util.BufferUtils;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public record BuiltBlur(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float blurRadius
) {
    public void render(Matrix4f matrix, float x, float y, float z) {
        float width = size.width();
        float height = size.height();
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        GlStateManager._disableCull();
        CompatShaders.usePositionColor();

        int passes = Math.max(1, Math.min(16, Math.round(Math.max(1.0f, blurRadius))));
        int baseColor = averageColor();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = passes; i >= 1; i--) {
            float spread = i * 0.75f;
            float fade = (float) i / (float) passes;
            int colorWithAlpha = withScaledAlpha(baseColor, fade * 0.20f * Math.max(0.25f, smoothness));
            addQuad(builder, matrix,
                    x - spread,
                    y - spread,
                    z,
                    width + spread * 2.0f,
                    height + spread * 2.0f,
                    colorWithAlpha);
        }

        int centerColor = withScaledAlpha(baseColor, 0.12f * Math.max(0.25f, smoothness));
        addQuad(builder, matrix, x, y, z, width, height, centerColor);

        BufferUtils.draw(builder);

        GlStateManager._enableCull();
        GlStateManager._disableBlend();
    }

    private int averageColor() {
        int a = (((color.color1() >>> 24) & 0xFF)
                + ((color.color2() >>> 24) & 0xFF)
                + ((color.color3() >>> 24) & 0xFF)
                + ((color.color4() >>> 24) & 0xFF)) / 4;
        int r = (((color.color1() >>> 16) & 0xFF)
                + ((color.color2() >>> 16) & 0xFF)
                + ((color.color3() >>> 16) & 0xFF)
                + ((color.color4() >>> 16) & 0xFF)) / 4;
        int g = (((color.color1() >>> 8) & 0xFF)
                + ((color.color2() >>> 8) & 0xFF)
                + ((color.color3() >>> 8) & 0xFF)
                + ((color.color4() >>> 8) & 0xFF)) / 4;
        int b = ((color.color1() & 0xFF)
                + (color.color2() & 0xFF)
                + (color.color3() & 0xFF)
                + (color.color4() & 0xFF)) / 4;
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static int withScaledAlpha(int argb, float scale) {
        int alpha = (argb >>> 24) & 0xFF;
        int scaled = Math.max(0, Math.min(255, Math.round(alpha * Math.max(0.0f, Math.min(1.0f, scale)))));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    private static void addQuad(BufferBuilder builder, Matrix4f matrix,
                                float x, float y, float z,
                                float width, float height,
                                int argb) {
        put(builder, matrix, x, y, z, argb);
        put(builder, matrix, x, y + height, z, argb);
        put(builder, matrix, x + width, y + height, z, argb);
        put(builder, matrix, x + width, y, z, argb);
    }

    private static void put(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        builder.vertex(matrix, x, y, z).color(r, g, b, a);
    }
}
