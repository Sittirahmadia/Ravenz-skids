package com.raven.ravenz.mixin;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.module.modules.render.SwingSpeed;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    public void getHandSwingDurationInject(CallbackInfoReturnable<Integer> cir) {
        if (RavenZClient.INSTANCE == null || RavenZClient.mc == null) return;

        var optionalModule = RavenZClient.INSTANCE.getModuleManager().getModule(SwingSpeed.class);
        if (optionalModule.isPresent()) {
            SwingSpeed module = optionalModule.get();
            if (module.isEnabled()) {
                cir.setReturnValue(module.getSwingSpeed());
            }
        }
    }
}
