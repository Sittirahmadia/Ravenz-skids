package com.raven.ravenz.module.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.raven.ravenz.event.impl.render.Render2DEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ColorSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.render.TextureRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class ArrowESP extends Module {
    private static final Identifier ARROW_TEXTURE = Identifier.of("krypton", "textures/visuals/triangle.png");

    private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
    private final NumberSetting range = new NumberSetting("Range", 10.0D, 200.0D, 120.0D, 5.0D);
    private final NumberSetting size = new NumberSetting("Size", 8.0D, 64.0D, 24.0D, 1.0D);
    private final NumberSetting offset = new NumberSetting("Offset", 0.0D, 64.0D, 28.0D, 1.0D);
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255, 200), true);

    public ArrowESP() {
        super("Arrow ESP", "Displays arrows pointing to players", -1, Category.RENDER);
        addSettings(showSelf, range, size, offset, color);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (isNull()) {
            return;
        }

        DrawContext context = event.getContext();
        int width = event.getWidth();
        int height = event.getHeight();
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();

        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );

        Color arrowColor = color.getValue();
        int baseAlpha = arrowColor.getAlpha();
        float sizeValue = size.getValueFloat();
        float offsetValue = offset.getValueFloat();
        double maxRange = range.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRender(player)) {
                continue;
            }

            double distanceSq = mc.player.squaredDistanceTo(player);
            if (distanceSq > maxRange * maxRange) {
                continue;
            }

            Vec3d playerPos = player.getLerpedPos(tickDelta);
            double dx = playerPos.x - cameraPos.x;
            double dz = playerPos.z - cameraPos.z;

            double planar = Math.sqrt(dx * dx + dz * dz);
            if (planar < 1.0E-6) {
                continue;
            }

            float yawToPlayer = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0F - cameraYaw);

            double distance = Math.sqrt(distanceSq);
            float distanceFactor = (float) Math.min(distance / maxRange, 1.0D);
            float alphaScale = 1.0f - distanceFactor * 0.6f;
            int arrowAlpha = Math.max(24, Math.min(255, (int) (baseAlpha * alphaScale)));
            int packed = (arrowAlpha << 24)
                    | (arrowColor.getRed() << 16)
                    | (arrowColor.getGreen() << 8)
                    | arrowColor.getBlue();

            float drawX = centerX;
            float drawY = centerY - (offsetValue + distanceFactor * offsetValue);
            TextureRenderer.drawCenteredQuad(
                    context,
                    ARROW_TEXTURE,
                    drawX,
                    drawY,
                    sizeValue,
                    sizeValue,
                    yawToPlayer,
                    packed,
                    true
            );
        }

        GlStateManager._disableBlend();
        GlStateManager._depthMask(true);
        GlStateManager._enableDepthTest();
    }

    private boolean shouldRender(PlayerEntity player) {
        if (player == mc.player && !showSelf.getValue()) {
            return false;
        }
        return !player.isSpectator();
    }
}
