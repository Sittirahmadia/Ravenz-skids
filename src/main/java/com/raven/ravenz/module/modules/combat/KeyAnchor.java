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

    // Explode Slot settings
    private final BooleanSetting useExplodeSlot = new BooleanSetting("Use Explode Slot", false);
    private final NumberSetting explodeSlot = new NumberSetting("Explode Slot", 1, 9, 1, 1);

    private final TimerUtil timer = new TimerUtil();

    // State
    private boolean keyPressed = false;

    public KeyAnchor() {
        super("Key Anchor", "Automatically places and explodes respawn anchors for PvP", -1, Category.COMBAT);
        this.addSettings(anchorKeybind, delay, useExplodeSlot, explodeSlot);
        this.getSettings().removeIf(setting -> setting instanceof KeybindSetting && !setting.equals(anchorKeybind));
    }

    @EventHandler
    private void onTickEvent(HandleInputEvent event) {
        if (isNull() || !isEnabled()) return;
        if (mc.currentScreen != null) return;

        boolean currentKeyState = KeyUtils.isKeyPressed(anchorKeybind.getKeyCode());

        // Hold — fire repeatedly on timer while key is held
        if (currentKeyState) {
            if (timer.hasElapsedTime(delay.getValueInt())) {
                processAnchorPvP();
                timer.reset();
            }
        } else {
            timer.reset();
        }

        keyPressed = currentKeyState;
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
                // Anchor is charged — explode it and STAY on explode slot
                swapToExplodeSlot();
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                // Do NOT restore slot after explode — stay on explode slot permanently
            } else {
                // Anchor is uncharged — recharge with glowstone
                if (swapToItem(Items.GLOWSTONE)) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                }
            }
            return;
        }

        // Not an anchor — try to place one, then restore slot
        BlockPos placementPos = targetBlock.offset(blockHit.getSide());
        if (isValidAnchorPosition(placementPos)) {
            if (swapToItem(Items.RESPAWN_ANCHOR)) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                }
        }
    }

    /**
     * Swap to the correct slot for exploding the anchor.
     * - Use Explode Slot ON -> switch to configured slot (1-9)
     * - OFF -> force slot 8 (index 7)
     */
    private void swapToExplodeSlot() {
        if (useExplodeSlot.getValue()) {
            mc.player.getInventory().selectedSlot = explodeSlot.getValueInt() - 1;
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

    @Override
    public void onEnable() {
        keyPressed = false;
        timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public int getKey() {
        return -1;
    }
}
