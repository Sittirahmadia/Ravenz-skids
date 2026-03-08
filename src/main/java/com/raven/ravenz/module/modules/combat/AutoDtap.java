package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.event.impl.player.AttackEvent;
import com.raven.ravenz.event.impl.player.ItemUseEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.simulation.ClickSimulator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * AutoDtap — crosshair-based double-pop crystal placer.
 *
 * Watches mc.crosshairTarget each tick:
 *   - EntityHitResult → break the crystal (doBreak)
 *   - BlockHitResult  → place a crystal (doPlace)
 *
 * Place validation uses an inline canPlace() that checks:
 *   - block is obsidian or bedrock (when requireObsidian is on)
 *   - the block above the placement face is air (and the one above that)
 *   - player is within 4.5 blocks of the placement pos
 *
 * This mirrors KeyCrystal's proven placement logic to avoid any
 * "canPlaceCrystalServer" || bug found in other clients.
 */
public final class AutoDtap extends Module {

    // ── Settings ───────────────────────────────────────────────────────────

    private final NumberSetting placeDelay = new NumberSetting(
            "Place Delay (ticks)", 0, 20, 0, 1);

    private final NumberSetting breakDelay = new NumberSetting(
            "Break Delay (ticks)", 0, 20, 0, 1);

    private final NumberSetting maxCrystals = new NumberSetting(
            "Max Crystals", 1, 6, 2, 1);

    private final BooleanSetting requireObsidian = new BooleanSetting(
            "Require Obsidian", true);

    private final BooleanSetting simulateClick = new BooleanSetting(
            "Simulate Click", true);

    private final BooleanSetting noCountGlitch = new BooleanSetting(
            "No Count Glitch", true);

    private final BooleanSetting noBounce = new BooleanSetting(
            "No Bounce", true);

    private final BooleanSetting activateOnRightClick = new BooleanSetting(
            "Activate On RightClick", false);

    private final BooleanSetting stopOnKill = new BooleanSetting(
            "Stop On Kill", false);

    // ── State ──────────────────────────────────────────────────────────────

    private int placeClock = 0;
    private int breakClock = 0;
    private int placedCount = 0;
    private EndCrystalEntity lastBrokenCrystal = null;

    // ── Constructor ────────────────────────────────────────────────────────

    public AutoDtap() {
        super("Auto Dtap", "Crosshair-based double-pop crystal placer", -1, Category.COMBAT);
        addSettings(
                placeDelay, breakDelay, maxCrystals,
                requireObsidian, simulateClick, noCountGlitch, noBounce,
                activateOnRightClick, stopOnKill
        );
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }

    private void reset() {
        placeClock        = 0;
        breakClock        = 0;
        placedCount       = 0;
        lastBrokenCrystal = null;
    }

    // ── Place / Break ──────────────────────────────────────────────────────

    /**
     * Breaks the crystal under the crosshair.
     * Mirrors KeyCrystal: optionally left-click simulate then invokeDoAttack.
     */
    private void doBreak(EndCrystalEntity crystal) {
        if (simulateClick.getValue()) ClickSimulator.leftClick();
        ((MinecraftClientAccessor) mc).invokeDoAttack();
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        if (placedCount > 0) placedCount--;
        lastBrokenCrystal = crystal;
        breakClock = 0;
    }

    /**
     * Places a crystal at the block the crosshair is aimed at.
     * Mirrors KeyCrystal: invokeDoItemUse().
     */
    private void doPlace() {
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        placedCount++;
        placeClock = 0;
    }

    /**
     * Validates whether a crystal can be placed at the crosshair block hit.
     *
     * targetBlock  = the block the crosshair ray hit (must be obsidian/bedrock).
     * placementPos = the block on the face side (must be air, with air above it).
     */
    private boolean canPlace(BlockHitResult hit) {
        BlockPos targetBlock   = hit.getBlockPos();
        BlockPos placementPos  = targetBlock.offset(hit.getSide());

        // Base block must be obsidian or bedrock (when setting is on)
        if (requireObsidian.getValue()) {
            var block = mc.world.getBlockState(targetBlock).getBlock();
            if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) return false;
        }

        // Placement column must be in range
        if (mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(placementPos)) > 4.5) return false;

        // Placement and the block above must both be air
        if (!mc.world.getBlockState(placementPos).isAir()) return false;
        if (!mc.world.getBlockState(placementPos.up()).isAir()) return false;

        // Must not overlap the player
        BlockPos playerPos = mc.player.getBlockPos();
        return !placementPos.equals(playerPos)
                && !placementPos.equals(playerPos.up())
                && !placementPos.up().equals(playerPos)
                && !placementPos.up().equals(playerPos.up());
    }

    // ── Tick ───────────────────────────────────────────────────────────────

    @EventHandler
    private void onHandleInput(HandleInputEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        // Right-click gate
        if (activateOnRightClick.getValue()
                && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
            return;

        // Must be holding an end crystal
        if (!mc.player.isHolding(Items.END_CRYSTAL)) return;

        // Stop on nearby kill
        if (stopOnKill.getValue() && isDeadPlayerNearby()) return;

        placeClock++;
        breakClock++;

        // ── Break ──────────────────────────────────────────────────────────
        if (breakClock >= breakDelay.getValueInt()
                && mc.crosshairTarget instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof EndCrystalEntity crystal
                && crystal != lastBrokenCrystal) {
            doBreak(crystal);
        }

        // ── Place ──────────────────────────────────────────────────────────
        if (placeClock >= placeDelay.getValueInt()
                && placedCount < maxCrystals.getValueInt()
                && mc.crosshairTarget instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK
                && canPlace(blockHit)) {
            doPlace();
        }
    }

    // ── Item use / attack cancel ───────────────────────────────────────────

    /** Cancels vanilla right-click crystal-use desync when not pressing RMB. */
    @EventHandler
    private void onItemUse(ItemUseEvent event) {
        if (isNull() || !mc.player.isHolding(Items.END_CRYSTAL)) return;
        if (noCountGlitch.getValue()
                && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) {
            event.cancel();
        }
    }

    /** Cancels vanilla left-click attack bounce when not pressing LMB. */
    @EventHandler
    private void onAttack(AttackEvent event) {
        if (isNull() || !mc.player.isHolding(Items.END_CRYSTAL)) return;
        if (noBounce.getValue()
                && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            event.cancel();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isDeadPlayerNearby() {
        if (mc.world == null) return false;
        List<? extends Entity> players = mc.world.getPlayers();
        for (Entity e : players) {
            if (e == mc.player) continue;
            if (e instanceof LivingEntity living && living.getHealth() <= 0f
                    && e.squaredDistanceTo(mc.player) < 36) {
                return true;
            }
        }
        return false;
    }
}
