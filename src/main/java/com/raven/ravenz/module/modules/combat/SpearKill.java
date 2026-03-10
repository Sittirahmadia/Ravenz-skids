package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.module.modules.misc.Teams;
import com.raven.ravenz.utils.friend.FriendManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.stream.StreamSupport;

public final class SpearKill extends Module {

    private final BooleanSetting targetPlayersOnly = new BooleanSetting("Target Players Only", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final NumberSetting lungeStrength = new NumberSetting("Lunge Strength", 0.0, 10.0, 5.0, 0.1);
    private final NumberSetting lockRange = new NumberSetting("Lock Range", 1.0, 256.0, 64.0, 1.0);
    private final NumberSetting chargeTicks = new NumberSetting("Charge Ticks", 1.0, 40.0, 10.0, 1.0);

    private int spearTicks = 0;
    private Entity lockedTarget;

    public SpearKill() {
        super("SpearKill", "Locks and lunges with spear while using it", Category.COMBAT);
        addSettings(targetPlayersOnly, ignoreFriends, lungeStrength, lockRange, chargeTicks);
    }

    @EventHandler
    private void onHandleInput(HandleInputEvent event) {
        if (isNull() || mc.currentScreen != null)
            return;

        if (!mc.options.useKey.isPressed() || !isHoldingSpear()) {
            resetState();
            return;
        }

        spearTicks++;

        if (lockedTarget == null || !lockedTarget.isAlive()) {
            lockedTarget = findTarget();
        }

        if (!(lockedTarget instanceof LivingEntity livingTarget) || !livingTarget.isAlive())
            return;

        if (!isValidTarget(livingTarget)) {
            lockedTarget = null;
            return;
        }

        aimAt(livingTarget);

        if (spearTicks >= chargeTicks.getValueInt()) {
            Vec3d direction = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
            mc.player.setSprinting(true);
            mc.player.setVelocity(direction.multiply(lungeStrength.getValue()));
        }
    }

    private void aimAt(Entity target) {
        Vec3d playerEyePos = mc.player.getEyePos();
        Vec3d targetPos = target.getBoundingBox().getCenter();
        Vec3d toTarget = targetPos.subtract(playerEyePos).normalize();

        float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));

        mc.player.setYaw(MathHelper.wrapDegrees(yaw));
        mc.player.setHeadYaw(MathHelper.wrapDegrees(yaw));
        mc.player.setPitch(MathHelper.clamp(pitch, -89.0f, 89.0f));
    }

    private Entity findTarget() {
        if (mc.targetedEntity instanceof LivingEntity living && isValidTarget(living)) {
            return living;
        }

        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(entity -> entity instanceof LivingEntity)
                .filter(this::isValidTarget)
                .filter(entity -> entity.squaredDistanceTo(mc.player) <= lockRange.getValue() * lockRange.getValue())
                .max((first, second) -> Double.compare(aimPriority(first), aimPriority(second)))
                .orElse(null);
    }

    private double aimPriority(Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f).normalize();
        Vec3d toEntity = entity.getBoundingBox().getCenter().subtract(eyePos).normalize();

        double dot = lookVec.dotProduct(toEntity);
        double distancePenalty = eyePos.squaredDistanceTo(entity.getBoundingBox().getCenter()) * 0.0005;
        return dot - distancePenalty;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity))
            return false;
        if (entity == mc.player || !entity.isAlive())
            return false;
        if (entity.distanceTo(mc.player) > lockRange.getValueFloat())
            return false;

        if (targetPlayersOnly.getValue() && !(entity instanceof PlayerEntity))
            return false;

        if (entity instanceof PlayerEntity playerEntity) {
            if (Teams.isTeammate(playerEntity))
                return false;
            if (ignoreFriends.getValue() && FriendManager.isFriend(playerEntity.getUuid()))
                return false;
        }

        return true;
    }

    private boolean isHoldingSpear() {
        return isSpear(mc.player.getMainHandStack()) || isSpear(mc.player.getOffHandStack());
    }

    private boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return false;
        var id = Registries.ITEM.getId(stack.getItem());
        if (id == null)
            return false;
        String path = id.getPath();
        return path.equals("spear") || path.endsWith("_spear") || path.contains("spear");
    }

    private void resetState() {
        spearTicks = 0;
        lockedTarget = null;
    }

    @Override
    public void onDisable() {
        resetState();
    }
}
