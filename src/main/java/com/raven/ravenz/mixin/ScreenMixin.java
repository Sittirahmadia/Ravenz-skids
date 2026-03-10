package com.raven.ravenz.mixin;

import com.raven.ravenz.gui.ClickGui;
import com.raven.ravenz.gui.newgui.NewClickGUI;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow
    @Nullable
    protected MinecraftClient client;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderBackgroundInject(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (client == null) return;
        Screen currentScreen = client.currentScreen;
        if (currentScreen instanceof ClickGui) {
            ci.cancel();
            return;
        }
        if (currentScreen instanceof NewClickGUI && !com.raven.ravenz.module.modules.client.ClientSettingsModule.isGuiBlurEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (client == null) return;
        Screen currentScreen = client.currentScreen;
        if (currentScreen instanceof NewClickGUI || currentScreen instanceof ClickGui) {
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(
                    GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE,
                    GL11.GL_ONE_MINUS_SRC_ALPHA
            );
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GL11.GL_LEQUAL);
            GlStateManager._enableCull();
            GlStateManager._disableScissorTest();
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._depthMask(true);
        }
    }
}
