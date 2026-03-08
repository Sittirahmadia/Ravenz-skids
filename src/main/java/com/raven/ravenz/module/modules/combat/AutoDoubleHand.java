package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoDoubleHand extends Module {

    private final BooleanSetting onPop          = new BooleanSetting("On Pop",            true);
    private final BooleanSetting onHealth        = new BooleanSetting("On Health",         false);
    private final NumberSetting  healthVal       = new NumberSetting("Health",              1, 20, 2, 1);

    private final BooleanSetting predict         = new BooleanSetting("Predict Damage",    true);
    private final BooleanSetting predictCrystals = new BooleanSetting("Predict Crystals",  false);
    private final NumberSetting  predictRadius   = new NumberSetting("Crystal Radius",      1, 12, 6, 0.5);

    private final BooleanSetting checkPlayers    = new BooleanSetting("Check Players",     true);
    private final NumberSetting  playerDistance  = new NumberSetting("Player Distance",     1, 20, 5, 0.1);

    private final BooleanSetting checkShield     = new BooleanSetting("Check Shield",      false);
    private final BooleanSetting onGround        = new BooleanSetting("On Ground",         true);
    private final NumberSetting  activatesAbove  = new NumberSetting("Activates Above",     0, 4, 0.2, 0.1);

    private boolean offhandMissingTotem = false;
    private boolean belowHealth         = false;

    public AutoDoubleHand() {
        super("Auto Double Hand", "Keeps a totem in your offhand slot automatically", -1, Category.COMBAT);
        addSettings(onPop, onHealth, healthVal,
                predict, predictCrystals, predictRadius,
                checkPlayers, playerDistance,
                checkShield, onGround, activatesAbove);
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

        if (checkShield.getValue() && mc.player.isBlocking()) return;

        boolean offhandIsTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

        // On Pop
        if (onPop.getValue()) {
            if (!offhandIsTotem && !offhandMissingTotem) {
                offhandMissingTotem = true;
                swapTotemToOffhand();
                return;
            }
            if (offhandIsTotem) offhandMissingTotem = false;
        }

        // On Health
        if (onHealth.getValue()) {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (hp <= healthVal.getValue() * 2f && !belowHealth) {
                belowHealth = true;
                swapTotemToOffhand();
                return;
            }
            if (hp > healthVal.getValue() * 2f) belowHealth = false;
        }

        // Crystal Prediction
        if (!predict.getValue() || offhandIsTotem) return;
        if (!onGround.getValue() && mc.player.isOnGround()) return;
        if (mc.player.getHealth() > 19) return;

        double above = activatesAbove.getValue();
        if (above > 0) {
            for (int i = 1; i <= (int) Math.ceil(above); i++) {
                if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, -i, 0)).isAir()) return;
            }
        }

        if (checkPlayers.getValue()) {
            double sqDist = playerDistance.getValue() * playerDistance.getValue();
            boolean hasNearby = mc.world.getPlayers().stream()
                    .anyMatch(p -> p != mc.player && mc.player.squaredDistanceTo(p) <= sqDist);
            if (!hasNearby) return;
        }

        double r = predictRadius.getValue();
        Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                new Box(pos.add(-r, -r, -r), pos.add(r, r, r)),
                e -> true)) {
            if (wouldLethal(new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()))) {
                swapTotemToOffhand();
                return;
            }
        }

        if (predictCrystals.getValue()) {
            BlockPos playerBP = mc.player.getBlockPos();
            int ri = (int) Math.ceil(r);
            outer:
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dy = -ri; dy <= ri; dy++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        BlockPos bp = playerBP.add(dx, dy, dz);
                        var block = mc.world.getBlockState(bp).getBlock();
                        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) continue;
                        if (wouldLethal(Vec3d.ofBottomCenter(bp).add(0, 1, 0))) {
                            swapTotemToOffhand();
                            break outer;
                        }
                    }
                }
            }
        }
    }

    private boolean wouldLethal(Vec3d crystalPos) {
        double dist = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ())
                .add(0, mc.player.getEyeHeight(mc.player.getPose()) * 0.5, 0)
                .distanceTo(crystalPos);
        double roughDmg = Math.max(0, 120.0 * Math.pow(Math.max(0, 1.0 - dist / 12.0), 2));
        float playerHP = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return roughDmg >= playerHP;
    }

    private void swapTotemToOffhand() {
        if (mc.player == null) return;
        int slot = findTotem(0, 9);
        if (slot == -1) slot = findTotem(9, 36);
        if (slot == -1) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot, 40, SlotActionType.SWAP, mc.player);
    }

    private int findTotem(int from, int to) {
        for (int i = from; i < to; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }
}
