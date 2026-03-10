package com.raven.ravenz.utils.render.shader.renderers;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raven.ravenz.utils.render.shader.IRenderer;
import com.raven.ravenz.utils.render.shader.states.QuadColorState;
import com.raven.ravenz.utils.render.shader.states.QuadRadiusState;
import com.raven.ravenz.utils.render.shader.states.SizeState;
import com.raven.ravenz.utils.render.CompatShaders;
import com.raven.ravenz.utils.render.font.util.BufferUtils;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public record BuiltRectangle(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness
) implements IRenderer {
    @Override
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

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addQuad(builder, matrix, x, y, z, width, height, color.color1(), color.color2(), color.color3(), color.color4());
        BufferUtils.draw(builder);

        GlStateManager._enableCull();
        GlStateManager._disableBlend();
    }

    private static void addQuad(BufferBuilder builder, Matrix4f matrix,
                                float x, float y, float z,
                                float width, float height,
                                int c1, int c2, int c3, int c4) {
        put(builder, matrix, x, y, z, c1);
        put(builder, matrix, x, y + height, z, c2);
        put(builder, matrix, x + width, y + height, z, c3);
        put(builder, matrix, x + width, y, z, c4);
    }

    private static void put(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        builder.vertex(matrix, x, y, z).color(r, g, b, a);
    }
}
