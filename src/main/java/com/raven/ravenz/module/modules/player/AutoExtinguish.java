package com.raven.ravenz.module.modules.player;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.RangeSetting;
import com.raven.ravenz.utils.math.MathUtils;
import com.raven.ravenz.utils.math.TimerUtil;
import com.raven.ravenz.utils.mc.InventoryUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public final class AutoExtinguish extends Module {
    public final RangeSetting delayMS = new RangeSetting("Delay (MS)", 0, 1200, 250, 330, 0.5);
    public final BooleanSetting pickUP = new BooleanSetting("Pick up after", false);
    public final BooleanSetting rotateBack = new BooleanSetting("Rotate back", false);
    public final BooleanSetting toPrevSlot = new BooleanSetting("Goto prev-slot", false);

    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil delay = new TimerUtil();
    private boolean isExtinguishing = false;
    private float originalPitch;
    private int originalSlot;
    private long currentDelay;
    private State currentState = State.READY;

    public AutoExtinguish() {
        super("Auto Extinguish", "Automatically places water to extinguish you", -1, Category.PLAYER);
        this.addSettings(delayMS, pickUP, rotateBack, toPrevSlot);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull())
            return;

        if (mc.player.isOnFire()) {
            if (!isExtinguishing) {
                startExtinguishing();
            } else {
                handleExtinguishing();
            }
        } else if (isExtinguishing) {
            finishExtinguishing();
        }
    }

    private void startExtinguishing() {
        isExtinguishing = true;
        originalPitch = mc.player.getPitch();
        originalSlot = mc.player.getInventory().selectedSlot;

        long min = (long) delayMS.getMinValue();
        long max = (long) delayMS.getMaxValue();

        currentDelay = (long) MathUtils.randomDoubleBetween(min, max);

        currentState = State.SWITCHING;
        timer.reset();
        delay.reset();
    }

    private void handleExtinguishing() {
        if (timer.hasElapsedTime(currentDelay)) {
            switch (currentState) {
                case SWITCHING:
                    InventoryUtil.swapToSlot(Items.WATER_BUCKET);
                    currentState = State.AIMING;
                    timer.reset();
                    break;

                case AIMING:
                    mc.player.setPitch(89.9f);
                    currentState = State.PLACING;
                    timer.reset();
                    break;

                case PLACING:
                    InventoryUtil.swapToSlot(Items.WATER_BUCKET);
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    currentState = pickUP.getValue() ? State.PICKING_UP : State.RETURNING;
                    delay.reset();  // reset pickup delay timer when entering PICKING_UP
                    timer.reset();
                    break;

                case PICKING_UP:
                    if (delay.hasElapsedTime(100)) {
                        ((MinecraftClientAccessor) mc).invokeDoItemUse();
                        currentState = State.RETURNING;
                        timer.reset();
                    }
                    break;

                case RETURNING:
                    if (toPrevSlot.getValue()) {
                        mc.player.getInventory().selectedSlot = originalSlot;
                    }
                    if (rotateBack.getValue()) {
                        mc.player.setPitch(originalPitch);
                    }
                    isExtinguishing = false;
                    currentState = State.READY;
                    this.setEnabled(false);  // safe disable without double-calling finishExtinguishing
                    break;
            }
        }
    }

    private void finishExtinguishing() {
        isExtinguishing = false;
        if (toPrevSlot.getValue()) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
        if (rotateBack.getValue()) {
            mc.player.setPitch(originalPitch);
        }
        currentState = State.READY;
    }

    @Override
    public void onEnable() {
        delay.reset();
        timer.reset();
        isExtinguishing = false;
        currentState = State.READY;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        delay.reset();
        timer.reset();
        if (isExtinguishing) {
            finishExtinguishing();
        }
        super.onDisable();
    }

    private enum State {
        READY,
        SWITCHING,
        AIMING,
        PLACING,
        PICKING_UP,
        RETURNING
    }
}