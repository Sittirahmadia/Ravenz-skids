package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.event.impl.player.TickEvent;
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
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class KeyAnchor extends Module {

    private final KeybindSetting anchorKeybind = new KeybindSetting("Anchor Key", GLFW.GLFW_MOUSE_BUTTON_5, false);
    private final NumberSetting delay = new NumberSetting("Delay (MS)", 1, 500, 50, 1);
    private final NumberSetting restoreDelayTicks = new NumberSetting("Restore Delay", 1, 20, 2, 1);

    // Restore slot settings
    private final BooleanSetting restoreToTotem = new BooleanSetting("Restore To Totem", true);
    private final NumberSetting totemSlot = new NumberSetting("Totem Slot", 1, 9, 8, 1);

    // Explode slot settings
    private final BooleanSetting useExplodeSlot = new BooleanSetting("Use Explode Slot", false);
    private final NumberSetting explodeSlot = new NumberSetting("Explode Slot", 1, 9, 8, 1);

    private final TimerUtil timer = new TimerUtil();
    private boolean keyPressed = false;
    private boolean isActive = false;
    private int originalSlot = -1;
    private boolean hasPlacedThisCycle = false;
    private boolean pendingRestoreSlot = false;
    private int pendingRestoreTicksLeft = 0;

    public KeyAnchor() {
        super("Key Anchor", "Automatically places and explodes respawn anchors for PvP", -1, Category.COMBAT);
        this.addSettings(anchorKeybind, delay, restoreDelayTicks,
                restoreToTotem, totemSlot,
                useExplodeSlot, explodeSlot);
        this.getSettings().removeIf(setting -> setting instanceof KeybindSetting && !setting.equals(anchorKeybind));
    }

    @EventHandler
    private void onTickEvent(HandleInputEvent event) {
        if (isNull() || !isEnabled()) return;
        if (mc.currentScreen != null) return;

        boolean currentKeyState = KeyUtils.isKeyPressed(anchorKeybind.getKeyCode());

        if (currentKeyState && !keyPressed) {
            startAnchorPvP();
        } else if (!currentKeyState && keyPressed) {
            stopAnchorPvP();
        } else if (!currentKeyState) {
            hasPlacedThisCycle = false;
        }

        keyPressed = currentKeyState;

        if (isActive && timer.hasElapsedTime(delay.getValueInt())) {
            processAnchorPvP();
            timer.reset();
        }

        if (pendingRestoreSlot) {
            if (pendingRestoreTicksLeft <= 0) {
                restoreSlot();
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
        restoreSlot();
        isActive = false;
        originalSlot = -1;
        pendingRestoreSlot = false;
        pendingRestoreTicksLeft = 0;
    }

    /**
     * Restore slot logic:
     * - Restore To Totem ON  → switch ke configured totem slot (1-9)
     * - Restore To Totem OFF → restore ke slot yang aktif sebelum key ditekan
     */
    private void restoreSlot() {
        if (mc.player == null) return;
        if (restoreToTotem.getValue()) {
            mc.player.getInventory().selectedSlot = totemSlot.getValueInt() - 1;
        } else if (originalSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
    }

    /**
     * Explode slot logic:
     * - Use Explode Slot ON  → switch ke configured explode slot sebelum explode
     * - Use Explode Slot OFF → pakai totem/sword seperti biasa (behavior lama)
     */
    private boolean applyExplodeSlot() {
        if (!useExplodeSlot.getValue()) return false;
        if (mc.player == null) return false;
        mc.player.getInventory().selectedSlot = explodeSlot.getValueInt() - 1;
        return true;
    }

    private void processAnchorPvP() {
        if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;

        BlockPos targetBlock = blockHit.getBlockPos();
        var blockState = mc.world.getBlockState(targetBlock);

        if (blockState.isAir()) return;

        if (blockState.getBlock() == Blocks.RESPAWN_ANCHOR) {
            int charges = blockState.get(RespawnAnchorBlock.CHARGES);
            if (charges > 0) {
                // Explode slot ON → pakai explode slot, lalu restore ke totem setelah delay
                // Explode slot OFF → pakai totem/sword seperti biasa
                boolean swapped = applyExplodeSlot();
                if (!swapped) {
                    swapped = swapToItem(Items.TOTEM_OF_UNDYING) || swapToSword();
                }
                if (swapped) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    scheduleRestoreSlot();
                    hasPlacedThisCycle = true;
                }
            } else {
                if (swapToItem(Items.GLOWSTONE)) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    hasPlacedThisCycle = true;
                }
            }
            return;
        }

        BlockPos placementPos = targetBlock.offset(blockHit.getSide());
        if (isValidAnchorPosition(placementPos) && !hasPlacedThisCycle) {
            if (swapToItem(Items.RESPAWN_ANCHOR)) {
                hasPlacedThisCycle = true;
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
            }
        }
    }

    private boolean isValidAnchorPosition(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;
        if (mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(pos)) > 4.5) return false;
        if (!mc.world.getBlockState(pos).isAir()) return false;
        BlockPos playerPos = mc.player.getBlockPos();
        return !pos.equals(playerPos) && !pos.equals(playerPos.up());
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

    private boolean swapToSword() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isIn(ItemTags.SWORDS)) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    private void scheduleRestoreSlot() {
        if (originalSlot != -1) {
            pendingRestoreSlot = true;
            pendingRestoreTicksLeft = restoreDelayTicks.getValueInt();
        }
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
