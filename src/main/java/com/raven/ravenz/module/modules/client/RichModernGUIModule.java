package com.raven.ravenz.module.modules.client;

import com.raven.ravenz.gui.RichModernGUI;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class RichModernGUIModule extends Module {

    public RichModernGUIModule() {
        super("RichModernGUI", "Rich Modern ClickGUI", GLFW.GLFW_KEY_UNKNOWN, Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            mc.setScreen(new RichModernGUI());
        }
        setEnabled(false);
    }

    public static Color getAccentColor() {
        return ClientSettingsModule.getAccentColor();
    }
}
