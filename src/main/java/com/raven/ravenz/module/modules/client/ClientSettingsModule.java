package com.raven.ravenz.module.modules.client;

import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.*;

import java.awt.Color;

public class ClientSettingsModule extends Module {
    private static ClientSettingsModule INSTANCE;

    public final ColorSetting accentColor = new ColorSetting("Accent Color", new Color(124, 77, 255, 255));
    public final ModeSetting fontStyle = new ModeSetting("Font", "Inter.ttf", "Inter.ttf", "jetbrainsmono.ttf",
            "poppins-medium.ttf", "Monaco.ttf");
    public final BooleanSetting scrollableCategories = new BooleanSetting("Scrollable Categories", false);
    public final NumberSetting guiScale = new NumberSetting("GUI Scale", 0, 3, 1, 1);
    public final NumberSetting guiTransparency = new NumberSetting("GUI Transparency", 0, 100, 0, 1);
    public final BooleanSetting guiBlur = new BooleanSetting("GUI Blur", false);
    public final BooleanSetting panelBlur = new BooleanSetting("Panel Blur", false);
    public final NumberSetting panelBlurRadius = new NumberSetting("Panel Blur Radius", 5, 30, 12, 1);
    public final BooleanSetting moduleDescriptions = new BooleanSetting("Module Descriptions", true);

    public final NumberSetting toggleWidth = new NumberSetting("Toggle Width", 10, 40, 20, 1);
    public final NumberSetting toggleHeight = new NumberSetting("Toggle Height", 6, 20, 10, 1);
    public final NumberSetting sliderHeight = new NumberSetting("Slider Height", 2, 10, 4, 0.5);
    public final NumberSetting sliderHandleSize = new NumberSetting("Slider Handle Size", 3, 8, 5, 0.5);

    public final BooleanSetting guiGlow = new BooleanSetting("GUI Glow", false);
    public final ColorSetting glowColor = new ColorSetting("Glow Color", new Color(124, 77, 255, 255));
    public final NumberSetting glowIntensity = new NumberSetting("Glow Intensity", 0, 2, 1, 0.1);
    public final NumberSetting glowThickness = new NumberSetting("Glow Thickness", 2, 10, 5, 1);
    public final NumberSetting bloomRadius = new NumberSetting("Bloom Radius", 5, 15, 10, 1);

    public final BooleanSetting autoFocusSearch = new BooleanSetting("Auto Focus Search", true);
    public final BooleanSetting snowEffect = new BooleanSetting("Snow Effect", false);
    public final BooleanSetting autoSaveSettings = new BooleanSetting("Auto Save Settings", false);

    public ClientSettingsModule() {
        super("ClientSettings", "Customize GUI appearance", 0, Category.CLIENT);
        INSTANCE = this;
        this.addSettings(accentColor, fontStyle, scrollableCategories, guiScale, guiTransparency, guiBlur, panelBlur, panelBlurRadius, moduleDescriptions,
                toggleWidth, toggleHeight, sliderHeight, sliderHandleSize,
                guiGlow, glowColor, glowIntensity, glowThickness, bloomRadius,
                autoFocusSearch, snowEffect, autoSaveSettings);
        this.setEnabled(true);
    }

    public static Color getAccentColor() {
        return INSTANCE != null ? INSTANCE.accentColor.getValue() : new Color(124, 77, 255, 255);
    }

    public static String getFontStyle() {
        return INSTANCE != null ? INSTANCE.fontStyle.getMode() : "Inter.ttf";
    }

    public static boolean isScrollable() {
        return INSTANCE != null && INSTANCE.scrollableCategories.getValue();
    }

    public static float getGuiTransparency() {
        return INSTANCE != null ? INSTANCE.guiTransparency.getValueFloat() / 100f : 0f;
    }

    public static boolean isGuiBlurEnabled() {
        return INSTANCE != null && INSTANCE.guiBlur.getValue();
    }

    public static boolean isPanelBlurEnabled() {
        return INSTANCE != null && INSTANCE.panelBlur.getValue();
    }

    public static float getPanelBlurRadius() {
        return INSTANCE != null ? INSTANCE.panelBlurRadius.getValueFloat() : 12f;
    }

    public static boolean isModuleDescriptionsEnabled() {
        return INSTANCE == null || INSTANCE.moduleDescriptions.getValue();
    }

    public static float getToggleWidth() {
        return INSTANCE != null ? INSTANCE.toggleWidth.getValueFloat() : 20f;
    }

    public static float getToggleHeight() {
        return INSTANCE != null ? INSTANCE.toggleHeight.getValueFloat() : 10f;
    }

    public static float getSliderHeight() {
        return INSTANCE != null ? INSTANCE.sliderHeight.getValueFloat() : 4f;
    }

    public static float getSliderHandleSize() {
        return INSTANCE != null ? INSTANCE.sliderHandleSize.getValueFloat() : 5f;
    }

    public static boolean isGuiGlowEnabled() {
        return INSTANCE != null && INSTANCE.guiGlow.getValue();
    }

    public static Color getGlowColor() {
        return INSTANCE != null ? INSTANCE.glowColor.getValue() : new Color(124, 77, 255, 255);
    }

    public static float getGlowIntensity() {
        return INSTANCE != null ? INSTANCE.glowIntensity.getValueFloat() : 1f;
    }

    public static float getGlowThickness() {
        return INSTANCE != null ? INSTANCE.glowThickness.getValueFloat() : 5f;
    }

    public static float getBloomRadius() {
        return INSTANCE != null ? INSTANCE.bloomRadius.getValueFloat() : 10f;
    }

    public static boolean isAutoFocusSearchEnabled() {
        return INSTANCE != null && INSTANCE.autoFocusSearch.getValue();
    }

    public static boolean isSnowEffectEnabled() {
        return INSTANCE != null && INSTANCE.snowEffect.getValue();
    }

    public static int getGuiScale() {
        return INSTANCE != null ? (int) INSTANCE.guiScale.getValue() : 1;
    }

    public static boolean isAutoSaveEnabled() {
        return INSTANCE != null && INSTANCE.autoSaveSettings.getValue();
    }
}
