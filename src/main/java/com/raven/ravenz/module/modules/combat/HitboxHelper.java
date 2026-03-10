package com.raven.ravenz.module.modules.combat;

/**
 * Static raycasting state tracker used by ProjectileUtilMixin and EntityMixin
 * to know when hitbox expansion should be active.
 */
public final class HitboxHelper {

    private static volatile boolean raycasting = false;

    private HitboxHelper() {}

    public static boolean isRaycasting() {
        return raycasting;
    }

    public static void setRaycasting(boolean value) {
        raycasting = value;
    }
}
