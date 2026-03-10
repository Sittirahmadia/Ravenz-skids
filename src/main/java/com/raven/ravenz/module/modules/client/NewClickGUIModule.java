package com.raven.ravenz.module.modules.client;

import com.raven.ravenz.gui.newgui.NewClickGUI;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class NewClickGUIModule extends Module {

    public NewClickGUIModule() {
        super("NewClickGUI", "Modern NanoVG-based ClickGUI", GLFW.GLFW_KEY_UNKNOWN, Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            mc.setScreen(new NewClickGUI());
        }
        setEnabled(false);
    }

    public static Color getAccentColor() {
        return ClientSettingsModule.getAccentColor();
    }

    public static String getFontStyle() {
        return ClientSettingsModule.getFontStyle();
    }
}
