package com.raven.ravenz.mixin;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.event.impl.render.Render3DEvent;
import com.raven.ravenz.module.modules.render.AspectRatio;
import com.raven.ravenz.utils.render.W2SUtil;
import com.raven.ravenz.utils.render.font.util.RendererUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @ModifyArgs(method = "getBasicProjectionMatrix", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"))
    private void modifyAspectRatio(Args args) {
        float customRatio = AspectRatio.getAspectRatio();
        if (customRatio > 0) {
            args.set(1, customRatio);
        }
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void renderHand(RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        float tickDelta = tickCounter.getTickProgress(true);
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        var cameraPos = camera.getCameraPos();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        captureProjectionData(mc, matrixStack, tickDelta);

        RavenZClient.INSTANCE.getEventBus().post(new Render3DEvent(matrixStack));
    }

    private static void captureProjectionData(MinecraftClient mc, MatrixStack matrixStack, float tickDelta) {
        Matrix4f projection = invokeGameRendererProjection(mc.gameRenderer, tickDelta);
        if (projection == null) {
            projection = invokeRenderSystemMatrix("getProjectionMatrix");
        }
        if (projection != null) {
            W2SUtil.matrixProject.set(projection);
            RendererUtils.lastProjMat.set(projection);
        }

        Matrix4f modelView = invokeRenderSystemMatrix("getModelViewMatrix");
        if (modelView != null) {
            W2SUtil.matrixModel.set(modelView);
            RendererUtils.lastModMat.set(modelView);
        }

        Matrix4f worldSpace = matrixStack.peek().getPositionMatrix();
        W2SUtil.matrixWorldSpace.set(worldSpace);
        RendererUtils.lastWorldSpaceMatrix.set(worldSpace);
    }

    private static Matrix4f invokeGameRendererProjection(GameRenderer gameRenderer, float tickDelta) {
        try {
            var method = GameRenderer.class.getDeclaredMethod("getProjectionMatrix", float.class);
            method.setAccessible(true);
            Object value = method.invoke(gameRenderer, tickDelta);
            if (value instanceof Matrix4f matrix4f) {
                return new Matrix4f(matrix4f);
            }
        } catch (Throwable ignored) {
        }

        try {
            return new Matrix4f(gameRenderer.getBasicProjectionMatrix(70.0f));
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Matrix4f invokeRenderSystemMatrix(String methodName) {
        try {
            Object value = RenderSystem.class.getMethod(methodName).invoke(null);
            if (value instanceof Matrix4f matrix4f) {
                return new Matrix4f(matrix4f);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }


}
