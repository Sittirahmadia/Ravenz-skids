package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.AttackEvent;
import com.raven.ravenz.event.impl.player.ItemUseEvent;
import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.simulation.ClickSimulator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AutoCrystal extends Module {

    // Place settings
    private final NumberSetting  placeDelay      = new NumberSetting("Place Delay",        0, 20,  0,   1);
    private final NumberSetting  placeChance     = new NumberSetting("Place Chance",        0, 100, 100, 1);

    // Break settings
    private final NumberSetting  breakDelay      = new NumberSetting("Break Delay",         0, 20,  0,   1);
    private final NumberSetting  breakChance     = new NumberSetting("Break Chance",         0, 100, 100, 1);

    // Behaviour settings
    private final BooleanSetting onRmb           = new BooleanSetting("On RMB",             true);
    private final BooleanSetting fastMode        = new BooleanSetting("Fast Mode",           true);
    private final BooleanSetting clickSimulation = new BooleanSetting("Click Simulation",    false);
    private final BooleanSetting noCountGlitch   = new BooleanSetting("No Count Glitch",     true);
    private final BooleanSetting noBounce        = new BooleanSetting("No Bounce",           true);
    private final BooleanSetting autoSwitch      = new BooleanSetting("Auto Switch",         false);
    private final BooleanSetting requireObsidian = new BooleanSetting("Require Obsidian",    true);
    private final BooleanSetting stopOnKill      = new BooleanSetting("Stop on Kill",         true);

    private int placeClock = 0;
    private int breakClock = 0;
    private EndCrystalEntity lastBrokenCrystal = null;

    // UUID → last known health; used by Stop on Kill
    private final Map<UUID, Float> lastHealthMap = new HashMap<>();

    public AutoCrystal() {
        super("Auto Crystal", "Automatically places and breaks end crystals", -1, Category.COMBAT);
        addSettings(placeDelay, placeChance, breakDelay, breakChance,
                onRmb, fastMode, clickSimulation, noCountGlitch, noBounce,
                autoSwitch, requireObsidian, stopOnKill);
    }

    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        if (onRmb.getValue() &&
                GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
            return;

        // Stop on Kill — disable module the moment an enemy player dies
        if (stopOnKill.getValue() && checkForKill()) {
            this.toggle();
            return;
        }

        if (autoSwitch.getValue() && !mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) {
                    mc.player.getInventory().selectedSlot = i;
                    break;
                }
            }
        }

        if (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
            placeCrystal();
            breakCrystal();
        }

        placeClock++;
        breakClock++;
    }

    // ── Stop on Kill logic ────────────────────────────────────────────────────

    /**
     * Returns true if any enemy PlayerEntity died this tick.
     * Tracks HP via lastHealthMap (UUID → float).
     */
    private boolean checkForKill() {
        boolean killed = false;
        Set<UUID> aliveNow = new HashSet<>();

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (entity == mc.player) continue;

            UUID id = player.getUuid();
            float hp = player.getHealth();
            aliveNow.add(id);

            if (lastHealthMap.containsKey(id)) {
                if (lastHealthMap.get(id) > 0f && hp <= 0f) killed = true;
            }

            lastHealthMap.put(id, hp);
        }

        // Clean up entries for players no longer in the world
        lastHealthMap.keySet().removeIf(id -> !aliveNow.contains(id));

        return killed;
    }

    // ── Crystal logic (unchanged) ─────────────────────────────────────────────

    private void placeCrystal() {
        if (placeClock < placeDelay.getValueInt()) return;
        if (Math.random() * 100 > placeChance.getValue()) return;

        var crosshair = mc.crosshairTarget;

        if (crosshair instanceof BlockHitResult blockHit && blockHit.getType() != HitResult.Type.MISS) {
            BlockPos pos = blockHit.getBlockPos();
            var state = mc.world.getBlockState(pos);

            boolean validBlock = !requireObsidian.getValue()
                    || state.isOf(Blocks.OBSIDIAN)
                    || state.isOf(Blocks.BEDROCK);

            if (validBlock && canPlaceCrystal(pos)) {
                doPlace(blockHit);
                placeClock = 0;
            }

        } else if (fastMode.getValue() && crosshair instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof EndCrystalEntity crystal) {
            tryFastPlace(crystal);
        }
    }

    private void tryFastPlace(EndCrystalEntity crystal) {
        BlockPos below = crystal.getBlockPos().down();
        var state = mc.world.getBlockState(below);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) return;
        if (!canPlaceCrystal(below)) return;

        var blockHit = new BlockHitResult(
                new net.minecraft.util.math.Vec3d(below.getX() + 0.5, below.getY() + 1.0, below.getZ() + 0.5),
                net.minecraft.util.math.Direction.UP, below, false);
        doPlace(blockHit);
        placeClock = 0;
    }

    private void doPlace(BlockHitResult blockHit) {
        if (clickSimulation.getValue()) ClickSimulator.leftClick();
        ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
        if (res.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void breakCrystal() {
        if (breakClock < breakDelay.getValueInt()) return;
        if (Math.random() * 100 > breakChance.getValue()) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof EndCrystalEntity crystal)) return;
        if (crystal == lastBrokenCrystal) return;
        doBreak(crystal);
    }

    private void doBreak(EndCrystalEntity crystal) {
        if (clickSimulation.getValue()) ClickSimulator.leftClick();
        mc.interactionManager.attackEntity(mc.player, crystal);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastBrokenCrystal = crystal;
        breakClock = 0;
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        BlockPos above  = pos.up();
        BlockPos above2 = pos.up(2);
        if (!mc.world.getBlockState(above).isAir())  return false;
        if (!mc.world.getBlockState(above2).isAir()) return false;
        var box = new net.minecraft.util.math.Box(
                above.getX(), above.getY(), above.getZ(),
                above.getX() + 1, above.getY() + 2, above.getZ() + 1);
        return mc.world.getOtherEntities(null, box).isEmpty();
    }

    private void reset() {
        placeClock = placeDelay.getValueInt();
        breakClock = breakDelay.getValueInt();
        lastBrokenCrystal = null;
        lastHealthMap.clear();
    }

    @EventHandler
    private void onItemUse(ItemUseEvent event) {
        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;
        if (noCountGlitch.getValue() &&
                GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) {
            event.cancel();
        }
    }

    @EventHandler
    private void onAttack(AttackEvent event) {
        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;
        if (noBounce.getValue() &&
                GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            event.cancel();
        }
    }
}
