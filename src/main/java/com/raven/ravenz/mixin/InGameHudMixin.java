package com.raven.ravenz.mixin;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.event.impl.render.Render2DEvent;
import com.raven.ravenz.utils.render.RenderUtils;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At(value = "TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        RenderUtils.unscaledProjection();
        RenderUtils.scaledProjection();

        GlStateManager._disableDepthTest();
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        GlStateManager._disableCull();
        
        if (com.raven.ravenz.utils.render.nanovg.NanoVGRenderer.beginFrame(context)) {
            RavenZClient.INSTANCE.getEventBus()
                    .post(new Render2DEvent(context, context.getScaledWindowWidth(), context.getScaledWindowHeight()));

            com.raven.ravenz.utils.render.nanovg.NanoVGRenderer.endFrame();
        }

        GlStateManager._enableDepthTest();
        GlStateManager._enableCull();
    }
}
