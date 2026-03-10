package com.raven.ravenz.utils.render.blur;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

import java.awt.Color;

public final class BlurRenderer {
    private static final BlurBuilder BLUR_BUILDER = new BlurBuilder();

    private BlurRenderer() {}

    public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, 
                                 float cornerRadius, Color color, float blurRadius) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BuiltBlur blur = BLUR_BUILDER
                .size(width, height)
                .radius(cornerRadius)
                .color(color)
                .smoothness(1.0f)
                .blurRadius(blurRadius)
                .build();
        blur.render(matrix, x, y, 0);
    }

    public static void drawBlur(Matrix3x2fStack matrices, float x, float y, float width, float height,
                                float cornerRadius, Color color, float blurRadius) {
        BuiltBlur blur = BLUR_BUILDER
                .size(width, height)
                .radius(cornerRadius)
                .color(color)
                .smoothness(1.0f)
                .blurRadius(blurRadius)
                .build();
        blur.render(toMatrix4f(matrices), x, y, 0);
    }

    public static void drawBlurWithCustomRadius(MatrixStack matrices, float x, float y, 
                                                  float width, float height, float[] cornerRadii, 
                                                  Color color, float blurRadius) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[] finalRadii = new float[4];
        for (int i = 0; i < 4; i++) {
            finalRadii[i] = cornerRadii[i];
            if (cornerRadii[i] != 0) {
                finalRadii[i] = cornerRadii[i] - 2;
            }
        }
        BuiltBlur blur = BLUR_BUILDER
                .size(width, height)
                .radius(new QuadRadiusState(finalRadii[0], finalRadii[1], finalRadii[2], finalRadii[3]))
                .color(color)
                .smoothness(1.0f)
                .blurRadius(blurRadius)
                .build();
        blur.render(matrix, x, y, 0);
    }

    public static void drawBlurWithCustomRadius(Matrix3x2fStack matrices, float x, float y,
                                                  float width, float height, float[] cornerRadii,
                                                  Color color, float blurRadius) {
        float[] finalRadii = new float[4];
        for (int i = 0; i < 4; i++) {
            finalRadii[i] = cornerRadii[i];
            if (cornerRadii[i] != 0) {
                finalRadii[i] = cornerRadii[i] - 2;
            }
        }
        BuiltBlur blur = BLUR_BUILDER
                .size(width, height)
                .radius(new QuadRadiusState(finalRadii[0], finalRadii[1], finalRadii[2], finalRadii[3]))
                .color(color)
                .smoothness(1.0f)
                .blurRadius(blurRadius)
                .build();
        blur.render(toMatrix4f(matrices), x, y, 0);
    }

    public static void drawStyledRect(MatrixStack matrices, float x, float y, float width, float height, 
                                       float cornerRadius, Color backgroundColor) {
        drawBlur(matrices, x, y, width, height, cornerRadius, Color.WHITE, 12);
    }

    public static void drawStyledRectEx(MatrixStack matrices, float x, float y, float width, float height, 
                                         float[] cornerRadii, Color backgroundColor, float blurRadius) {
        drawBlurWithCustomRadius(matrices, x, y, width, height, cornerRadii, Color.WHITE, blurRadius);
    }

    public static void drawSimpleBlur(MatrixStack matrices, float x, float y, float width, float height) {
        drawBlur(matrices, x, y, width, height, 0, Color.WHITE, 10);
    }

    public static void drawRoundedBlur(MatrixStack matrices, float x, float y, float width, float height, 
                                         float cornerRadius) {
        drawBlur(matrices, x, y, width, height, cornerRadius, Color.WHITE, 12);
    }

    private static Matrix4f toMatrix4f(Matrix3x2fStack matrix) {
        Matrix4f out = new Matrix4f();
        out.identity();
        out.m00(matrix.m00());
        out.m01(matrix.m01());
        out.m10(matrix.m10());
        out.m11(matrix.m11());
        out.m30(matrix.m20());
        out.m31(matrix.m21());
        return out;
    }
}
