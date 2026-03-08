package com.raven.ravenezplus;

import com.raven.ravenz.gui.RavenScreen;
import com.raven.ravenz.module.modules.combat.SafeAnchor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class RavenZPlusClient implements ClientModInitializer {
    private static KeyBinding openUi;
    private static KeyBinding anchorMacroKey;
    public static final SafeAnchor SAFE_ANCHOR = new SafeAnchor();

    @Override
    public void onInitializeClient() {
        openUi = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.ravenezplus.open_ui",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_SHIFT,
                        KeyBinding.Category.MISC
                )
        );

        anchorMacroKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.ravenezplus.safe_anchor",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_H,
                        KeyBinding.Category.MISC
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openUi.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new RavenScreen());
                }
            }

            while (anchorMacroKey.wasPressed()) {
                SAFE_ANCHOR.executeMacro(client);
            }
        });
    }
}
