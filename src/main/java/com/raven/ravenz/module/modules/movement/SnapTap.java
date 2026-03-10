package com.raven.ravenz.module.modules.movement;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import meteordevelopment.orbit.EventHandler;

public final class SnapTap extends Module {

    public static long LEFT_STRAFE_LAST_PRESS_TIME = 0;
    public static long RIGHT_STRAFE_LAST_PRESS_TIME = 0;

    public static long FORWARD_STRAFE_LAST_PRESS_TIME = 0;
    public static long BACKWARD_STRAFE_LAST_PRESS_TIME = 0;


    public SnapTap() {
        super("Snap Tap", "Prioritizes the last pressed movement key like Wooting keyboards", -1, Category.MOVEMENT);
        this.addSettings();
    }
}
