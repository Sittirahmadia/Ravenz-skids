package com.raven.ravenz.module.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.raven.ravenz.event.impl.render.Render3DEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.ColorSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.render.Render3DEngine;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public final class TargetESP extends Module {

    private static final Identifier FIREFLY_TEXTURE = Identifier.of("krypton", "textures/visuals/firefly.png");
    private static final BufferAllocator TARGET_ESP_BUFFER_ALLOCATOR = new BufferAllocator(1 << 16);

    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Living");
    private final NumberSetting layers = new NumberSetting("Layers", 1, 5, 3, 1);
    private final NumberSetting orbsPerLayer = new NumberSetting("Orbs Per Layer", 5, 20, 14, 1);
    private final NumberSetting orbSize = new NumberSetting("Orb Size", 0.1, 0.8, 0.3, 0.05);
    private final NumberSetting speed = new NumberSetting("Speed", 0.5, 5.0, 2.5, 0.1);
    private final NumberSetting heightOffset = new NumberSetting("Height Offset", 0.0, 2.0, 1.0, 0.1);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Single", "Single", "Gradient");
    private final ColorSetting color = new ColorSetting("Color", new Color(120, 240, 255, 220));
    private final ColorSetting gradientColor1 = new ColorSetting("Gradient Color 1", new Color(255, 0, 0, 220));
    private final ColorSetting gradientColor2 = new ColorSetting("Gradient Color 2", new Color(0, 0, 255, 220));

    public TargetESP() {
        super("Target ESP", "Ghost-like spiral orbs around players", -1, Category.RENDER);
        addSettings(targets, layers, orbsPerLayer, orbSize, speed, heightOffset, colorMode, color,
                gradientColor1, gradientColor2);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (isNull()) {
            return;
        }

        MatrixStack stack = event.getMatrixStack();
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);

        float size = orbSize.getValueFloat();
        float yOff = heightOffset.getValueFloat();
        Color baseColor = color.getValue();
        int numLayers = layers.getValueInt();
        int orbCount = orbsPerLayer.getValueInt();
        boolean useGradient = colorMode.isMode("Gradient");

        float age = (System.currentTimeMillis() % 100000L) / 50.0f;
        float ageMultiplier = age * speed.getValueFloat();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity)) {
                continue;
            }

            Vec3d pos = Render3DEngine.getInterpolatedPos(entity, tickDelta);
            double baseX = pos.x;
            double baseY = pos.y + yOff;
            double baseZ = pos.z;

            if (mc.player.canSee(entity)) {
                GlStateManager._enableDepthTest();
                GlStateManager._depthMask(false);
            } else {
                GlStateManager._disableDepthTest();
            }

            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(TARGET_ESP_BUFFER_ALLOCATOR);
            VertexConsumer consumer = immediate.getBuffer(RenderLayers.entityTranslucent(FIREFLY_TEXTURE));

            for (int layer = 0; layer < numLayers; layer++) {
                float layerOffset = layer * 120.0f;
                float layerMultiplier = layer + 1.0f;

                for (int i = 0; i <= orbCount; i++) {
                    float iFloat = (float) i;
                    double radians = Math.toRadians(((iFloat / 1.5f + age * speed.getValueFloat()) * 8.0f + layerOffset) % 2880.0f);
                    double sinQuad = Math.sin(Math.toRadians(ageMultiplier + i * layerMultiplier) * 3.0f) / 1.8f;
                    float offset = iFloat / orbCount;

                    int orbColor;
                    if (useGradient) {
                        Color blendedColor = blendColors(gradientColor1.getValue(), gradientColor2.getValue(), offset);
                        orbColor = applyOpacity(blendedColor.getRGB(), offset);
                    } else {
                        orbColor = applyOpacity(baseColor.getRGB(), offset);
                    }

                    double ghostX = Math.cos(radians) * entity.getWidth();
                    double ghostY = sinQuad;
                    double ghostZ = Math.sin(radians) * entity.getWidth();

                    stack.push();
                    stack.translate(baseX + ghostX, baseY + ghostY, baseZ + ghostZ);
                    stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                    stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

                    var matrix = stack.peek().getPositionMatrix();

                    float a = ((orbColor >>> 24) & 0xFF) / 255.0f;
                    float r = ((orbColor >>> 16) & 0xFF) / 255.0f;
                    float g = ((orbColor >>> 8) & 0xFF) / 255.0f;
                    float b = (orbColor & 0xFF) / 255.0f;

                    int ir = Math.max(0, Math.min(255, Math.round(r * 255.0f)));
                    int ig = Math.max(0, Math.min(255, Math.round(g * 255.0f)));
                    int ib = Math.max(0, Math.min(255, Math.round(b * 255.0f)));
                    int ia = Math.max(0, Math.min(255, Math.round(a * 255.0f)));

                    consumer.vertex(matrix, -size, size, 0.0f).texture(0.0f, 1.0f).color(ir, ig, ib, ia).overlay(0).light(0xF000F0);
                    consumer.vertex(matrix, size, size, 0.0f).texture(1.0f, 1.0f).color(ir, ig, ib, ia).overlay(0).light(0xF000F0);
                    consumer.vertex(matrix, size, -size, 0.0f).texture(1.0f, 0.0f).color(ir, ig, ib, ia).overlay(0).light(0xF000F0);
                    consumer.vertex(matrix, -size, -size, 0.0f).texture(0.0f, 0.0f).color(ir, ig, ib, ia).overlay(0).light(0xF000F0);

                    stack.pop();
                }
            }

            immediate.draw();

            if (mc.player.canSee(entity)) {
                GlStateManager._depthMask(true);
            }
        }

        GlStateManager._enableDepthTest();
        GlStateManager._disableBlend();
    }

    private Color blendColors(Color color1, Color color2, float ratio) {
        ratio = Math.min(1, Math.max(0, ratio));
        int r = (int) (color1.getRed() * (1 - ratio) + color2.getRed() * ratio);
        int g = (int) (color1.getGreen() * (1 - ratio) + color2.getGreen() * ratio);
        int b = (int) (color1.getBlue() * (1 - ratio) + color2.getBlue() * ratio);
        int a = (int) (color1.getAlpha() * (1 - ratio) + color2.getAlpha() * ratio);
        return new Color(r, g, b, a);
    }

    private int applyOpacity(int colorInt, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        Color color = new Color(colorInt, true);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity)).getRGB();
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.player) return false;
        if (targets.isMode("Players")) return entity instanceof PlayerEntity;
        return entity instanceof LivingEntity;
    }
}
