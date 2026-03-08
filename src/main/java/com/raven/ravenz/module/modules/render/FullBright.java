package com.raven.ravenz.module.modules.render;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullBright extends Module {
    private static final double TARGET_GAMMA = 16.0;
    private static final int NIGHT_VISION_DURATION = 220;

    private Double previousGamma = null;

    public FullBright() {
        super("Full Bright", "Removes darkness", Category.RENDER);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        applyGamma();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        applyGamma();
        applyNightVision();
    }

    private void applyGamma() {
        if (mc != null && mc.options != null) {
            try {
                SimpleOption<Double> gamma = mc.options.getGamma();
                if (previousGamma == null) {
                    previousGamma = gamma.getValue();
                }
                gamma.setValue(TARGET_GAMMA);
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyNightVision() {
        if (mc == null || mc.player == null) return;
        try {
            mc.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION,
                    NIGHT_VISION_DURATION,
                    0,
                    false,
                    false
            ));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc != null && mc.options != null && previousGamma != null) {
            try {
                mc.options.getGamma().setValue(previousGamma);
            } catch (Throwable ignored) {
            }
        }
        previousGamma = null;
    }
}
