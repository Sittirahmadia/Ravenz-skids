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
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

/**
 * AutoDtap — crosshair-based double-pop crystal placer.
 * Uses canPlaceCrystal() with inline obsidian/bedrock check to avoid
 * the broken canPlaceCrystalServer bug (uses && instead of ||).
 */
public final class AutoDtap extends Module {

    private final NumberSetting  placeDelay          = new NumberSetting("Place Delay",         0, 20, 0, 1);
    private final NumberSetting  breakDelay          = new NumberSetting("Break Delay",          0, 20, 0, 1);
    private final NumberSetting  maxCrystals         = new NumberSetting("Max Crystals",         1, 6,  2, 1);
    private final BooleanSetting requireObsidian     = new BooleanSetting("Require Obsidian",    true);
    private final BooleanSetting simulateClick       = new BooleanSetting("Simulate Click",      true);
    private final BooleanSetting noCountGlitch       = new BooleanSetting("No Count Glitch",     true);
    private final BooleanSetting noBounce            = new BooleanSetting("No Bounce",           true);
    private final BooleanSetting activateOnRightClick= new BooleanSetting("Activate On RMB",     false);

    private int placeClock = 0;
    private int breakClock = 0;
    private int placedCount = 0;
    private EndCrystalEntity lastBrokenCrystal = null;

    public AutoDtap() {
        super("Auto Dtap", "Crosshair-based double-pop crystal placer", -1, Category.COMBAT);
        addSettings(placeDelay, breakDelay, maxCrystals,
                requireObsidian, simulateClick, noCountGlitch, noBounce,
                activateOnRightClick);
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

    private void reset() {
        placeClock = 0;
        breakClock = 0;
        placedCount = 0;
        lastBrokenCrystal = null;
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        if (activateOnRightClick.getValue() &&
                GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
            return;

        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;

        placeClock++;
        breakClock++;

        // Break
        if (breakClock >= breakDelay.getValueInt()
                && mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof EndCrystalEntity crystal
                && crystal != lastBrokenCrystal) {
            doBreak(crystal);
        }

        // Place
        if (placeClock >= placeDelay.getValueInt()
                && placedCount < maxCrystals.getValueInt()
                && mc.crosshairTarget instanceof BlockHitResult blockHit
                && blockHit.getType() != HitResult.Type.MISS
                && canPlace(blockHit)) {
            doPlace(blockHit);
        }
    }

    private void doPlace(BlockHitResult hit) {
        if (simulateClick.getValue()) ClickSimulator.leftClick();
        ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (res.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
        placedCount++;
        placeClock = 0;
    }

    private void doBreak(EndCrystalEntity crystal) {
        if (simulateClick.getValue()) ClickSimulator.leftClick();
        mc.interactionManager.attackEntity(mc.player, crystal);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (placedCount > 0) placedCount--;
        lastBrokenCrystal = crystal;
        breakClock = 0;
    }

    private boolean canPlace(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        var state = mc.world.getBlockState(pos);

        boolean validBase = !requireObsidian.getValue()
                || state.isOf(Blocks.OBSIDIAN)
                || state.isOf(Blocks.BEDROCK);
        if (!validBase) return false;

        BlockPos above = pos.up();
        BlockPos above2 = pos.up(2);
        if (!mc.world.getBlockState(above).isAir()) return false;
        if (!mc.world.getBlockState(above2).isAir()) return false;

        var box = new net.minecraft.util.math.Box(
                above.getX(), above.getY(), above.getZ(),
                above.getX() + 1, above.getY() + 2, above.getZ() + 1);
        return mc.world.getOtherEntities(null, box).isEmpty();
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
