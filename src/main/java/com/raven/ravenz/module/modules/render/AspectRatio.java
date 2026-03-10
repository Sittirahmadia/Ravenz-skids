package com.raven.ravenz.module.modules.render;

import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.NumberSetting;

public class AspectRatio extends Module {

    private static AspectRatio INSTANCE;

    private final NumberSetting aspectRatio = new NumberSetting("Ratio", 0.5, 3.0, 1.78, 0.01);

    public AspectRatio() {
        super("Aspect Ratio", "Changes the game's aspect ratio", Category.RENDER);
        this.addSettings(aspectRatio);
        INSTANCE = this;
    }

    public static AspectRatio getInstance() {
        return INSTANCE;
    }

    public static float getAspectRatio() {
        if (INSTANCE != null && INSTANCE.isEnabled()) {
            return (float) INSTANCE.aspectRatio.getValue();
        }
        return -1;
    }

    public static boolean isAspectRatioEnabled() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}
