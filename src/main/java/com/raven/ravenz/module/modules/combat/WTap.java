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

public class WTap extends Module {
    public final NumberSetting chance = new NumberSetting("Chance (%)", 1, 100, 100, 1);
    private final NumberSetting msDelay = new NumberSetting("Ms", 1, 500, 60, 1);
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only on ground", true);
    boolean wasSprinting;
    TimerUtil timer = new TimerUtil();

    public WTap() {
        super("WTap", "Makes you automatically WTAP", -1, Category.COMBAT);
        this.addSettings(msDelay, chance, onlyOnGround);
    }

    @EventHandler
    private void onAttackEvent(DoAttackEvent event) {
        if (isNull()) return;
        if (Math.random() * 100 > chance.getValueFloat()) return;
        var target = mc.targetedEntity;
        if (!mc.player.isOnGround() && onlyOnGround.getValue()) return;
        if (target == null) return;
        if (!target.isAlive()) return;
        if (!KeyUtils.isKeyPressed(GLFW.GLFW_KEY_W)) return;
        if (mc.player.isSprinting()) {
            wasSprinting = true;
            mc.options.forwardKey.setPressed(false);
        }
    }


    @Override
    public void onEnable() {
        wasSprinting = false;
        timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        // Re-enable forward key if we stopped sprinting mid-wtap
        if (wasSprinting) {
            mc.options.forwardKey.setPressed(true);
        }
        wasSprinting = false;
        super.onDisable();
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;
        if (!KeyUtils.isKeyPressed(GLFW.GLFW_KEY_W)) return;

        if (wasSprinting) {
            if (timer.hasElapsedTime(msDelay.getValueInt(), true)) {
                mc.options.forwardKey.setPressed(true);
                wasSprinting = false;
            }
        }
    }
}
