package com.raven.ravenz.module.modules.player;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.module.setting.RangeSetting;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import com.raven.ravenz.utils.math.MathUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

/**
 * While the player's inventory is open (or Auto Open is enabled),
 * automatically moves totems into the offhand slot and/or a chosen hotbar slot.
 *
 * Trigger modes:
 *  Always  — acts every tick when inventory is open.
 *  OnKey   — only while holding the activate key.
 *  OnLowHP — only when player HP is below the threshold.
 */
public final class AutoInventoryTotem extends Module {

    // ── Mode ───────────────────────────────────────────────────────────────
    private final ModeSetting triggerMode = new ModeSetting(
            "Trigger", "Always", "Always", "OnKey", "OnLowHP");

    // ── Slot targets ───────────────────────────────────────────────────────
    private final BooleanSetting fillOffhand = new BooleanSetting("Offhand", true);
    private final BooleanSetting fillHotbar  = new BooleanSetting("Hotbar", true);
    private final NumberSetting  totemSlot   = new NumberSetting("Totem Slot", 1, 9, 1, 1);
    private final BooleanSetting forceTotem  = new BooleanSetting("Force Totem", false);

    // ── Auto open ──────────────────────────────────────────────────────────
    private final BooleanSetting autoOpen = new BooleanSetting("Auto Open", false);

    // ── Timing ────────────────────────────────────────────────────────────
    private final RangeSetting delay = new RangeSetting("Delay (ticks)", 0, 20, 0, 0, 1);

    // ── OnLowHP threshold ─────────────────────────────────────────────────
    private final NumberSetting hpThreshold = new NumberSetting("HP Threshold", 1, 20, 8, 1);

    // ── OnKey ─────────────────────────────────────────────────────────────
    private final KeybindSetting activateKey = new KeybindSetting("Activate Key", GLFW.GLFW_KEY_C, false);

    // ── Re-equip ──────────────────────────────────────────────────────────
    private final BooleanSetting reEquip = new BooleanSetting("Re-Equip", true);

    // ── State ─────────────────────────────────────────────────────────────
    private int invClock   = -1;
    private int lastTotemCount = 0;
    private boolean wasOpen = false;

    public AutoInventoryTotem() {
        super("Auto Inventory Totem", "Keeps totems in offhand and hotbar automatically", -1, Category.PLAYER);
        addSettings(triggerMode, fillOffhand, fillHotbar, totemSlot, forceTotem,
                autoOpen, delay, hpThreshold, activateKey, reEquip);
    }

    @Override
    public void onEnable() {
        invClock = -1;
        lastTotemCount = 0;
        wasOpen = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (autoOpen.getValue() && mc.currentScreen instanceof InventoryScreen && !wasOpen) {
            mc.setScreen(null);
        }
        super.onDisable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.getNetworkHandler() == null) return;

        // Re-equip detection
        if (reEquip.getValue()) {
            int current = countTotemInInventory();
            if (current < lastTotemCount) invClock = 0; // act immediately
            lastTotemCount = current;
        }

        if (!isTriggerActive()) { invClock = -1; return; }

        // Auto-open inventory if needed and enabled
        if (autoOpen.getValue() && needsTotem() && !(mc.currentScreen instanceof InventoryScreen)) {
            wasOpen = false;
            mc.setScreen(new InventoryScreen(mc.player));
        }

        if (!(mc.currentScreen instanceof InventoryScreen invScreen)) {
            invClock = -1;
            return;
        }

        // Init delay clock on first tick inside inventory
        if (invClock == -1) {
            int min = (int) delay.getMinValue();
            int max = (int) delay.getMaxValue();
            invClock = min >= max ? min : (int) MathUtils.randomDoubleBetween(min, max + 1);
        }
        if (invClock > 0) { invClock--; return; }

        int syncId = invScreen.getScreenHandler().syncId;
        PlayerInventory inv = mc.player.getInventory();
        boolean didAction = false;

        // Fill offhand
        if (fillOffhand.getValue() && !inv.offhand.get(0).isOf(Items.TOTEM_OF_UNDYING)) {
            int slot = findTotemSlot();
            if (slot != -1) {
                mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
                didAction = true;
            }
        }

        // Fill hotbar slot
        if (!didAction && fillHotbar.getValue()) {
            int targetSlot = totemSlot.getValueInt() - 1;
            ItemStack inSlot = inv.main.get(targetSlot);
            boolean needs = inSlot.isEmpty()
                    || (forceTotem.getValue() && !inSlot.isOf(Items.TOTEM_OF_UNDYING));
            if (needs) {
                int slot = findTotemSlot();
                if (slot != -1) {
                    mc.interactionManager.clickSlot(syncId, slot, targetSlot, SlotActionType.SWAP, mc.player);
                }
            }
        }

        // Auto-close if we opened it and we're done
        if (autoOpen.getValue() && !wasOpen && !needsTotem()) {
            mc.setScreen(null);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isTriggerActive() {
        if (mc.player == null) return false;
        return switch (triggerMode.getMode()) {
            case "OnKey"   -> KeyUtils.isKeyPressed(activateKey.getKeyCode());
            case "OnLowHP" -> mc.player.getHealth() <= hpThreshold.getValue() * 2f;
            default        -> true; // Always
        };
    }

    private boolean needsTotem() {
        if (mc.player == null) return false;
        boolean offhandMissing = fillOffhand.getValue()
                && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        boolean hotbarMissing  = fillHotbar.getValue()
                && !mc.player.getInventory().getStack(totemSlot.getValueInt() - 1).isOf(Items.TOTEM_OF_UNDYING);
        return (offhandMissing || hotbarMissing) && countTotemInInventory() > 0;
    }

    /** Finds the first totem in inventory slots 9–35 (not hotbar, not offhand). */
    private int findTotemSlot() {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }

    /** Counts totems in inventory slots 9–35. */
    private int countTotemInInventory() {
        int count = 0;
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) count++;
        }
        return count;
    }
}
