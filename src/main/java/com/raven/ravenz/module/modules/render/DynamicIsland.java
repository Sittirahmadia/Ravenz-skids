package com.raven.ravenz.module.modules.render;

import com.raven.ravenz.event.impl.render.Render2DEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.*;
import com.raven.ravenz.utils.render.nanovg.NanoVGRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;

import java.awt.*;

public class DynamicIsland extends Module {
    private static final float OUTER_PADDING = 6f;
    private static final float TEXT_SIZE = 12f;
    private static final float CORNER_RADIUS = 14f;
    private static final float ISLAND_X = 8f;
    private static final float ISLAND_Y = 8f;
    private static final Color BACKGROUND_COLOR = new Color(12, 12, 12, 235);
    private static final Color TEXT_COLOR = new Color(230, 230, 230, 255);
    private static final Color ACCENT_COLOR = new Color(120, 170, 255, 255);
    private static final Color SEPARATOR_COLOR = new Color(70, 70, 70, 220);

    private final BooleanSetting showFps;

    public DynamicIsland() {
        super("Dynamic Island", "Shows a dynamic island HUD in the center-top", Category.RENDER);

        this.showFps = new BooleanSetting("Show FPS", true);
        addSettings(showFps);
        
        setEnabled(true);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!shouldRender()) {
            return;
        }

        boolean isInChat = mc.currentScreen instanceof ChatScreen;
        if (mc.currentScreen != null && !isInChat) {
            return;
        }

        render();
    }

    private boolean shouldRender() {
        return mc.player != null && mc.world != null;
    }

    private void render() {
        String title = "RidhoXNoqwd";
        String fpsText = "⚡ " + mc.getCurrentFps() + " fps";

        float textHeight = NanoVGRenderer.getTextHeight(TEXT_SIZE);
        float boxHeight = textHeight + OUTER_PADDING * 2;
        float iconDotSize = 10f;

        float titleWidth = NanoVGRenderer.getTextWidth(title, TEXT_SIZE);
        float leftBoxWidth = OUTER_PADDING * 2 + iconDotSize + 6f + titleWidth;

        float x = ISLAND_X;
        float y = ISLAND_Y;

        NanoVGRenderer.drawRoundedRect(x, y, leftBoxWidth, boxHeight, CORNER_RADIUS, BACKGROUND_COLOR);

        float dotX = x + OUTER_PADDING;
        float dotY = y + (boxHeight - iconDotSize) / 2f;
        NanoVGRenderer.drawRoundedRect(dotX, dotY, iconDotSize, iconDotSize, iconDotSize / 2f, ACCENT_COLOR);

        float titleX = dotX + iconDotSize + 6f;
        float textY = y + OUTER_PADDING;
        NanoVGRenderer.drawText(title, titleX, textY, TEXT_SIZE, TEXT_COLOR);

        if (!showFps.getValue()) {
            return;
        }

        float rightGap = 6f;
        float fpsWidth = NanoVGRenderer.getTextWidth(fpsText, TEXT_SIZE);
        float rightBoxWidth = OUTER_PADDING * 2 + fpsWidth;
        float rightX = x + leftBoxWidth + rightGap;

        NanoVGRenderer.drawRoundedRect(rightX, y, rightBoxWidth, boxHeight, CORNER_RADIUS, BACKGROUND_COLOR);

        float separatorX = rightX - rightGap / 2f;
        float separatorY = y + 4f;
        NanoVGRenderer.drawRoundedRect(separatorX, separatorY, 1.5f, boxHeight - 8f, 1f, SEPARATOR_COLOR);

        float fpsX = rightX + OUTER_PADDING;
        NanoVGRenderer.drawText(fpsText, fpsX, textY, TEXT_SIZE, TEXT_COLOR);
    }
}
