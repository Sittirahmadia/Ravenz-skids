package com.raven.ravenz.mixin;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onPress(long window, int action, KeyInput input, CallbackInfo ci) {
        int key = input.getKeycode();
        if (window == this.client.getWindow().getHandle()) {
            if (this.client.currentScreen == null) {
                for (Module module : RavenZClient.INSTANCE.moduleManager.getModules()) {
                    if (key == module.getKey()) {
                        if (module.getKeybindSetting().isHoldMode()) {
                            if (action == GLFW.GLFW_PRESS && !module.isEnabled()) {
                                module.setEnabled(true);
                            } else if (action == GLFW.GLFW_RELEASE && module.isEnabled()) {
                                module.setEnabled(false);
                            }
                        } else {
                            if (action == GLFW.GLFW_PRESS && KeyUtils.isKeyPressed(key)) {
                                module.toggle();
                            }
                        }
                    }
                }
            }
        }
    }
}
