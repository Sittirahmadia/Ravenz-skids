package com.raven.ravenz.module.modules.misc;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import com.raven.ravenz.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

public final class WindChargeKey extends Module {
    private final KeybindSetting windChargeKeybind = new KeybindSetting("Wind Charge Key", GLFW.GLFW_KEY_G, true);
    private final NumberSetting throwDelay = new NumberSetting("Throw Delay", 50, 1000, 200, 25);
    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 0, 500, 50, 10);
    private final TimerUtil throwTimer = new TimerUtil();
    private final TimerUtil switchTimer = new TimerUtil();
    private boolean keyPressed = false;
    private int originalSlot = -1;
    private boolean needsSwitchBack = false;

    public WindChargeKey() {
        super("Wind Charge Key", "Automatically throws wind charges", -1, Category.MISC);
        this.addSettings(windChargeKeybind, throwDelay, switchDelay);
        this.getSettings().removeIf(setting -> setting instanceof KeybindSetting && !setting.equals(windChargeKeybind));
    }

    @EventHandler
    private void onTickEvent(HandleInputEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        boolean currentKeyState = KeyUtils.isKeyPressed(windChargeKeybind.getKeyCode());

        if (currentKeyState && !keyPressed && throwTimer.hasElapsedTime(throwDelay.getValueInt(), false)) {
            throwWindCharge();
            throwTimer.reset();
        }

        if (needsSwitchBack && switchTimer.hasElapsedTime(switchDelay.getValueInt())) {
            mc.player.getInventory().selectedSlot = originalSlot;
            needsSwitchBack = false;
            originalSlot = -1;
        }

        keyPressed = currentKeyState;
    }

    private void throwWindCharge() {
        int windChargeSlot = findWindChargeSlot();
        if (windChargeSlot == -1) return;

        if (mc.player.getItemCooldownManager().isCoolingDown(new net.minecraft.item.ItemStack(Items.WIND_CHARGE))) {
            return;
        }

        originalSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = windChargeSlot;
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        needsSwitchBack = true;
        switchTimer.reset();
    }

    private int findWindChargeSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.WIND_CHARGE) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onEnable() {
        keyPressed = false;
        originalSlot = -1;
        needsSwitchBack = false;
        throwTimer.reset();
        switchTimer.reset();
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