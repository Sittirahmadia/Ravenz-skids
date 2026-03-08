package com.raven.ravenz.module.modules.combat;

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
import com.raven.ravenz.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

/**
 * Auto Inventory Totem — silently opens the inventory and moves totems
 * into the offhand and a configured hotbar slot.
 *
 * Trigger modes:  Always | OnKey | OnLowHP
 * Pick modes:     First | Last | Random
 */
public class AutoInventoryTotem extends Module {

    // ── Trigger / Pick ─────────────────────────────────────────────────────
    private final ModeSetting triggerMode = new ModeSetting("Trigger",   "Always", "Always", "OnKey", "OnLowHP");
    private final ModeSetting pickMode    = new ModeSetting("Pick Mode", "First",  "First",  "Last",  "Random");

    // ── Slot config ────────────────────────────────────────────────────────
    private final BooleanSetting fillOffhand = new BooleanSetting("Fill Offhand",     true);
    private final BooleanSetting fillHotbar  = new BooleanSetting("Fill Hotbar Slot", true);
    private final NumberSetting  totemSlot   = new NumberSetting("Totem Hotbar Slot", 1, 9, 1, 1);
    private final BooleanSetting forceTotem  = new BooleanSetting("Force Totem",      false);

    // ── Auto-open ──────────────────────────────────────────────────────────
    private final BooleanSetting autoOpen  = new BooleanSetting("Auto Open Inv", false);
    private final RangeSetting   openDelay = new RangeSetting("Open Delay (ms)", 0, 500, 0, 50, 10);

    // ── Trigger settings ───────────────────────────────────────────────────
    private final NumberSetting  hpThreshold = new NumberSetting("HP Threshold",  1, 20, 8, 1);
    private final KeybindSetting activateKey = new KeybindSetting("Activate Key", GLFW.GLFW_KEY_C, false);

    // ── Re-equip ───────────────────────────────────────────────────────────
    private final BooleanSetting reEquip = new BooleanSetting("Re-Equip", true);

    // ── State ──────────────────────────────────────────────────────────────
    private final TimerUtil openTimer = new TimerUtil();
    private boolean wasInvOpen   = false;
    private int     lastTotemCount = 0;

    public AutoInventoryTotem() {
        super("Auto Inventory Totem", "Automatically keeps totems in offhand and hotbar via inventory", -1, Category.COMBAT);
        addSettings(triggerMode, pickMode, fillOffhand, fillHotbar, totemSlot, forceTotem,
                autoOpen, openDelay, hpThreshold, activateKey, reEquip);
    }

    @Override
    public void onEnable() {
        openTimer.reset();
        wasInvOpen = false;
        lastTotemCount = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (!wasInvOpen && mc.currentScreen instanceof InventoryScreen) {
            mc.setScreen(null);
        }
        super.onDisable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;

        // ── Re-equip detection ─────────────────────────────────────────────
        if (reEquip.getValue()) {
            int currentCount = countTotemsInInventory();
            boolean totemUsed = currentCount < lastTotemCount;
            lastTotemCount = currentCount;
            if (totemUsed && autoOpen.getValue()) {
                openTimer.reset(); // trigger re-open immediately
            }
        }

        if (!isTriggerActive()) return;

        boolean needsAction = needsOffhand() || needsHotbar();

        // Nothing to do — close if we auto-opened
        if (!needsAction) {
            if (!wasInvOpen && mc.currentScreen instanceof InventoryScreen) {
                mc.setScreen(null);
                wasInvOpen = false;
            }
            return;
        }

        // ── Auto-open inventory ────────────────────────────────────────────
        if (autoOpen.getValue() && !(mc.currentScreen instanceof InventoryScreen)) {
            long delay = (long) MathUtils.randomDoubleBetween(openDelay.getMinValue(), openDelay.getMaxValue());
            if (openTimer.hasElapsedTime(delay)) {
                wasInvOpen = mc.currentScreen instanceof InventoryScreen;
                mc.setScreen(new InventoryScreen(mc.player));
                openTimer.reset();
            }
            return;
        }

        if (!(mc.currentScreen instanceof InventoryScreen)) return;

        int syncId = mc.player.currentScreenHandler.syncId;

        // ── Fill offhand ───────────────────────────────────────────────────
        if (fillOffhand.getValue() && needsOffhand()) {
            int slot = findTotemInInventory();
            if (slot != -1) {
                mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
                return;
            }
        }

        // ── Fill hotbar slot ───────────────────────────────────────────────
        if (fillHotbar.getValue() && needsHotbar()) {
            int targetSlot = totemSlot.getValueInt() - 1;
            int slot = findTotemInInventory();
            if (slot != -1) {
                mc.interactionManager.clickSlot(syncId, slot, targetSlot, SlotActionType.SWAP, mc.player);
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isTriggerActive() {
        return switch (triggerMode.getMode()) {
            case "OnKey"   -> KeyUtils.isKeyPressed(activateKey.getKeyCode());
            case "OnLowHP" -> mc.player != null && mc.player.getHealth() <= hpThreshold.getValue() * 2f;
            default        -> true; // Always
        };
    }

    private boolean needsOffhand() {
        if (!fillOffhand.getValue()) return false;
        return !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private boolean needsHotbar() {
        if (!fillHotbar.getValue()) return false;
        int slot = totemSlot.getValueInt() - 1;
        var stack = mc.player.getInventory().getStack(slot);
        if (stack.isOf(Items.TOTEM_OF_UNDYING)) return false;
        return stack.isEmpty() || forceTotem.getValue();
    }

    /**
     * Finds a totem in the main inventory (slots 9–35) and hotbar fallback.
     * Respects Pick Mode: First / Last / Random.
     */
    private int findTotemInInventory() {
        java.util.List<Integer> candidates = new java.util.ArrayList<>();

        // Main inventory slots 9–35
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING))
                candidates.add(i);
        }
        // Hotbar fallback (excluding target slot)
        if (candidates.isEmpty()) {
            int target = totemSlot.getValueInt() - 1;
            for (int i = 0; i < 9; i++) {
                if (i == target) continue;
                if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING))
                    candidates.add(i);
            }
        }

        if (candidates.isEmpty()) return -1;

        return switch (pickMode.getMode()) {
            case "Last"   -> candidates.get(candidates.size() - 1);
            case "Random" -> candidates.get((int) (Math.random() * candidates.size()));
            default       -> candidates.get(0); // First
        };
    }

    /** Count totems in the full inventory (excluding offhand). */
    private int countTotemsInInventory() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) count++;
        }
        return count;
    }
}
