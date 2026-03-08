package com.raven.ravenz.module.modules.misc;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * MaceTarget — equips elytra, flies up above the nearest player,
 * then dives and hits them with a mace for fall-damage bonus.
 *
 * Stages:
 *  FLYING_UP  — equips elytra, shoots fireworks to gain height above target
 *  TARGETING  — looks at target, waits until within 16 blocks horizontally
 *  ATTACKING  — dives, switches to mace when in range, attacks, loops
 */
public final class MaceTarget extends Module {

    private final NumberSetting  height     = new NumberSetting("Height",     20, 50, 20, 1);
    private final BooleanSetting autoReloop = new BooleanSetting("Auto Reloop", true);

    private Stage stage         = Stage.FLYING_UP;
    private long  lastFirework  = 0;
    private long  lastReset     = 0;
    private int   waitTicks     = 0;

    public MaceTarget() {
        super("Mace Target", "Flies up with elytra and hits the nearest player with a mace", -1, Category.MISC);
        addSettings(height, autoReloop);
    }

    @Override
    public void onEnable() {
        stage = Stage.FLYING_UP;
        lastFirework = 0;
        lastReset = 0;
        waitTicks = 0;
        super.onEnable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;

        if (waitTicks > 0) { waitTicks--; return; }

        PlayerEntity target = findNearestPlayer();
        if (target == null) return;

        boolean hasElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST)
                .get(DataComponentTypes.GLIDER) != null;
        long now = System.currentTimeMillis();

        switch (stage) {
            case FLYING_UP -> {
                if (!hasElytra) {
                    int slot = findHotbar(Items.ELYTRA);
                    if (slot != -1) { mc.player.getInventory().setSelectedSlot(slot); useMainHand(); waitTicks = 2; }
                }
                if (now - lastFirework > 300) {
                    int slot = findHotbar(Items.FIREWORK_ROCKET);
                    if (slot != -1) { mc.player.getInventory().setSelectedSlot(slot); useMainHand(); }
                    lastFirework = now;
                }
                double heightDiff = mc.player.getY() - target.getY();
                if (heightDiff >= height.getValue()) {
                    stage = Stage.TARGETING;
                } else {
                    // Aim toward target height
                    lookAt(target.getPos().add(0, height.getValue(), 0));
                }
            }
            case TARGETING -> {
                lookAt(target.getPos());
                if (mc.player.distanceTo(target) < 16) stage = Stage.ATTACKING;
            }
            case ATTACKING -> {
                lookAt(target.getPos());

                // Swap back from elytra when close enough
                if (hasElytra && now - lastReset > 200) {
                    for (int i = 0; i < 46; i++) {
                        var stack = mc.player.getInventory().getStack(i);
                        var comp = stack.get(DataComponentTypes.EQUIPPABLE);
                        if (comp != null && comp.slot() == EquipmentSlot.CHEST && !stack.isOf(Items.ELYTRA)) {
                            mc.player.getInventory().setSelectedSlot(i < 9 ? i : 0);
                            useMainHand();
                            break;
                        }
                    }
                    lastReset = now;
                }

                if (now - lastReset > 100 && mc.player.distanceTo(target) < 3) {
                    int maceSlot = findHotbar(Items.MACE);
                    if (maceSlot != -1) {
                        int prev = mc.player.getInventory().getSelectedSlot();
                        if (maceSlot != prev)
                            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
                        mc.interactionManager.attackEntity(mc.player, target);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        if (maceSlot != prev)
                            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prev));

                        if (autoReloop.getValue()) {
                            stage = Stage.FLYING_UP;
                            lastFirework = 0;
                        } else {
                            setEnabled(false);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onInput(HandleInputEvent event) {
        if (isNull()) return;
        if (findNearestPlayer() == null) return;

        boolean gliding = mc.player.isGliding();
        if (gliding) {
            // Alternate jump every 2 ticks to maintain elytra flight
            event.setJump(mc.player.age % 2 == 0);
            if (!mc.player.isGliding()) lastFirework = 0;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private PlayerEntity findNearestPlayer() {
        PlayerEntity closest = null;
        double best = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double d = mc.player.squaredDistanceTo(p);
            if (d < best) { best = d; closest = p; }
        }
        return closest;
    }

    private void lookAt(Vec3d target) {
        double dx = target.x - mc.player.getEyePos().x;
        double dy = target.y - mc.player.getEyePos().y;
        double dz = target.z - mc.player.getEyePos().z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horiz)));
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private void useMainHand() {
        ((com.raven.ravenz.mixin.MinecraftClientAccessor) mc).invokeDoItemUse();
    }

    private int findHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private enum Stage { FLYING_UP, TARGETING, ATTACKING }
}
