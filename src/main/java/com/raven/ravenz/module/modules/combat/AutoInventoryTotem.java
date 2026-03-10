package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.mixin.HandledScreenAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

/**
 * AutoInventoryTotem — ported from vydra (AutoInvTotemLegit)
 *
 * - Auto Switch: selects totem hotbar slot whenever a screen is open
 * - Slot-Under-Mouse: hover over a totem in inventory → auto swap to offhand
 * - Activate Key: optionally only run when holding a key
 */
public class AutoInventoryTotem extends Module {

    private final BooleanSetting autoSwitch     = new BooleanSetting("Auto Slot Switch",  true);
    private final NumberSetting  switchDelay    = new NumberSetting("Switch Delay (t)",   0, 20, 0, 1);
    private final NumberSetting  totemSlot      = new NumberSetting("Totem Slot",         0, 8, 2, 1);
    private final BooleanSetting forceTotem     = new BooleanSetting("Replace Junk Items", true);
    private final BooleanSetting activateOnKey  = new BooleanSetting("Activation Key",    false);
    private final KeybindSetting activateKey    = new KeybindSetting("Binding",           GLFW.GLFW_KEY_C, false);

    private int invClock         = -1;
    private int switchCounter    = 0;

    public AutoInventoryTotem() {
        super("Auto Inventory Totem", "Automatically puts totems in offhand when in inventory", -1, Category.COMBAT);
        addSettings(autoSwitch, switchDelay, totemSlot, forceTotem, activateOnKey, activateKey);
    }

    @Override
    public void onEnable() {
        invClock = -1;
        switchCounter = 0;
        super.onEnable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;

        // ── Auto Switch Slot ───────────────────────────────────────────────
        if (autoSwitch.getValue() && mc.currentScreen != null) {
            if (switchCounter <= 0) {
                mc.player.getInventory().selectedSlot = (int) totemSlot.getValue();
                switchCounter = (int) switchDelay.getValue();
            } else {
                switchCounter--;
            }
        }

        // ── Only run in inventory ──────────────────────────────────────────
        if (!(mc.currentScreen instanceof InventoryScreen invScreen)) {
            invClock = -1;
            return;
        }

        // ── Activation key check ───────────────────────────────────────────
        if (activateOnKey.getValue()) {
            if (!com.raven.ravenz.utils.keybinding.KeyUtils.isKeyPressed(activateKey.getKeyCode())) return;
        }

        // ── Delay counter ──────────────────────────────────────────────────
        if (invClock == -1) invClock = 0;
        if (invClock > 0) { invClock--; return; }

        int syncId = mc.player.currentScreenHandler.syncId;
        int hotbarSlot = (int) totemSlot.getValue();

        // ── If offhand is empty / not totem → find one and swap ───────────
        if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            // Try slot under mouse first
            if (mc.currentScreen instanceof HandledScreen<?> hs) {
                Slot focused = ((HandledScreenAccessor) hs).getFocusedSlot();
                if (focused != null) {
                    int idx = focused.getIndex();
                    if (idx >= 0 && idx <= 35) {
                        if (mc.player.getInventory().getStack(idx).isOf(Items.TOTEM_OF_UNDYING)) {
                            mc.interactionManager.clickSlot(syncId, idx, 40, SlotActionType.SWAP, mc.player);
                            return;
                        }
                    }
                }
            }
        } else {
            // Offhand has totem — check if hotbar slot needs one
            var mainHand = mc.player.getInventory().getStack(hotbarSlot);
            if (!mainHand.isEmpty() && (!forceTotem.getValue() || mainHand.isOf(Items.TOTEM_OF_UNDYING))) {
                return;
            }

            // Slot under mouse → swap to hotbar slot
            if (mc.currentScreen instanceof HandledScreen<?> hs) {
                Slot focused = ((HandledScreenAccessor) hs).getFocusedSlot();
                if (focused != null) {
                    int idx = focused.getIndex();
                    if (idx >= 0 && idx <= 35 && idx != hotbarSlot) {
                        if (mc.player.getInventory().getStack(idx).isOf(Items.TOTEM_OF_UNDYING)) {
                            mc.interactionManager.clickSlot(syncId, idx, hotbarSlot, SlotActionType.SWAP, mc.player);
                        }
                    }
                }
            }
        }
    }
}
