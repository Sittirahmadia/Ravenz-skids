package com.raven.ravenz.module.modules.client;

import com.raven.ravenz.gui.RavenScreen;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.gui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

public final class ClickGUIModule extends Module {
    public static final ModeSetting theme = new ModeSetting("Theme", ThemeManager.getDefaultName(),
            ThemeManager.getThemeNamesArray());

    public ClickGUIModule() {
        super("Click Gui", "Toggles the Raven-Z- GUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
        addSettings(theme);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            mc.setScreen(new RavenScreen());
        }
        setEnabled(false);
    }
}
