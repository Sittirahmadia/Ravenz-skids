package com.raven.ravenz.module.modules.render;

import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ColorSetting;
import com.raven.ravenz.module.setting.ModeSetting;

import java.awt.*;

public class ContainerSlots extends Module {
    public static final ModeSetting fontMode = new ModeSetting("Font", "Inter", "Inter", "MC");
    public static final ColorSetting color = new ColorSetting("Color", new Color(15, 115, 225));
    public static final BooleanSetting disableText = new BooleanSetting("Disable Text", false);

    public static final BooleanSetting highlightTotem = new BooleanSetting("Highlight Totem", false);
    public static final ColorSetting highlightColor = new ColorSetting("Highlight Color", new Color(255, 225, 0));

    public ContainerSlots() {
        super("Container Slots", "Renders container indices", Category.RENDER);
        addSettings(fontMode, color, highlightTotem, highlightColor, disableText);
    }
}