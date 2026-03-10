package com.raven.ravenz.module.modules.misc;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * DoubleAnchor — places and charges 2 respawn anchors in sequence.
 * Steps: anchor → place → glowstone → charge → anchor → place+place → glowstone → charge → totem
 * Binds to a key and self-disables when done.
 */
public final class DoubleAnchor extends Module {

    private final NumberSetting  switchDelay    = new NumberSetting("Switch Delay", 0, 20, 0, 1);
    private final NumberSetting  totemSlot      = new NumberSetting("Totem Slot", 1, 9, 9, 1);
    private final BooleanSetting useActivateKey = new BooleanSetting("Use Activation Key", false);
    private final KeybindSetting activateKey    = new KeybindSetting("Activation Key", GLFW.GLFW_KEY_UNKNOWN, false);

    private int step         = 0;
    private int delayCounter = 0;

    public DoubleAnchor() {
        super("Double Anchor", "Places and charges 2 respawn anchors automatically", -1, Category.MISC);
        addSettings(switchDelay, totemSlot, useActivateKey, activateKey);
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

        if (useActivateKey.getValue()) {
            int key = activateKey.getKeyCode();
            if (key == GLFW.GLFW_KEY_UNKNOWN || !KeyUtils.isKeyPressed(key)) return;
        }

        if (!hasRequiredItems()) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;
        if (blockHit.getType() == HitResult.Type.MISS) return;

        if (delayCounter < switchDelay.getValueInt()) { delayCounter++; return; }

        BlockPos pos = blockHit.getBlockPos();

        switch (step) {
            case 0 -> selectItem(Items.RESPAWN_ANCHOR);
            case 1 -> interactAt(pos);
            case 2 -> selectItem(Items.GLOWSTONE);
            case 3 -> interactAt(pos);
            case 4 -> selectItem(Items.RESPAWN_ANCHOR);
            case 5 -> { interactAt(pos); interactAt(pos); }
            case 6 -> selectItem(Items.GLOWSTONE);
            case 7 -> interactAt(pos);
            case 8 -> selectSlot(totemSlot.getValueInt() - 1);
            case 9 -> interactAt(pos);
            case 10 -> { reset(); disable(); return; }
        }
        step++;
        delayCounter = 0;
    }

    private void selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    private void selectSlot(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private void interactAt(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(pos), Direction.UP, pos, false);
        ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (res.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean hasRequiredItems() {
        boolean hasAnchor = false, hasGlow = false;
        for (int i = 0; i < 9; i++) {
            var s = mc.player.getInventory().getStack(i);
            if (s.isOf(Items.RESPAWN_ANCHOR)) hasAnchor = true;
            if (s.isOf(Items.GLOWSTONE))      hasGlow   = true;
        }
        return hasAnchor && hasGlow;
    }

    private void disable() { this.setEnabled(false); }

    private void reset() {
        step = 0;
        delayCounter = 0;
    }
}
