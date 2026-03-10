package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import com.raven.ravenz.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class KeyAnchor extends Module {

    private final KeybindSetting anchorKeybind = new KeybindSetting("Anchor Key", GLFW.GLFW_MOUSE_BUTTON_4, false);
    private final NumberSetting delay = new NumberSetting("Delay (MS)", 1, 500, 50, 1);
    private final NumberSetting restoreDelayTicks = new NumberSetting("Restore Delay", 1, 20, 2, 1);

    // Explode Slot settings
    private final BooleanSetting useExplodeSlot = new BooleanSetting("Use Explode Slot", false);
    private final NumberSetting explodeSlot = new NumberSetting("Explode Slot", 1, 9, 1, 1);
    private final BooleanSetting useHand = new BooleanSetting("Use Hand", false);

    private final TimerUtil timer = new TimerUtil();

    // State
    private boolean keyPressed = false;
    private boolean isActive = false;
    private int originalSlot = -1;
    private boolean hasPlacedThisCycle = false;
    private boolean pendingRestoreSlot = false;
    private int pendingRestoreTicksLeft = 0;

    public KeyAnchor() {
        super("Key Anchor", "Automatically places and explodes respawn anchors for PvP", -1, Category.COMBAT);
        this.addSettings(anchorKeybind, delay, restoreDelayTicks, useExplodeSlot, explodeSlot, useHand);
        this.getSettings().removeIf(setting -> setting instanceof KeybindSetting && !setting.equals(anchorKeybind));
    }

    @EventHandler
    private void onTickEvent(HandleInputEvent event) {
        if (isNull() || !isEnabled()) return;
        if (mc.currentScreen != null) return;

        boolean currentKeyState = KeyUtils.isKeyPressed(anchorKeybind.getKeyCode());

        if (currentKeyState && !keyPressed) {
            // Key just pressed — cancel any pending restore first so originalSlot is still valid
            cancelPendingRestore();
            startAnchorPvP();
        } else if (!currentKeyState && keyPressed) {
            // Key just released
            stopAnchorPvP();
        } else if (!currentKeyState) {
            // Key is not held — allow placing again next press
            hasPlacedThisCycle = false;
        }

        keyPressed = currentKeyState;

        // Process anchor logic on timer while key is held
        if (isActive && timer.hasElapsedTime(delay.getValueInt())) {
            processAnchorPvP();
            timer.reset();
        }

        // Tick down pending slot restore
        if (pendingRestoreSlot) {
            if (pendingRestoreTicksLeft <= 0) {
                restoreOriginalSlot();
                pendingRestoreSlot = false;
            } else {
                pendingRestoreTicksLeft--;
            }
        }
    }

    private void startAnchorPvP() {
        if (isActive) return;
        isActive = true;
        originalSlot = mc.player.getInventory().selectedSlot;
        hasPlacedThisCycle = false;
        timer.reset();
    }

    private void stopAnchorPvP() {
        if (!isActive) return;
        cancelPendingRestore();
        restoreOriginalSlot();
        isActive = false;
        originalSlot = -1;
        hasPlacedThisCycle = false;
    }

    private void processAnchorPvP() {
        if (mc.world == null || mc.player == null) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;

        BlockPos targetBlock = blockHit.getBlockPos();
        var blockState = mc.world.getBlockState(targetBlock);

        if (blockState.isAir()) return;

        if (blockState.getBlock() == Blocks.RESPAWN_ANCHOR) {
            int charges = blockState.get(RespawnAnchorBlock.CHARGES);
            if (charges > 0) {
                // Anchor is charged — explode it
                swapToExplodeSlot();
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                // Only schedule restore if we actually switched slots
                if (!useHand.getValue()) {
                    scheduleRestoreOriginalSlot();
                }
                // Reset place flag so we can place again after explode
                hasPlacedThisCycle = false;
            } else {
                // Anchor is uncharged — recharge with glowstone
                if (swapToItem(Items.GLOWSTONE)) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    scheduleRestoreOriginalSlot();
                }
            }
            return;
        }

        // Not an anchor — try to place one
        BlockPos placementPos = targetBlock.offset(blockHit.getSide());
        if (!hasPlacedThisCycle && isValidAnchorPosition(placementPos)) {
            if (swapToItem(Items.RESPAWN_ANCHOR)) {
                hasPlacedThisCycle = true;
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                scheduleRestoreOriginalSlot();
            }
        }
    }

    /**
     * Swap to the correct slot for exploding the anchor.
     * Priority:
     *   1. Use Hand ON  -> keep current slot, use whatever is in hand
     *   2. Use Explode Slot ON -> switch to configured slot (1-9)
     *   3. Both OFF -> force slot 8 (index 7) to avoid accidental sword swap
     */
    private void swapToExplodeSlot() {
        if (useHand.getValue()) return; // keep current slot

        if (useExplodeSlot.getValue()) {
            int slot = explodeSlot.getValueInt() - 1; // 1-9 -> 0-8
            mc.player.getInventory().selectedSlot = slot;
            return;
        }

        // Fallback: force slot 8 (index 7)
        mc.player.getInventory().selectedSlot = 7;
    }

    private boolean swapToItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    private boolean isValidAnchorPosition(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;
        if (mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(pos)) > 4.5) return false;
        if (!mc.world.getBlockState(pos).isAir()) return false;
        BlockPos playerPos = mc.player.getBlockPos();
        return !pos.equals(playerPos) && !pos.equals(playerPos.up());
    }

    private void restoreOriginalSlot() {
        if (originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
    }

    private void scheduleRestoreOriginalSlot() {
        if (originalSlot == -1) return;
        // Reset tick counter every time to avoid stacking multiple restores
        pendingRestoreSlot = true;
        pendingRestoreTicksLeft = restoreDelayTicks.getValueInt();
    }

    private void cancelPendingRestore() {
        pendingRestoreSlot = false;
        pendingRestoreTicksLeft = 0;
    }

    @Override
    public void onEnable() {
        keyPressed = false;
        isActive = false;
        originalSlot = -1;
        hasPlacedThisCycle = false;
        pendingRestoreSlot = false;
        pendingRestoreTicksLeft = 0;
        timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        stopAnchorPvP();
        super.onDisable();
    }

    @Override
    public int getKey() {
        return -1;
    }
}
