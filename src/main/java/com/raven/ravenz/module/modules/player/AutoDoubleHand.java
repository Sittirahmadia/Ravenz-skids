package com.raven.ravenz.module.modules.player;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.mc.InventoryUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Automatically moves a totem into your selected hotbar slot when:
 *  - Your offhand totem was consumed (On Pop).
 *  - Your health drops below a threshold (On Health).
 *  - A nearby End Crystal could deal lethal damage (Predict Damage).
 */
public final class AutoDoubleHand extends Module {

    private final BooleanSetting onPop = new BooleanSetting("On Pop", false);
    private final BooleanSetting onHealth = new BooleanSetting("On Health", false);
    private final NumberSetting  healthThreshold = new NumberSetting("Health", 1, 20, 6, 1);

    private final BooleanSetting predict     = new BooleanSetting("Predict Damage", true);
    private final NumberSetting  crystalRange = new NumberSetting("Crystal Range", 1, 12, 6, 0.5);
    private final BooleanSetting checkPlayers = new BooleanSetting("Check Players", true);
    private final NumberSetting  playerDist   = new NumberSetting("Player Distance", 1, 20, 8, 0.5);

    private boolean offhandPopped = false;
    private boolean belowHealth   = false;

    public AutoDoubleHand() {
        super("Auto Double Hand", "Moves a totem to your hotbar when you're about to die", -1, Category.PLAYER);
        addSettings(onPop, onHealth, healthThreshold, predict, crystalRange, checkPlayers, playerDist);
    }

    @Override
    public void onEnable() {
        offhandPopped = false;
        belowHealth   = false;
        super.onEnable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        var inventory = mc.player.getInventory();

        // ── On Pop: offhand had a totem, now it doesn't ───────────────────
        if (onPop.getValue()) {
            boolean hasOffhandTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
            if (!hasOffhandTotem && !offhandPopped) {
                offhandPopped = true;
                InventoryUtil.swapToSlot(Items.TOTEM_OF_UNDYING);
            }
            if (hasOffhandTotem) offhandPopped = false;
        }

        // ── On Health: swap when below threshold ──────────────────────────
        if (onHealth.getValue()) {
            boolean low = mc.player.getHealth() <= healthThreshold.getValue() * 2f;
            if (low && !belowHealth) {
                belowHealth = true;
                InventoryUtil.swapToSlot(Items.TOTEM_OF_UNDYING);
            }
            if (!low) belowHealth = false;
        }

        // ── Predict Damage: check nearby crystals ─────────────────────────
        if (!predict.getValue()) return;
        if (mc.player.getHealth() > 19f) return; // already near full — skip

        if (checkPlayers.getValue()) {
            double maxDist = playerDist.getValue();
            boolean nearPlayer = mc.world.getPlayers().stream()
                    .filter(p -> p != mc.player)
                    .anyMatch(p -> mc.player.squaredDistanceTo(p) <= maxDist * maxDist);
            if (!nearPlayer) return;
        }

        List<EndCrystalEntity> crystals = nearbyCrystals();
        for (EndCrystalEntity crystal : crystals) {
            double damage = estimateCrystalDamage(new net.minecraft.util.math.Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()));
            if (damage >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                InventoryUtil.swapToSlot(Items.TOTEM_OF_UNDYING);
                break;
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private List<EndCrystalEntity> nearbyCrystals() {
        double r = crystalRange.getValue();
        Vec3d pos = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        return mc.world.getEntitiesByClass(EndCrystalEntity.class,
                new Box(pos.subtract(r, r, r), pos.add(r, r, r)),
                e -> true);
    }

    /**
     * Simplified crystal damage estimate.
     * Real formula is complex; this gives a reasonable approximation for the
     * distance-based explosion damage without full raycasting.
     */
    private double estimateCrystalDamage(Vec3d crystalPos) {
        double dist = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(crystalPos);
        double maxDamage = 97.0; // approximate max end crystal explosion damage
        double radius = 12.0;
        if (dist > radius) return 0;
        double impact = (1.0 - dist / radius) * maxDamage;
        // Factor in armor (very rough)
        double armorFactor = 1.0 - (mc.player.getArmor() * 0.04 * 0.75);
        return impact * armorFactor;
    }
}
