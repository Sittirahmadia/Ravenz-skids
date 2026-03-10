package com.raven.ravenz.module.modules.render;

import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ColorSetting;
import com.raven.ravenz.module.setting.ModeSetting;

import java.awt.*;

public class ContainerSlots extends Module {
    public static ContainerSlots INSTANCE;
    public final ModeSetting fontMode = new ModeSetting("Font", "Inter", "Inter", "MC");
    public final ColorSetting color = new ColorSetting("Color", new Color(15, 115, 225));
    public final BooleanSetting disableText = new BooleanSetting("Disable Text", false);

    public final BooleanSetting highlightTotem = new BooleanSetting("Highlight Totem", false);
    public final ColorSetting highlightColor = new ColorSetting("Highlight Color", new Color(255, 225, 0));

    public ContainerSlots() {
        super("Container Slots", "Renders container indices", Category.RENDER);
        INSTANCE = this;
        addSettings(fontMode, color, highlightTotem, highlightColor, disableText);
    }
}