package com.raven.ravenz.utils.mc.crystal;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static com.raven.ravenz.RavenZClient.mc;

public final class RotationUtils {

    private RotationUtils() {}

    /**
     * Returns the eye position of a player entity.
     * For the local player we use getEyePos() directly;
     * for remote players we also use getEyePos() which is correct.
     */
    public static Vec3d getEyesPos(PlayerEntity player) {
        return player.getEyePos();
    }

    public static Vec3d getPlayerLookVec(float yaw, float pitch) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g), i = MathHelper.sin(g);
        float j = MathHelper.cos(f), k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public static Vec3d getPlayerLookVec(PlayerEntity player) {
        return getPlayerLookVec(player.getYaw(), player.getPitch());
    }

    /** Returns {yaw, pitch} as double[2] aiming from entity eyes toward target */
    public static double[] getDirection(PlayerEntity entity, Vec3d target) {
        Vec3d eyes = entity.getEyePos();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = MathHelper.sqrt((float)(dx * dx + dz * dz));
        double yaw   = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        double pitch = -MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dy, dist)));
        return new double[]{yaw, pitch};
    }
}
