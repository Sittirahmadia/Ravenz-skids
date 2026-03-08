package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class SpearSwap extends Module {

    private final NumberSetting switchBackDelay = new NumberSetting("Switch Back Delay", 0, 200, 30, 1);

    private int originalSlot = -1;
    private int swappedSlot = -1;
    private long swapTime = 0;
    private boolean shouldSwitchBack = false;
    private boolean attackPressedLastTick = false;

    public SpearSwap() {
        super("Auto Lunge", "Swaps to a lunge spear on left click then switches back", Category.COMBAT);
        addSettings(switchBackDelay);
    }

    @EventHandler
    private void onHandleInput(HandleInputEvent event) {
        if (isNull()) {
            resetSwapState();
            attackPressedLastTick = false;
            return;
        }

        if (shouldSwitchBack && System.currentTimeMillis() - swapTime >= switchBackDelay.getValueInt()) {
            if (originalSlot >= 0 && originalSlot < 9 && mc.player.getInventory().selectedSlot == swappedSlot) {
                mc.player.getInventory().selectedSlot = originalSlot;
            }
            resetSwapState();
        }

        boolean attackPressed = mc.options.attackKey.isPressed();
        if (mc.currentScreen != null || !attackPressed || attackPressedLastTick) {
            attackPressedLastTick = attackPressed;
            return;
        }

        attackPressedLastTick = true;

        int spearSlot = findLungeSpearSlot();
        if (spearSlot == -1) {
            return;
        }

        int currentSlot = mc.player.getInventory().selectedSlot;
        if (currentSlot == spearSlot) {
            return;
        }

        originalSlot = currentSlot;
        swappedSlot = spearSlot;
        mc.player.getInventory().selectedSlot = spearSlot;
        swapTime = System.currentTimeMillis();
        shouldSwitchBack = true;
    }

    private int findLungeSpearSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isSpear(stack) && hasLungeEnchantment(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        var id = Registries.ITEM.getId(stack.getItem());
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        return path.equals("spear") || path.endsWith("_spear") || path.contains("spear");
    }

    private boolean hasLungeEnchantment(ItemStack stack) {
        return stack.getEnchantments().getEnchantments().stream()
                .anyMatch(enchantment -> {
                    String enchantmentId = enchantment.getIdAsString().toLowerCase();
                    return enchantmentId.contains("lunge") || enchantmentId.contains("lung");
                });
    }

    private void resetSwapState() {
        originalSlot = -1;
        swappedSlot = -1;
        swapTime = 0;
        shouldSwitchBack = false;
    }

    @Override
    public void onDisable() {
        resetSwapState();
        attackPressedLastTick = false;
    }
}
