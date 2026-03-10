package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.AttackEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.ModeSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends Module {
    public final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Watchdog Old", "Mospixel");

    public Criticals() {
        super("Criticals", "Makes you hit every crit (BLATANT)", -1, Category.COMBAT);
        this.addSetting(mode);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (isNull()) return;
        boolean willCritLegit = mc.player.fallDistance > 0.0F && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isTouchingWater() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && !mc.player.hasVehicle() && e.getTarget() instanceof LivingEntity;
        if (willCritLegit) return;

        switch (mode.getMode()) {
            case "Vanilla" -> {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getEntityPos().x, mc.player.getEntityPos().y + 0.2, mc.player.getEntityPos().z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getEntityPos().x, mc.player.getEntityPos().y + 0.1, mc.player.getEntityPos().z, false, false));
            }
            case "Watchdog Old" -> {
                if (mc.player.isOnGround()) {
                    mc.player.setPosition(mc.player.getX(), mc.player.getY() + 0.001D, mc.player.getZ());
                }
            }
            case "Mospixel" -> {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getEntityPos().x, mc.player.getEntityPos().y + 0.000000271875, mc.player.getEntityPos().z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getEntityPos().x, mc.player.getEntityPos().y, mc.player.getEntityPos().z, false, false));
            }
        }
    }
}
