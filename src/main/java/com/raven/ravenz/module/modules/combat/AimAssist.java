package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssist extends Module {

    private final ModeSetting aimAt = new ModeSetting("Aim At", "Head", "Head", "Chest", "Legs");
    private final BooleanSetting lookAtCorner = new BooleanSetting("Look At Corner", false);
    private final BooleanSetting yawAssist = new BooleanSetting("Horizontal", true);
    private final NumberSetting yawSpeed = new NumberSetting("Horizontal Speed", 0.1, 10.0, 1.0, 0.1);
    private final BooleanSetting pitchAssist = new BooleanSetting("Vertical", true);
    private final NumberSetting pitchSpeed = new NumberSetting("Vertical Speed", 0.1, 10.0, 0.5, 0.1);
    private final NumberSetting distance = new NumberSetting("Distance", 3.0, 10.0, 6.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 1.0, 360.0, 180.0, 1.0);
    private final BooleanSetting seeOnly = new BooleanSetting("See Only", true);

    public AimAssist() {
        super("Aim Assist", "Automatically aims at the nearest player", -1, Category.COMBAT);
        addSettings(aimAt, lookAtCorner, yawAssist, yawSpeed, pitchAssist, pitchSpeed, distance, fov, seeOnly);
    }

    @EventHandler
    private void onHandleInput(HandleInputEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        PlayerEntity target = findNearestPlayer();
        if (target == null) return;

        Vec3d targetPos = getAimPos(target);

        if (lookAtCorner.getValue()) {
            double ox = mc.player.getX() - target.getX() > 0 ? 0.29 : -0.29;
            double oz = mc.player.getZ() - target.getZ() > 0 ? 0.29 : -0.29;
            targetPos = targetPos.add(ox, 0, oz);
        }

        double[] dir = calcDirection(targetPos);
        float targetYaw   = (float) dir[0];
        float targetPitch = (float) dir[1];

        if (angleTo(targetYaw, targetPitch) > fov.getValue() / 2.0) return;

        float yawStr   = yawSpeed.getValueFloat()   / 50.0f;
        float pitchStr = pitchSpeed.getValueFloat()  / 50.0f;

        float newYaw   = MathHelper.lerpAngleDegrees(yawStr,   mc.player.getYaw(),   targetYaw);
        float newPitch = MathHelper.lerpAngleDegrees(pitchStr, mc.player.getPitch(), targetPitch);

        if (yawAssist.getValue())   mc.player.setYaw(newYaw);
        if (pitchAssist.getValue()) mc.player.setPitch(MathHelper.clamp(newPitch, -90f, 90f));
    }

    private PlayerEntity findNearestPlayer() {
        PlayerEntity nearest = null;
        double best = distance.getValue();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            double d = mc.player.distanceTo(p);
            if (d > best) continue;
            if (seeOnly.getValue() && !mc.player.canSee(p)) continue;
            best = d;
            nearest = p;
        }
        return nearest;
    }

    private Vec3d getAimPos(PlayerEntity target) {
        return switch (aimAt.getMode()) {
            case "Chest" -> new net.minecraft.util.math.Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
            case "Legs"  -> new net.minecraft.util.math.Vec3d(target.getX(), target.getY() + 0.1, target.getZ());
            default      -> new net.minecraft.util.math.Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
        };
    }

    private double[] calcDirection(Vec3d targetPos) {
        Vec3d eyes = mc.player.getEyePos();
        double dx = targetPos.x - eyes.x;
        double dy = targetPos.y - eyes.y;
        double dz = targetPos.z - eyes.z;
        double hLen = Math.sqrt(dx * dx + dz * dz);
        return new double[]{
            Math.toDegrees(Math.atan2(-dx, dz)),
            Math.toDegrees(-Math.atan2(dy, hLen))
        };
    }

    private double angleTo(float targetYaw, float targetPitch) {
        float dYaw   = Math.abs(MathHelper.wrapDegrees(targetYaw   - mc.player.getYaw()));
        float dPitch = Math.abs(targetPitch - mc.player.getPitch());
        return Math.sqrt(dYaw * dYaw + dPitch * dPitch);
    }
}
