package com.raven.ravenz.module.modules.client;

import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.ColorSetting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class KeybindsModule extends Module {
    private static KeybindsModule INSTANCE;

    public final ColorSetting backgroundColor = new ColorSetting("Background Color", new Color(18, 18, 22, 240));
    public final ColorSetting keyColor = new ColorSetting("Key Color", new Color(35, 35, 40, 255));
    public final ColorSetting keyHoverColor = new ColorSetting("Key Hover Color", new Color(50, 50, 60, 255));
    public final ColorSetting keyAssignedColor = new ColorSetting("Key Assigned Color", new Color(88, 101, 242, 255));
    public final ColorSetting keySelectedColor = new ColorSetting("Key Selected Color", new Color(100, 255, 100, 255));
    public final ColorSetting accentColor = new ColorSetting("Accent Color", new Color(88, 101, 242, 255));

    public KeybindsModule() {
        super("Keybinds", "Visual keyboard keybind editor", GLFW.GLFW_KEY_K, Category.CLIENT);
        INSTANCE = this;
        addSettings(backgroundColor, keyColor, keyHoverColor, keyAssignedColor, keySelectedColor, accentColor);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            mc.setScreen(new com.raven.ravenz.gui.KeybindsScreen());
        }
        setEnabled(false);
    }

    public static Color getBackgroundColor() {
        return INSTANCE != null ? INSTANCE.backgroundColor.getValue() : new Color(18, 18, 22, 240);
    }

    public static Color getKeyColor() {
        return INSTANCE != null ? INSTANCE.keyColor.getValue() : new Color(35, 35, 40, 255);
    }

    public static Color getKeyHoverColor() {
        return INSTANCE != null ? INSTANCE.keyHoverColor.getValue() : new Color(50, 50, 60, 255);
    }

    public static Color getKeyAssignedColor() {
        return INSTANCE != null ? INSTANCE.keyAssignedColor.getValue() : new Color(88, 101, 242, 255);
    }

    public static Color getKeySelectedColor() {
        return INSTANCE != null ? INSTANCE.keySelectedColor.getValue() : new Color(100, 255, 100, 255);
    }

    public static Color getAccentColor() {
        return INSTANCE != null ? INSTANCE.accentColor.getValue() : new Color(88, 101, 242, 255);
    }
}
