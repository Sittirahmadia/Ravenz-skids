package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

public final class XbowCart extends Module {

    private final KeybindSetting key = new KeybindSetting("Key", GLFW.GLFW_KEY_V, true);
    private final NumberSetting delay = new NumberSetting("Delay (ms)", 10, 500, 150, 25);

    private boolean pressed;
    private long lastAction;
    private State state = State.IDLE;

    public XbowCart() {
        super("Xbow cart", "Rail → TNT cart → F&S → crossbow", -1, Category.COMBAT);
        addSettings(key, delay);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        boolean keyDown = KeyUtils.isKeyPressed(key.getKeyCode());
        long now = System.currentTimeMillis();

        if (keyDown && !pressed && state == State.IDLE) {
            state = State.RAIL;
            lastAction = now;
        }
        pressed = keyDown;

        int d = delay.getValueInt();

        switch (state) {
            case IDLE -> {}
            case RAIL -> {
                if (now - lastAction >= d) {
                    if (switchToRail()) {
                        ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    }
                    state = State.CART;
                    lastAction = now;
                }
            }
            case CART -> {
                if (now - lastAction >= d) {
                    if (switchToItem(Items.TNT_MINECART)) {
                        ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    }
                    state = State.FIRE;
                    lastAction = now;
                }
            }
            case FIRE -> {
                if (now - lastAction >= d) {
                    if (switchToItem(Items.FLINT_AND_STEEL)) {
                        ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    }
                    state = State.CROSSBOW;
                    lastAction = now;
                }
            }
            case CROSSBOW -> {
                if (now - lastAction >= d) {
                    switchToItem(Items.CROSSBOW);
                    state = State.IDLE;
                }
            }
        }
    }

    private boolean switchToRail() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                net.minecraft.item.Item item = stack.getItem();
                if (item == Items.RAIL || item == Items.POWERED_RAIL ||
                    item == Items.DETECTOR_RAIL || item == Items.ACTIVATOR_RAIL) {
                    mc.player.getInventory().selectedSlot = i;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean switchToItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onEnable() {
        pressed = false;
        state = State.IDLE;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        super.onDisable();
    }

    private enum State { IDLE, RAIL, CART, FIRE, CROSSBOW }
}
