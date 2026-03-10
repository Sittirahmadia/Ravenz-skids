package com.raven.ravenz.module.modules.combat;


import com.raven.ravenz.event.impl.network.PacketEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.mc.ChatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public final class Velocity extends Module {
    public final NumberSetting chance = new NumberSetting("Chance (%)", 1, 100, 100, 1);
    public final BooleanSetting ignoreWhenBackwards = new BooleanSetting("Ignore S press", true);
    public final BooleanSetting ignoreOnFire = new BooleanSetting("Ignore on fire", true);

    public Velocity() {
        super("Velocity", "Automatically jump resets to reduce your velocity", -1, Category.COMBAT);
        this.addSettings(chance, ignoreWhenBackwards, ignoreOnFire);
    }

    @EventHandler
    private void onPacketEvent(PacketEvent event) {
        if (isNull()) return;

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
            if (chanceCheck() && mc.player.isOnGround()) {
                if (ignoreWhenBackwards.getValue() && mc.options.backKey.isPressed()) return;
                if (ignoreOnFire.getValue() && mc.player.isOnFire()) return;
                if (mc.currentScreen != null) return;
                mc.player.jump();
            }
        }
    }

    private boolean chanceCheck() {
        return (Math.random() * 100 < chance.getValueFloat());
    }
}
