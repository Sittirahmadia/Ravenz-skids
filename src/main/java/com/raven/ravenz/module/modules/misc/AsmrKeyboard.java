package com.raven.ravenz.module.modules.misc;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * AsmrKeyboard — plays keyboard/mouse click sounds with per-key toggles.
 */
public class AsmrKeyboard extends Module {

    private final BooleanSetting leftClick  = new BooleanSetting("Left Click",  true);
    private final BooleanSetting rightClick = new BooleanSetting("Right Click", true);

    private final BooleanSetting keyW     = new BooleanSetting("Key W",    true);
    private final BooleanSetting keyA     = new BooleanSetting("Key A",    true);
    private final BooleanSetting keyS     = new BooleanSetting("Key S",    true);
    private final BooleanSetting keyD     = new BooleanSetting("Key D",    true);
    private final BooleanSetting keySpace = new BooleanSetting("Space",    true);
    private final BooleanSetting keyShift = new BooleanSetting("Shift",    false);
    private final BooleanSetting keyE     = new BooleanSetting("Key E",    false);
    private final BooleanSetting keyQ     = new BooleanSetting("Key Q",    false);
    private final BooleanSetting keyR     = new BooleanSetting("Key R",    false);
    private final BooleanSetting keyF     = new BooleanSetting("Key F",    false);

    private final BooleanSetting arrowKeys  = new BooleanSetting("Arrow Keys",  false);
    private final BooleanSetting hotbarKeys = new BooleanSetting("Hotbar 1-9",  false);

    private final ModeSetting sound = new ModeSetting("Sound",
            "UI Click", "UI Click", "Tripwire", "Note Hat", "Note Snare", "Note Pling");
    private final NumberSetting volume = new NumberSetting("Volume", 5, 100, 70, 5);
    private final NumberSetting pitch  = new NumberSetting("Pitch",  60, 150, 95, 5);

    // Previous key states
    private boolean prevLeft, prevRight;
    private boolean prevW, prevA, prevS, prevD, prevSpace, prevShift;
    private boolean prevE, prevQ, prevR, prevF;
    private boolean prevArUp, prevArDown, prevArLeft, prevArRight;
    private boolean prev1, prev2, prev3, prev4, prev5, prev6, prev7, prev8, prev9;

    private long nextPlayMs = 0L;

    public AsmrKeyboard() {
        super("ASMR Keyboard", "Plays sounds on keyboard and mouse presses", -1, Category.MISC);
        addSettings(leftClick, rightClick,
                keyW, keyA, keyS, keyD, keySpace, keyShift,
                keyE, keyQ, keyR, keyF,
                arrowKeys, hotbarKeys,
                sound, volume, pitch);
    }

