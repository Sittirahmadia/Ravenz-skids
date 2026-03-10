package com.raven.ravenz.utils.render.nanovg;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class NanoVGRenderer {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final int FALLBACK_FONT_ID = 1;
    private static final Map<Integer, Identifier> IMAGES = new HashMap<>();

    private static DrawContext currentContext = null;
    private static int frameDepth = 0;
    private static int scissorDepth = 0;
    private static int nextImageId = 1;

    public static void init() {}

    public static void reinit() {}

    public static boolean beginFrame(DrawContext context) {
        if (context == null) return false;
        currentContext = context;
        frameDepth++;
        return true;
    }

    public static boolean beginFrame() {
        if (currentContext == null) return false;
        frameDepth++;
        return true;
    }

    public static boolean isAvailable() {
        return true;
    }

    public static void endFrame() {
        if (frameDepth <= 0) return;
        frameDepth--;
        if (frameDepth == 0) {
            while (scissorDepth > 0 && currentContext != null) {
                currentContext.disableScissor();
                scissorDepth--;
            }
            currentContext = null;
        }
    }

    public static boolean isInFrame() {
        return frameDepth > 0 && currentContext != null;
    }

    public static void drawRect(float x, float y, float width, float height, Color color) {
        if (!ready()) return;
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.round(x + width);
        int y2 = Math.round(y + height);
        if (x2 <= x1 || y2 <= y1) return;
        currentContext.fill(x1, y1, x2, y2, argb(color));
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        drawRect(x, y, width, height, color);
    }

    public static void drawRoundedRectVarying(float x, float y, float width, float height,
                                              float radiusTopLeft, float radiusTopRight,
                                              float radiusBottomRight, float radiusBottomLeft, Color color) {
        drawRect(x, y, width, height, color);
    }

    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius,
                                              float strokeWidth, Color color) {
        if (!ready()) return;
        float sw = Math.max(1f, strokeWidth);
        drawRect(x, y, width, sw, color);
        drawRect(x, y + height - sw, width, sw, color);
        drawRect(x, y + sw, sw, Math.max(0f, height - sw * 2), color);
        drawRect(x + width - sw, y + sw, sw, Math.max(0f, height - sw * 2), color);
    }

    public static void drawRoundedRectGradient(float x, float y, float width, float height,
                                               float radius, Color colorTop, Color colorBottom) {
        if (!ready()) return;
        int steps = Math.max(1, Math.round(height / 2f));
        float stepH = height / steps;
        for (int i = 0; i < steps; i++) {
            float t = (float) i / Math.max(1, steps - 1);
            drawRect(x, y + i * stepH, width, stepH + 1f, lerp(colorTop, colorBottom, t));
        }
    }

    public static void drawRoundedRectWithShadow(float x, float y, float width, float height,
                                                 float radius, Color color, Color shadowColor,
                                                 float shadowBlur, float shadowSpread) {
        drawRect(x - shadowSpread, y - shadowSpread, width + shadowSpread * 2f, height + shadowSpread * 2f,
                shadowColor);
        drawRoundedRect(x, y, width, height, radius, color);
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        if (!ready()) return;
        int cx = Math.round(x);
        int cy = Math.round(y);
        int r = Math.max(1, Math.round(radius));
        int c = argb(color);
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - (double) dy * dy);
            currentContext.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, c);
        }
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float strokeWidth, Color color) {
        if (!ready()) return;
        int steps = Math.max(1, Math.round(Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1))));
        float r = Math.max(0.5f, strokeWidth / 2f);
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float px = x1 + (x2 - x1) * t;
            float py = y1 + (y2 - y1) * t;
            drawCircle(px, py, r, color);
        }
    }

    public static void drawText(String text, float x, float y, float size, Color color) {
        renderText(text, x, y, size, color, false);
    }

    public static void drawText(String text, float x, float y, float size, Color color, boolean bold) {
        renderText(text, x, y, size, color, false);
    }

    public static void drawTextWithFont(String text, float x, float y, float size, Color color, int fontId) {
        renderText(text, x, y, size, color, false);
    }

    public static void drawTextWithShadow(String text, float x, float y, float size, Color color) {
        renderText(text, x, y, size, color, true);
    }

    public static void drawTextWithShadow(String text, float x, float y, float size, Color color, int fontId) {
        renderText(text, x, y, size, color, true);
    }

    public static void drawTextWithShadow(String text, float x, float y, float size, Color color,
                                          float shadowOffsetX, float shadowOffsetY, Color shadowColor) {
        renderText(text, x + shadowOffsetX, y + shadowOffsetY, size, shadowColor, false);
        renderText(text, x, y, size, color, false);
    }

    public static void drawTextWithShadow(String text, float x, float y, float size, Color color, int fontId,
                                          float shadowOffsetX, float shadowOffsetY, Color shadowColor) {
        drawTextWithShadow(text, x, y, size, color, shadowOffsetX, shadowOffsetY, shadowColor);
    }

    public static void drawIcon(String icon, float x, float y, float size, Color color) {
        renderText(icon, x, y, size, color, false);
    }

    public static float getTextWidth(String text, float size) {
        if (text == null || text.isEmpty() || size <= 0f) return 0f;
        return MC.textRenderer.getWidth(text) * (size / 9f);
    }

    public static float getTextWidth(String text, float size, boolean bold) {
        return getTextWidth(text, size);
    }

    public static float getTextWidthWithFont(String text, float size, int fontId) {
        return getTextWidth(text, size);
    }

    public static float getTextHeight(float size) {
        return Math.max(0f, size);
    }

    public static int getPoppinsFontId() {
        return FALLBACK_FONT_ID;
    }

    public static int getJetBrainsFontId() {
        return FALLBACK_FONT_ID;
    }

    public static int getRegularFontId() {
        return FALLBACK_FONT_ID;
    }

    public static int getMonacoFontId() {
        return FALLBACK_FONT_ID;
    }

    public static int loadImage(String path) {
        Identifier id = parseIdentifier(path);
        if (id == null) return -1;
        int imageId = nextImageId++;
        IMAGES.put(imageId, id);
        return imageId;
    }

    public static void drawImage(int imageId, float x, float y, float width, float height, Color tint) {
        if (!ready()) return;
        Identifier id = IMAGES.get(imageId);
        if (id == null) return;
        int w = Math.max(1, Math.round(width));
        int h = Math.max(1, Math.round(height));
        currentContext.drawTexture(RenderPipelines.GUI_TEXTURED, id,
                Math.round(x), Math.round(y),
                0f, 0f,
                w, h,
                w, h);
    }

    public static int loadImageFromGLTexture(int glTextureId, int width, int height) {
        return -1;
    }

    public static void deleteImage(int imageId) {
        IMAGES.remove(imageId);
    }

    public static void drawImageRegion(int imageId,
                                       float srcX, float srcY, float srcW, float srcH,
                                       float texW, float texH,
                                       float destX, float destY, float destW, float destH,
                                       Color tint) {
        if (!ready()) return;
        Identifier id = IMAGES.get(imageId);
        if (id == null) return;
        currentContext.drawTexture(RenderPipelines.GUI_TEXTURED, id,
                Math.round(destX), Math.round(destY),
                srcX, srcY,
                Math.max(1, Math.round(destW)), Math.max(1, Math.round(destH)),
                Math.max(1, Math.round(texW)), Math.max(1, Math.round(texH)));
    }

    public static void save() {
        if (!ready()) return;
        currentContext.getMatrices().pushMatrix();
    }

    public static void restore() {
        if (!ready()) return;
        currentContext.getMatrices().popMatrix();
    }

    public static void translate(float x, float y) {
        if (!ready()) return;
        currentContext.getMatrices().translate(x, y);
    }

    public static void scale(float x, float y) {
        if (!ready()) return;
        currentContext.getMatrices().scale(x, y);
    }

    public static void scissor(float x, float y, float width, float height) {
        if (!ready()) return;
        currentContext.enableScissor(
                Math.round(x),
                Math.round(y),
                Math.round(x + width),
                Math.round(y + height)
        );
        scissorDepth++;
    }

    public static void resetScissor() {
        if (!ready() || scissorDepth <= 0) return;
        currentContext.disableScissor();
        scissorDepth--;
    }

    public static void cleanup() {
        IMAGES.clear();
        currentContext = null;
        frameDepth = 0;
        scissorDepth = 0;
    }

    private static boolean ready() {
        return frameDepth > 0 && currentContext != null;
    }

    private static int argb(Color color) {
        return (color.getAlpha() & 0xFF) << 24
                | (color.getRed() & 0xFF) << 16
                | (color.getGreen() & 0xFF) << 8
                | (color.getBlue() & 0xFF);
    }

    private static void renderText(String text, float x, float y, float size, Color color, boolean shadow) {
        if (!ready() || text == null || text.isEmpty()) return;
        float scale = Math.max(0.01f, size / 9f);
        var matrices = currentContext.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        currentContext.drawText(MC.textRenderer, text, 0, 0, argb(color), shadow);
        matrices.popMatrix();
    }

    private static Identifier parseIdentifier(String path) {
        if (path == null || path.isEmpty()) return null;
        String p = path.trim();
        try {
            if (p.startsWith("assets/")) {
                String rest = p.substring("assets/".length());
                int slash = rest.indexOf('/');
                if (slash <= 0 || slash + 1 >= rest.length()) return null;
                String namespace = rest.substring(0, slash);
                String innerPath = rest.substring(slash + 1);
                return Identifier.of(namespace, innerPath);
            }
            if (p.contains(":")) {
                return Identifier.of(p);
            }
            return Identifier.of("krypton", p);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Color lerp(Color a, Color b, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * clamped);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        int al = Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped);
        return new Color(r, g, bl, al);
    }
}
