package com.raven.ravenz.utils.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raven.ravenz.utils.render.font.util.BufferUtils;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class Render3DEngine {
    private Render3DEngine() {
    }

    public static void setup() {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        GlStateManager._disableCull();
        CompatShaders.usePositionColor();
    }

    public static void setupThroughWalls() {
        setup();
        GlStateManager._disableDepthTest();
    }

    public static void end() {
        GlStateManager._enableCull();
        GlStateManager._enableDepthTest();
        GlStateManager._disableBlend();
    }

    public static void drawBox(MatrixStack stack, Box box, Color color) {
        drawFilledBox(stack, box, color);
    }

    public static void drawBox(MatrixStack stack, double x, double y, double z, float width, float height, Color color) {
        float halfWidth = width / 2.0f;
        Box box = new Box(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
        drawFilledBox(stack, box, color);
    }

    public static void drawOutlineBox(MatrixStack stack, Box box, Color color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        addLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        addLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        addLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);

        BufferUtils.draw(buffer);
    }

    public static void drawOutlineBox(MatrixStack stack, double x, double y, double z, float width, float height, Color color) {
        float halfWidth = width / 2.0f;
        Box box = new Box(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
        drawOutlineBox(stack, box, color);
    }

    public static void drawFilledBox(MatrixStack stack, Box box, Color color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        addTri(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addTri(buffer, matrix, minX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);

        addTri(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addTri(buffer, matrix, maxX, minY, minZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);

        addTri(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addTri(buffer, matrix, maxX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);

        addTri(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addTri(buffer, matrix, minX, minY, maxZ, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);

        addTri(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addTri(buffer, matrix, minX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);

        addTri(buffer, matrix, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);
        addTri(buffer, matrix, minX, minY, minZ, maxX, minY, maxZ, maxX, minY, minZ, r, g, b, a);

        BufferUtils.draw(buffer);
    }

    public static void drawFilledBox(MatrixStack stack, double x, double y, double z, float width, float height, Color color) {
        float halfWidth = width / 2.0f;
        Box box = new Box(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
        drawFilledBox(stack, box, color);
    }

    public static void drawLine(MatrixStack stack, Vec3d start, Vec3d end, Color color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        addLine(
                buffer,
                matrix,
                (float) start.x,
                (float) start.y,
                (float) start.z,
                (float) end.x,
                (float) end.y,
                (float) end.z,
                r,
                g,
                b,
                a
        );

        BufferUtils.draw(buffer);
    }

    public static Vec3d getInterpolatedPos(Entity entity, float tickDelta) {
        return entity.getLerpedPos(tickDelta);
    }

    private static void addLine(BufferBuilder buffer, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }

    private static void addTri(BufferBuilder buffer, Matrix4f matrix,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
    }
}
