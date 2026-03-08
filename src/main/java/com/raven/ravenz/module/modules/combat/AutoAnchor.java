package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class AutoAnchor extends Module {

    private final NumberSetting switchDelay = new NumberSetting("Switch Delay",  0, 20, 0, 1);
    private final NumberSetting placeDelay  = new NumberSetting("Place Delay",   0, 20, 0, 1);
    private final NumberSetting cooldown    = new NumberSetting("Cooldown",      0, 20, 0, 1);
    private final NumberSetting itemSwap    = new NumberSetting("Swap Slot",     1,  9, 1, 1);
    private final BooleanSetting chargeOnly = new BooleanSetting("Charge Only",  false);
    private final BooleanSetting syncSlot   = new BooleanSetting("Sync Slot",    true);

    private boolean hasAnchored;
    private int switchClock, placeClock, cooldownClock;

    public AutoAnchor() {
        super("Auto Anchor", "Automatically charges and detonates respawn anchors", -1, Category.COMBAT);
        addSettings(switchDelay, placeDelay, cooldown, itemSwap, chargeOnly, syncSlot);
    }

    @Override
    public void onEnable() { reset(); }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        if (hasAnchored) {
            if (cooldownClock < cooldown.getValueInt()) { cooldownClock++; return; }
            reset();
        }

        boolean rmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (!rmb) { reset(); return; }

        if (mc.player.isUsingItem()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
        if (hit.getType() == HitResult.Type.MISS) return;

        BlockPos pos = hit.getBlockPos();
        var state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof RespawnAnchorBlock)) return;

        int charges = state.get(RespawnAnchorBlock.CHARGES);
        mc.options.useKey.setPressed(false);

        if (charges == 0) {
            // Charge with glowstone
            if (!mc.player.isHolding(Items.GLOWSTONE)) {
                if (switchClock < switchDelay.getValueInt()) { switchClock++; return; }
                swapToItem(Items.GLOWSTONE);
                switchClock = 0;
            }
            if (placeClock < placeDelay.getValueInt()) { placeClock++; return; }
            ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            if (res.isAccepted() && res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
            placeClock = 0;

        } else if (!chargeOnly.getValue()) {
            // Detonation: swap to configured slot
            int target = itemSwap.getValueInt() - 1;
            if (mc.player.getInventory().selectedSlot != target) {
                if (switchClock < switchDelay.getValueInt()) { switchClock++; return; }
                mc.player.getInventory().selectedSlot = target;
                if (syncSlot.getValue()) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(target));
                }
                switchClock = 0;
            }
            if (placeClock < placeDelay.getValueInt()) { placeClock++; return; }
            ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            if (res.isAccepted() && res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
            placeClock = 0;
            hasAnchored = true;
        }
    }

    private void reset() {
        hasAnchored   = false;
        switchClock   = 0;
        placeClock    = 0;
        cooldownClock = 0;
    }

    private void swapToItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                if (syncSlot.getValue()) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                }
                return;
            }
        }
    }
}
