package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public class AIT extends Module {

    private final BooleanSetting autoSwitch    = new BooleanSetting("Auto Switch",      false);
    private final NumberSetting  delay         = new NumberSetting("Delay",              0, 20, 0, 1);
    private final NumberSetting  totemSlot     = new NumberSetting("Totem Slot",         0, 8,  0, 1);
    private final BooleanSetting forceTotem    = new BooleanSetting("Force Totem",       false);
    private final BooleanSetting activateOnKey = new BooleanSetting("Activate On Key",   false);
    private final KeybindSetting activateKey   = new KeybindSetting("Binding",           GLFW.GLFW_KEY_C, false);

    private int invClock = -1;

    public AIT() {
        super("AIT", "Automatically puts totems in offhand when in inventory", -1, Category.COMBAT);
        addSettings(autoSwitch, delay, totemSlot, forceTotem, activateOnKey, activateKey);
    }

    @Override
    public void onEnable() {
        invClock = -1;
        super.onEnable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            invClock = -1;
            return;
        }

        if (invClock == -1)
            invClock = (int) delay.getValue();

        if (invClock > 0) {
            invClock--;
            return;
        }

        var inv = mc.player.getInventory();

        if (autoSwitch.getValue())
            inv.selectedSlot = (int) totemSlot.getValue();

        if (activateOnKey.getValue()) {
            if (!com.raven.ravenz.utils.keybinding.KeyUtils.isKeyPressed(activateKey.getKeyCode()))
                return;
        }

        int syncId = ((InventoryScreen) mc.currentScreen).getScreenHandler().syncId;

        // Fill offhand
        if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            int slot = findTotemSlot();
            if (slot != -1) {
                mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
                return;
            }
        }

        // Fill main hand
        var mainHand = inv.getStack(inv.selectedSlot);
        if (mainHand.isEmpty() || (forceTotem.getValue() && !mainHand.isOf(Items.TOTEM_OF_UNDYING))) {
            int slot = findTotemSlot();
            if (slot != -1) {
                mc.interactionManager.clickSlot(syncId, slot, inv.selectedSlot, SlotActionType.SWAP, mc.player);
            }
        }
    }

    private int findTotemSlot() {
        var inv = mc.player.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING))
                return i;
        }
        return -1;
    }
}
