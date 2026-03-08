package com.raven.ravenz.module.events;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.event.impl.input.MouseClickEvent;
import com.raven.ravenz.module.Module;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

public class MouseModuleHandler {

    @EventHandler
    public void onMouseClick(MouseClickEvent event) {
        if (event.action() == GLFW.GLFW_PRESS) {
            int button = event.button();

            for (Module module : RavenZClient.INSTANCE.getModuleManager().getModules()) {
                int moduleKey = module.getKey();

                if (moduleKey == -1 || moduleKey == 0) continue;

                boolean matches = moduleKey == button ||
                        moduleKey == -(button + 1) ||
                        moduleKey == (-100 - button);

                if (matches) {
                    module.toggle();
                    break;
                }
            }
        }
    }
}