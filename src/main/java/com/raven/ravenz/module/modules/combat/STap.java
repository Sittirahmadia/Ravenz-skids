package com.raven.ravenz.module.modules.combat;


import com.raven.ravenz.event.impl.player.AttackEvent;
import com.raven.ravenz.event.impl.player.DoAttackEvent;
import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import com.raven.ravenz.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

public class STap extends Module {
    public final NumberSetting chance = new NumberSetting("Chance (%)", 1, 100, 100, 1);
    private final NumberSetting msDelay = new NumberSetting("Ms", 1, 500, 60, 1);
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only on ground", true);
    boolean wasSprinting;
    TimerUtil timer = new TimerUtil();

    public STap() {
        super("STap", "Makes you automatically STAP", -1, Category.COMBAT);
        this.addSettings(msDelay, chance, onlyOnGround);
    }

    @EventHandler
    private void onAttackEvent(DoAttackEvent event) {
        if (isNull()) return;
        var target = mc.targetedEntity;
        if (target == null) return;
        if (!target.isAlive()) return;
        if (!mc.player.isOnGround() && onlyOnGround.getValue()) return;
        if (Math.random() * 100 > chance.getValueFloat()) return;
        if (!KeyUtils.isKeyPressed(GLFW.GLFW_KEY_W)) return;
        if (mc.player.isSprinting()) {
            wasSprinting = true;
            mc.options.backKey.setPressed(true);
        }
    }


    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;
        if (!KeyUtils.isKeyPressed(GLFW.GLFW_KEY_W)) return;
        if (timer.hasElapsedTime(msDelay.getValueInt(), true)) {
            if (wasSprinting) {
                mc.options.backKey.setPressed(false);
                wasSprinting = false;
            }
        }
    }
}