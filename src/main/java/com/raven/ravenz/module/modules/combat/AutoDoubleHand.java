package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Auto Double Hand — keeps a totem of undying in the offhand slot.
 *
 * On Pop:           re-equips offhand when it empties after totem usage.
 * On Health:        equips immediately when HP drops below the threshold.
 * Predict:          equips when a nearby crystal could one-shot the player.
 * Predict Crystals: also considers potential crystal positions on nearby obsidian.
 * Check Players:    only trigger prediction when hostile players are nearby.
 * Activates Above:  only predict when airborne above the configured height.
 */
public class AutoDoubleHand extends Module {

    private final BooleanSetting onPop    = new BooleanSetting("On Pop",    true);
    private final BooleanSetting onHealth = new BooleanSetting("On Health", false);
    private final NumberSetting  healthVal = new NumberSetting("Health",     1, 20, 6, 1);

    private final BooleanSetting predict         = new BooleanSetting("Predict Crystal",    false);
    private final NumberSetting  predictRadius   = new NumberSetting("Crystal Radius",       1, 12, 6, 0.5);
    private final BooleanSetting predictCrystals = new BooleanSetting("Predict Placements", false);

    private final BooleanSetting checkPlayers    = new BooleanSetting("Check Players",       true);
    private final NumberSetting  playerDistance  = new NumberSetting("Player Distance",      1, 20, 8, 0.5);

    private final NumberSetting  activatesAbove  = new NumberSetting("Activates Above",      0, 4, 0, 0.1);

    private boolean offhandMissingTotem = false;
    private boolean belowHealth         = false;

    public AutoDoubleHand() {
        super("Auto Double Hand", "Keeps a totem in your offhand slot automatically", -1, Category.COMBAT);
        addSettings(onPop, onHealth, healthVal, predict, predictRadius, predictCrystals,
                checkPlayers, playerDistance, activatesAbove);
    }

    @Override
    public void onEnable() {
        offhandMissingTotem = false;
        belowHealth = false;
        super.onEnable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;

        boolean offhandIsTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

        // ── On Pop detection ──────────────────────────────────────────────
        if (onPop.getValue()) {
            if (!offhandIsTotem && !offhandMissingTotem) {
                offhandMissingTotem = true;
                swapTotemToOffhand();
                return;
            }
            if (offhandIsTotem) offhandMissingTotem = false;
        }

        // ── Health trigger ────────────────────────────────────────────────
        if (onHealth.getValue()) {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (hp <= healthVal.getValue() * 2f && !belowHealth) {
                belowHealth = true;
                swapTotemToOffhand();
                return;
            }
            if (hp > healthVal.getValue() * 2f) belowHealth = false;
        }

        // ── Crystal prediction ────────────────────────────────────────────
        if (!predict.getValue() || offhandIsTotem) return;

        // Height gate: only predict when airborne above threshold
        double above = activatesAbove.getValue();
        if (above > 0) {
            // Check that the blocks below the player are air
            for (int i = 1; i <= (int) Math.ceil(above); i++) {
                if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, -i, 0)).isAir())
                    return;
            }
        }

        // Player gate: only predict when an enemy is nearby
        if (checkPlayers.getValue()) {
            double sqDist = playerDistance.getValue() * playerDistance.getValue();
            boolean hasNearbyPlayer = mc.world.getPlayers().stream()
                    .anyMatch(p -> p != mc.player && mc.player.squaredDistanceTo(p) <= sqDist);
            if (!hasNearbyPlayer) return;
        }

        double r = predictRadius.getValue();
        Vec3d pos = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        // Check existing end crystals
        var crystals = mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                new Box(pos.add(-r, -r, -r), pos.add(r, r, r)),
                e -> true);

        for (EndCrystalEntity crystal : crystals) {
            if (wouldLethal(new net.minecraft.util.math.Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()))) { swapTotemToOffhand(); return; }
        }

        // Predict future crystal positions on nearby obsidian/bedrock
        if (predictCrystals.getValue()) {
            BlockPos playerBP = mc.player.getBlockPos();
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dy = -ri; dy <= ri; dy++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        BlockPos bp = playerBP.add(dx, dy, dz);
                        var block = mc.world.getBlockState(bp).getBlock();
                        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) continue;
                        // Crystal would sit one block above
                        Vec3d crystalPos = Vec3d.ofBottomCenter(bp).add(0, 1, 0);
                        if (wouldLethal(crystalPos)) { swapTotemToOffhand(); return; }
                    }
                }
            }
        }
    }

    /** True if a crystal at {@code crystalPos} would be lethal to the local player. */
    private boolean wouldLethal(Vec3d crystalPos) {
        double dist = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(0, mc.player.getEyeHeight(mc.player.getPose()) * 0.5, 0)
                .distanceTo(crystalPos);
        // Rough estimate: max damage ~120 at distance 0, drops off over ~12 blocks
        double roughDmg = Math.max(0, 120.0 * Math.pow(Math.max(0, 1.0 - dist / 12.0), 2));
        float playerHP = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return roughDmg >= playerHP;
    }

    /**
     * Looks for a totem in the full inventory (hotbar first, then main inventory)
     * and swaps it to the offhand using slot action 40.
     */
    private void swapTotemToOffhand() {
        if (mc.player == null) return;

        // Search hotbar first (slots 0–8)
        int totemSlot = findTotem(0, 9);
        // Fall back to main inventory (slots 9–35)
        if (totemSlot == -1) totemSlot = findTotem(9, 36);
        if (totemSlot == -1) return;

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                totemSlot,
                40, // offhand swap
                SlotActionType.SWAP,
                mc.player);
    }

    private int findTotem(int from, int to) {
        for (int i = from; i < to; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING))
                return i;
        }
        return -1;
    }
}
