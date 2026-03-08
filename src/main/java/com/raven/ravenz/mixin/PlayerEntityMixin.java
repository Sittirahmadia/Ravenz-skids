package com.raven.ravenz.mixin;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.module.modules.movement.KeepSprint;
import com.raven.ravenz.module.modules.player.FastMine;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void modifyBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        if (RavenZClient.INSTANCE == null) return;

        Optional<FastMine> optionalModule = RavenZClient.INSTANCE.getModuleManager().getModule(FastMine.class);
        if (optionalModule.isEmpty()) return;

        FastMine fastMine = optionalModule.get();
        if (!fastMine.isEnabled()) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player != MinecraftClient.getInstance().player) return;

        float modifiedSpeed = cir.getReturnValue() * fastMine.getSpeed();
        cir.setReturnValue(modifiedSpeed);
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void keepSprintTail(Entity target, CallbackInfo ci) {
        if (RavenZClient.INSTANCE == null) return;

        Optional<KeepSprint> keep = RavenZClient.INSTANCE.getModuleManager().getModule(KeepSprint.class);
        if (keep.isPresent() && keep.get().isEnabled()) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (player == MinecraftClient.getInstance().player) {
                player.setSprinting(true);
            }
        }
    }
}
