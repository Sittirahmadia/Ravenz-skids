package com.raven.ravenz.mixin;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.event.impl.network.DisconnectEvent;
import com.raven.ravenz.event.impl.player.DoAttackEvent;
import com.raven.ravenz.event.impl.player.ItemUseEvent;
import com.raven.ravenz.utils.IMinecraft;
import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.event.impl.world.WorldChangeEvent;
import com.raven.ravenz.gui.ClickGui;
import com.raven.ravenz.module.modules.client.ClickGUIModule;
import com.raven.ravenz.module.modules.client.Client;
import com.raven.ravenz.profiles.ProfileManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements IMinecraft {

    @Shadow
    public ClientWorld world;
    @Shadow
    public HitResult crosshairTarget;
    @Shadow
    public ClientPlayerEntity player;
    @Shadow
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    @Final
    private RenderTickCounter.Dynamic renderTickCounter;

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void setTitle(CallbackInfoReturnable<String> cir) {
        if (RavenZClient.INSTANCE == null || RavenZClient.mc == null) return;

        var optionalClientModule = RavenZClient.INSTANCE.getModuleManager().getModule(Client.class);
        if (optionalClientModule.isPresent()) {
            Client client = optionalClientModule.get();
            if (client.isEnabled() && client.getTitle()) {
                cir.setReturnValue("RidhoXNoqwd 1.21.11");
            }
        }
    }

    @Inject(method = "run", at = @At("HEAD"))
    public void runInject(CallbackInfo ci) {
        if (RavenZClient.INSTANCE != null) {
            ProfileManager profileManager = RavenZClient.INSTANCE.getProfileManager();
            profileManager.loadProfile("default");
        }
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    public void ravenZHandleInputEvents(CallbackInfo ci) {
        if (RavenZClient.INSTANCE != null) {
            HandleInputEvent event = new HandleInputEvent();
            RavenZClient.INSTANCE.getEventBus().post(event);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (RavenZClient.INSTANCE == null || RavenZClient.mc == null) return;

        if (world != null) {
            RavenZClient.INSTANCE.getEventBus().post(new TickEvent());
        }

    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    public final void doAttackInject(CallbackInfoReturnable<Boolean> cir) {
        DoAttackEvent event = new DoAttackEvent();
        RavenZClient.INSTANCE.getEventBus().post(event);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stopInject(CallbackInfo ci) {
        if (RavenZClient.INSTANCE != null) {
            ProfileManager profileManager = RavenZClient.INSTANCE.getProfileManager();
            profileManager.saveProfile("default", true);
        }
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    public void onWorldChangeInject(ClientWorld newWorld, boolean worldLoading, CallbackInfo ci) {
        if (RavenZClient.INSTANCE != null && RavenZClient.mc != null) {
            RavenZClient.INSTANCE.getEventBus().post(new WorldChangeEvent(newWorld));
        }
    }
    @Inject(method = "onDisconnected", at = @At("HEAD"))
    public final void onDisconnected(CallbackInfo ci) {
        DisconnectEvent event = new DisconnectEvent();
        RavenZClient.INSTANCE.getEventBus().post(event);
    }
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    public final void doItemUseInject(CallbackInfo ci) {
        ItemUseEvent event = new ItemUseEvent();

        RavenZClient.INSTANCE.getEventBus().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
