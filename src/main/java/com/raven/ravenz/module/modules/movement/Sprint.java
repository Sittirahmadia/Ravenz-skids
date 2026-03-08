package com.raven.ravenz.module.modules.movement;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import meteordevelopment.orbit.EventHandler;

public final class Sprint extends Module {

    public Sprint() {
        super("Sprint", "Makes you automatically sprint", -1, Category.MOVEMENT);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;
        if (mc.options.getSprintToggled().getValue()) mc.options.getSprintToggled().setValue(false);

        mc.options.sprintKey.setPressed(true);
    }
}
