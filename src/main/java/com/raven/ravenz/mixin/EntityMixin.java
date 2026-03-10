package com.raven.ravenz.mixin;

import com.raven.ravenz.module.modules.combat.Hitboxes;
import com.raven.ravenz.module.modules.combat.HitboxHelper;
import com.raven.ravenz.module.modules.render.OutlineESP;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract BlockPos getLandingPos();
    @Shadow public abstract boolean isOnGround();
    @Shadow public abstract World getEntityWorld();
    @Shadow protected abstract void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition);
    @Shadow public abstract EntityDimensions getDimensions(net.minecraft.entity.EntityPose pose);

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        OutlineESP outlineESP = OutlineESP.getInstance();
        if (outlineESP != null && outlineESP.isEnabled()) {
            Entity self = (Entity) (Object) this;
            if (outlineESP.shouldEntityGlow(self)) {
                cir.setReturnValue(true);
            }
        }
       
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"))
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
      
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isRemoved()Z"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (getEntityWorld().isClient()) {
            BlockPos blockPos = getLandingPos();
            BlockState blockState = getEntityWorld().getBlockState(blockPos);
            fall(movement.y, isOnGround(), blockState, blockPos);
        }
    }

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<Box> cir) {
        if (!HitboxHelper.isRaycasting()) return;

        Entity self = (Entity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || self == mc.player) return;
        if (!(self instanceof PlayerEntity)) return;

        Hitboxes hitboxes = Hitboxes.getInstance();
        if (hitboxes == null || !hitboxes.isEnabled()) return;

        double expansion = hitboxes.getExpansionFor(self);
        if (expansion <= 0) return;

        Box box = cir.getReturnValue();
        cir.setReturnValue(hitboxes.isBlatant()
                ? box.expand(expansion)
                : box.expand(expansion, 0, expansion));
    }
}
