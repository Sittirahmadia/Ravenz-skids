package com.raven.ravenz.module.setting;

import org.lwjgl.glfw.GLFW;

public class KeybindSetting extends Setting {

    private int     keyCode;
    private boolean holdMode;
    private boolean listening;
    private boolean prevState;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Full constructor: name, default key, holdMode */
    public KeybindSetting(String name, int defaultKey, boolean holdMode) {
        super(name);
        this.keyCode   = defaultKey;
        this.holdMode  = holdMode;
        this.listening = false;
        this.prevState = false;
    }

    /** Constructor with key, defaults holdMode to false */
    public KeybindSetting(String name, int defaultKey) {
        this(name, defaultKey, false);
    }

    /** Constructor with no key bound (-1 = None), holdMode false */
    public KeybindSetting(String name) {
        this(name, -1, false);
    }

    // ── Key code ──────────────────────────────────────────────────────────────

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isNone() {
        return keyCode == -1 || keyCode == GLFW.GLFW_KEY_UNKNOWN;
    }

    // ── Hold mode ─────────────────────────────────────────────────────────────

    public boolean isHoldMode() {
        return holdMode;
    }

    public void setHoldMode(boolean holdMode) {
        this.holdMode = holdMode;
    }

    public void toggleHoldMode() {
        this.holdMode = !this.holdMode;
    }

    // ── Listening (GUI rebind state) ──────────────────────────────────────────

    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public void toggleListening() {
        this.listening = !this.listening;
    }

    // ── Press detection ───────────────────────────────────────────────────────

    /**
     * Returns true on rising edge of key press (for toggle mode).
     * Call once per tick.
     */
    public boolean isPressed(long windowHandle) {
        if (isNone()) return false;
        boolean current = getRawState(windowHandle);
        boolean risingEdge = current && !prevState;
        prevState = current;
        return risingEdge;
    }

    /**
     * Returns true while the key is held down (for hold mode).
     */
    public boolean isHeld(long windowHandle) {
        if (isNone()) return false;
        return getRawState(windowHandle);
    }

    /**
     * Returns true based on current mode:
     * - Hold mode  → true while held
     * - Toggle mode → true on rising edge only
     */
    public boolean isActive(long windowHandle) {
        return holdMode ? isHeld(windowHandle) : isPressed(windowHandle);
    }

    private boolean getRawState(long windowHandle) {
        // Mouse buttons: GLFW_MOUSE_BUTTON_1 (0) through GLFW_MOUSE_BUTTON_8 (7)
        if (keyCode >= GLFW.GLFW_MOUSE_BUTTON_1 && keyCode <= GLFW.GLFW_MOUSE_BUTTON_8) {
            return GLFW.glfwGetMouseButton(windowHandle, keyCode) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
    }

    // ── Display name ──────────────────────────────────────────────────────────

    public String getKeyName() {
        if (isNone()) return "None";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_1) return "LMB";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_2) return "RMB";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_3) return "MMB";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_4) return "Mouse4";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_5) return "Mouse5";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_6) return "Mouse6";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_7) return "Mouse7";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_8) return "Mouse8";
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        return name != null ? name.toUpperCase() : "Key" + keyCode;
    }

    @Override
    public String toString() {
        return getName() + ": " + getKeyName() + (holdMode ? " [Hold]" : " [Toggle]");
    }
}
