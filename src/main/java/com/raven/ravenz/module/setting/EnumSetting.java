package com.raven.ravenz.module.setting;

public class EnumSetting<T extends Enum<T>> extends Setting {

    private T value;
    private final T[] values;

    public EnumSetting(String name, T defaultValue) {
        super(name);
        this.value  = defaultValue;
        // Gets all possible enum constants from the default value's class
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    /**
     * Returns the current enum value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets the current enum value.
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Cycles to the next enum constant (wraps around).
     * Useful for GUI click-to-cycle behaviour.
     */
    public void cycle() {
        int next = (value.ordinal() + 1) % values.length;
        value = values[next];
    }

    /**
     * Cycles to the previous enum constant (wraps around).
     */
    public void cycleBack() {
        int prev = (value.ordinal() - 1 + values.length) % values.length;
        value = values[prev];
    }

    /**
     * Returns all possible enum constants for this setting.
     */
    public T[] getValues() {
        return values;
    }

    /**
     * Sets value by name string (case-insensitive). Used for config loading.
     */
    public void setValueByName(String name) {
        for (T v : values) {
            if (v.name().equalsIgnoreCase(name)) {
                value = v;
                return;
            }
        }
    }

    @Override
    public String toString() {
        return getName() + ": " + value.name();
    }
}
