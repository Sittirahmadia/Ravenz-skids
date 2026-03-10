package com.raven.ravenz.module.modules.render;

import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.NumberSetting;

public final class SwingSpeed extends Module {
    public final NumberSetting swingSpeed = new NumberSetting("Swing Speed", 1, 20, 12, 1);

    public SwingSpeed() {
        super("Swing Speed", "Modifies your swing speed", -1, Category.RENDER);
        this.addSetting(swingSpeed);
    }

    public int getSwingSpeed() {
        return swingSpeed.getValueInt();
    }
}
