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
import com.raven.ravenz.utils.mc.ChatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public final class AutoInventoryTotem extends Module {

    // ── Trigger mode ───────────────────────────────────────────────────────
    private final ModeSetting    triggerMode   = new ModeSetting("Trigger", "Always", "Always", "OnKey", "OnLowHP");

    // ── Slot targets ───────────────────────────────────────────────────────
    private final BooleanSetting fillOffhand   = new BooleanSetting("Offhand",       true);
    private final BooleanSetting fillHotbar    = new BooleanSetting("Hotbar",        true);
    private final NumberSetting  totemSlot     = new NumberSetting("Totem Slot",     1, 9, 1, 1);
    private final BooleanSetting fillSlot2     = new BooleanSetting("Second Slot",   false);
    private final NumberSetting  totemSlot2    = new NumberSetting("Totem Slot 2",   1, 9, 2, 1);
    private final BooleanSetting forceTotem    = new BooleanSetting("Force Totem",   false);

    // ── Auto open ──────────────────────────────────────────────────────────
    private final BooleanSetting autoOpen      = new BooleanSetting("Auto Open",     false);

    // ── Timing ────────────────────────────────────────────────────────────
    private final RangeSetting   delay         = new RangeSetting("Delay (ticks)", 0, 20, 0, 0, 1);
    private final NumberSetting  swapCooldown  = new NumberSetting("Swap Cooldown", 0, 10, 1, 1);

    // ── HP settings ───────────────────────────────────────────────────────
    private final NumberSetting  hpThreshold   = new NumberSetting("HP Threshold",   1, 20, 8, 1);
    private final BooleanSetting lowHpBypass   = new BooleanSetting("Low HP Bypass", true);
    private final NumberSetting  bypassHp      = new NumberSetting("Bypass HP",      1, 20, 4, 1);

    // ── OnKey ─────────────────────────────────────────────────────────────
    private final KeybindSetting activateKey   = new KeybindSetting("Activate Key",  GLFW.GLFW_KEY_C, false);

    // ── Re-equip ──────────────────────────────────────────────────────────
    private final BooleanSetting reEquip       = new BooleanSetting("Re-Equip",      true);

    // ── Notifications ─────────────────────────────────────────────────────
    private final BooleanSetting warnLow       = new BooleanSetting("Warn Low",      true);
    private final NumberSetting  warnThreshold = new NumberSetting("Warn At",        1, 10, 3, 1);

    // ── State ─────────────────────────────────────────────────────────────
    private int     invClock         = -1;
    private int     swapCooldownTick = 0;
    private int     lastTotemCount   = 0;
    private boolean autoOpened       = false; // FIX: separate flag for "we opened it" vs "player opened it"
    private boolean offhandWasTotem  = false;
    private boolean reEquipPending   = false; // FIX: re-equip flag independent of trigger gate
    private boolean warned           = false;

    public AutoInventoryTotem() {
        super("Auto Inventory Totem", "Keeps totems in offhand and hotbar automatically", -1, Category.PLAYER);
        addSettings(triggerMode, fillOffhand, fillHotbar, totemSlot,
                fillSlot2, totemSlot2, forceTotem,
                autoOpen, delay, swapCooldown,
                hpThreshold, lowHpBypass, bypassHp,
                activateKey, reEquip, warnLow, warnThreshold);
    }

    @Override
    public void onEnable() {
        invClock         = -1;
        swapCooldownTick = 0;
        lastTotemCount   = 0;
        autoOpened       = false;
        reEquipPending   = false;
        warned           = false;
        if (mc.player != null)
            offhandWasTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        // FIX: only close inventory if WE opened it
        if (autoOpened && mc.currentScreen instanceof InventoryScreen)
            mc.setScreen(null);
        super.onDisable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.getNetworkHandler() == null) return;

        // ── Swap cooldown ─────────────────────────────────────────────────
        if (swapCooldownTick > 0) { swapCooldownTick--; return; }

        // ── Re-equip: detect offhand totem pop ────────────────────────────
        boolean offhandIsTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        if (reEquip.getValue() && offhandWasTotem && !offhandIsTotem) {
            reEquipPending = true; // FIX: store as pending flag, bypass trigger gate below
            invClock = 0;
        }
        offhandWasTotem = offhandIsTotem;

        // ── Low totem warning ─────────────────────────────────────────────
        int currentCount = countTotems();
        if (warnLow.getValue()) {
            if (currentCount > 0 && currentCount <= warnThreshold.getValueInt() && !warned) {
                ChatUtil.warningChatMessage("Low on totems! (" + currentCount + " left)");
                warned = true;
            } else if (currentCount > warnThreshold.getValueInt()) {
                warned = false;
            }
            if (currentCount == 0 && lastTotemCount > 0) {
                ChatUtil.errorChatMessage("No totems remaining!");
            }
        }
        lastTotemCount = currentCount;

        // ── Trigger gate (bypass for re-equip and low HP) ─────────────────
        // FIX: reEquipPending and lowHpBypass both skip the trigger check
        boolean bypassing = reEquipPending
                || (lowHpBypass.getValue() && mc.player.getHealth() <= bypassHp.getValue() * 2f);

        if (!bypassing && !isTriggerActive()) {
            invClock = -1;
            return;
        }

        // ── Auto-open inventory ───────────────────────────────────────────
        if (autoOpen.getValue() && needsTotem() && !(mc.currentScreen instanceof InventoryScreen)) {
            autoOpened = true; // FIX: mark that WE opened it
            mc.setScreen(new InventoryScreen(mc.player));
        }

        if (!(mc.currentScreen instanceof InventoryScreen invScreen)) {
            invClock = -1;
            return;
        }

        // FIX: if inventory was already open before we triggered, mark autoOpened=false
        // so we don't close it when done
        if (invClock == -1 && !autoOpened) {
            // player opened the inventory themselves, don't touch it
        }

        // ── Delay clock ───────────────────────────────────────────────────
        if (invClock == -1) {
            int min = (int) delay.getMinValue();
            int max = (int) delay.getMaxValue();
            invClock = min >= max ? min : (int) MathUtils.randomDoubleBetween(min, max + 1);
        }
        if (invClock > 0) { invClock--; return; }

        int syncId = invScreen.getScreenHandler().syncId;
        PlayerInventory inv = mc.player.getInventory();
        boolean didSwap = false;

        // ── Fill offhand ──────────────────────────────────────────────────
        if (fillOffhand.getValue()) {
            // FIX: removed dead offhandNeedsTotem variable — single clean condition
            if (!offhandIsTotem && (mc.player.getOffHandStack().isEmpty() || forceTotem.getValue())) {
                int src = findSourceSlot(totemSlot.getValueInt() - 1,
                        fillSlot2.getValue() ? totemSlot2.getValueInt() - 1 : Integer.MIN_VALUE);
                if (src != -1) {
                    mc.interactionManager.clickSlot(syncId, src, 40, SlotActionType.SWAP, mc.player);
                    swapCooldownTick = swapCooldown.getValueInt();
                    reEquipPending = false;
                    didSwap = true;
                }
            }
        }

        // ── Fill primary hotbar slot ──────────────────────────────────────
        if (!didSwap && fillHotbar.getValue()) {
            int target = totemSlot.getValueInt() - 1;
            boolean needs = inv.main.get(target).isEmpty()
                    || (forceTotem.getValue() && !inv.main.get(target).isOf(Items.TOTEM_OF_UNDYING));
            if (needs) {
                int src = findSourceSlot(target,
                        fillSlot2.getValue() ? totemSlot2.getValueInt() - 1 : Integer.MIN_VALUE);
                if (src != -1) {
                    mc.interactionManager.clickSlot(syncId, src, target, SlotActionType.SWAP, mc.player);
                    swapCooldownTick = swapCooldown.getValueInt();
                    didSwap = true;
                }
            }
        }

        // ── Fill second hotbar slot ───────────────────────────────────────
        if (!didSwap && fillSlot2.getValue()) {
            int target2 = totemSlot2.getValueInt() - 1;
            boolean needs2 = inv.main.get(target2).isEmpty()
                    || (forceTotem.getValue() && !inv.main.get(target2).isOf(Items.TOTEM_OF_UNDYING));
            if (needs2) {
                int src = findSourceSlot(target2, totemSlot.getValueInt() - 1);
                if (src != -1) {
                    mc.interactionManager.clickSlot(syncId, src, target2, SlotActionType.SWAP, mc.player);
                    swapCooldownTick = swapCooldown.getValueInt();
                }
            }
        }

        reEquipPending = false;

        // ── Auto-close only if WE opened it and we're done ────────────────
        // FIX: use autoOpened flag instead of !wasOpen which was always true
        if (autoOpened && !needsTotem()) {
            mc.setScreen(null);
            autoOpened = false;
            invClock = -1;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isTriggerActive() {
        if (mc.player == null) return false;
        return switch (triggerMode.getMode()) {
            case "OnKey"   -> KeyUtils.isKeyPressed(activateKey.getKeyCode());
            case "OnLowHP" -> mc.player.getHealth() <= hpThreshold.getValue() * 2f;
            default        -> true;
        };
    }

    private boolean needsTotem() {
        if (mc.player == null) return false;
        boolean offMissing   = fillOffhand.getValue()
                && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        boolean slot1Missing = fillHotbar.getValue()
                && !mc.player.getInventory().getStack(totemSlot.getValueInt() - 1).isOf(Items.TOTEM_OF_UNDYING);
        boolean slot2Missing = fillSlot2.getValue()
                && !mc.player.getInventory().getStack(totemSlot2.getValueInt() - 1).isOf(Items.TOTEM_OF_UNDYING);
        return (offMissing || slot1Missing || slot2Missing) && countTotems() > 0;
    }

    /**
     * FIX: removed unused syncId param. Searches main inventory (9-35) first,
     * falls back to hotbar (0-8). Skips any slot in excludeSlots.
     * Uses Integer.MIN_VALUE as a sentinel for "no second exclude".
     */
    private int findSourceSlot(int... excludeSlots) {
        for (int i = 9; i < 36; i++) {
            if (isExcluded(i, excludeSlots)) continue;
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (isExcluded(i, excludeSlots)) continue;
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }

    // FIX: guard against Integer.MIN_VALUE sentinel so it never matches a real slot
    private boolean isExcluded(int slot, int... excludes) {
        for (int e : excludes) if (e != Integer.MIN_VALUE && e == slot) return true;
        return false;
    }

    private int countTotems() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) count++;
        }
        return count;
    }
}
