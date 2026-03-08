package com.raven.ravenz.module.modules.client;

import com.raven.ravenz.gui.ClickGui;
import com.raven.ravenz.gui.theme.ThemeManager;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public final class ClickGUIModule extends Module {

    // Kept for backward compatibility with SettingsRenderer/ColorPicker that reference this field
    public static final ModeSetting theme = new ModeSetting("Theme", ThemeManager.getDefaultName(),
            ThemeManager.getThemeNamesArray());

    public ClickGUIModule() {
        super("Click Gui", "Toggles the Raven-Z- GUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
        addSettings(theme);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            mc.setScreen(new ClickGui());
        }
        setEnabled(false);
    }
}