    @Override
    public void onDisable() {
        prevLeft = prevRight = false;
        prevW = prevA = prevS = prevD = prevSpace = prevShift = false;
        prevE = prevQ = prevR = prevF = false;
        prevArUp = prevArDown = prevArLeft = prevArRight = false;
        prev1 = prev2 = prev3 = prev4 = prev5 = prev6 = prev7 = prev8 = prev9 = false;
        super.onDisable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;
        long handle = mc.getWindow().getHandle();

        prevLeft  = handle(leftClick.getValue(),  btn(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT),  prevLeft,  1.03f);
        prevRight = handle(rightClick.getValue(), btn(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT), prevRight, 0.90f);

        prevW     = handle(keyW.getValue(),     key(handle, GLFW.GLFW_KEY_W),     prevW,     1.00f);
        prevA     = handle(keyA.getValue(),     key(handle, GLFW.GLFW_KEY_A),     prevA,     0.98f);
        prevS     = handle(keyS.getValue(),     key(handle, GLFW.GLFW_KEY_S),     prevS,     0.97f);
        prevD     = handle(keyD.getValue(),     key(handle, GLFW.GLFW_KEY_D),     prevD,     1.01f);
        prevSpace = handle(keySpace.getValue(), key(handle, GLFW.GLFW_KEY_SPACE), prevSpace, 0.86f);
        prevShift = handle(keyShift.getValue(),
                key(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || key(handle, GLFW.GLFW_KEY_RIGHT_SHIFT),
                prevShift, 0.93f);

        prevE = handle(keyE.getValue(), key(handle, GLFW.GLFW_KEY_E), prevE, 1.02f);
        prevQ = handle(keyQ.getValue(), key(handle, GLFW.GLFW_KEY_Q), prevQ, 1.01f);
        prevR = handle(keyR.getValue(), key(handle, GLFW.GLFW_KEY_R), prevR, 0.99f);
        prevF = handle(keyF.getValue(), key(handle, GLFW.GLFW_KEY_F), prevF, 1.00f);

        if (arrowKeys.getValue()) {
            prevArUp    = handle(true, key(handle, GLFW.GLFW_KEY_UP),    prevArUp,    0.98f);
            prevArDown  = handle(true, key(handle, GLFW.GLFW_KEY_DOWN),  prevArDown,  0.98f);
            prevArLeft  = handle(true, key(handle, GLFW.GLFW_KEY_LEFT),  prevArLeft,  0.98f);
            prevArRight = handle(true, key(handle, GLFW.GLFW_KEY_RIGHT), prevArRight, 0.98f);
        } else {
            prevArUp    = key(handle, GLFW.GLFW_KEY_UP);
            prevArDown  = key(handle, GLFW.GLFW_KEY_DOWN);
            prevArLeft  = key(handle, GLFW.GLFW_KEY_LEFT);
            prevArRight = key(handle, GLFW.GLFW_KEY_RIGHT);
        }

        if (hotbarKeys.getValue()) {
            prev1 = handle(true, key(handle, GLFW.GLFW_KEY_1), prev1, 1.0f);
            prev2 = handle(true, key(handle, GLFW.GLFW_KEY_2), prev2, 1.0f);
            prev3 = handle(true, key(handle, GLFW.GLFW_KEY_3), prev3, 1.0f);
            prev4 = handle(true, key(handle, GLFW.GLFW_KEY_4), prev4, 1.0f);
            prev5 = handle(true, key(handle, GLFW.GLFW_KEY_5), prev5, 1.0f);
            prev6 = handle(true, key(handle, GLFW.GLFW_KEY_6), prev6, 1.0f);
            prev7 = handle(true, key(handle, GLFW.GLFW_KEY_7), prev7, 1.0f);
            prev8 = handle(true, key(handle, GLFW.GLFW_KEY_8), prev8, 1.0f);
            prev9 = handle(true, key(handle, GLFW.GLFW_KEY_9), prev9, 1.0f);
        } else {
            prev1 = key(handle, GLFW.GLFW_KEY_1);
            prev2 = key(handle, GLFW.GLFW_KEY_2);
            prev3 = key(handle, GLFW.GLFW_KEY_3);
            prev4 = key(handle, GLFW.GLFW_KEY_4);
            prev5 = key(handle, GLFW.GLFW_KEY_5);
            prev6 = key(handle, GLFW.GLFW_KEY_6);
            prev7 = key(handle, GLFW.GLFW_KEY_7);
            prev8 = key(handle, GLFW.GLFW_KEY_8);
            prev9 = key(handle, GLFW.GLFW_KEY_9);
        }
    }

    /** Returns the new state; plays sound on rising edge if enabled. */
    private boolean handle(boolean enabled, boolean down, boolean previous, float pitchAdjust) {
        if (enabled && down && !previous) playSound(pitchAdjust);
        return down;
    }

    private void playSound(float pitchAdjust) {
        long now = System.currentTimeMillis();
        if (now < nextPlayMs) return;
        nextPlayMs = now + 12L;

        float vol       = (float) volume.getValue() / 100.0f;
        float basePitch = (float) pitch.getValue()  / 100.0f;
        float realPitch = Math.max(0.5f, Math.min(2.0f, basePitch * pitchAdjust));

        SoundEvent ev = resolveSound();
        mc.getSoundManager().play(PositionedSoundInstance.master(ev, vol, realPitch));
    }

    private SoundEvent resolveSound() {
        return switch (sound.getMode()) {
            case "Tripwire"   -> SoundEvents.BLOCK_TRIPWIRE_CLICK_ON;
            case "Note Hat"   -> SoundEvents.BLOCK_NOTE_BLOCK_HAT.value();
            case "Note Snare" -> SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value();
            case "Note Pling" -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            default           -> SoundEvents.UI_BUTTON_CLICK.value();
        };
    }

    private static boolean key(long handle, int keyCode) {
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }

    private static boolean btn(long handle, int button) {
        return GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
    }
}
