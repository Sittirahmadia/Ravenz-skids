package com.raven.ravenz.module;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.utils.IMinecraft;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.Setting;
import com.raven.ravenz.utils.notification.NotificationManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
public abstract class Module implements IMinecraft {

    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<Setting> settings = new ArrayList<>();
    private final Category moduleCategory;
    private final KeybindSetting keybindSetting;

    private final String name;
    private final String description;

    private String suffix;
    private boolean enabled;
    private int key;
    private boolean registered;

    public Module(String name, String description, int key, Category moduleCategory) {
        this.name = name;
        this.description = description;
        this.key = key;
        this.moduleCategory = moduleCategory;
        this.enabled = false;

        this.keybindSetting = new KeybindSetting("Keybind", key, true);
        addSetting(keybindSetting);
    }

    public Module(String name, String description, Category moduleCategory) {
        this(name, description, -1, moduleCategory);
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public boolean isNull() {
        return mc.player == null || mc.world == null;
    }

    public void addSetting(Setting setting) {
        settings.add(setting);
    }

    public void addSettings(Setting... settings) {
        this.settings.addAll(Arrays.asList(settings));
    }

    public int getKey() {
        if (isNull()) return key;
        return keybindSetting != null ? keybindSetting.getKeyCode() : key;
    }

    public void setKey(int key) {
        if (isNull()) return;
        this.key = key;
        if (keybindSetting != null) {
            keybindSetting.setKeyCode(key);
        }
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;

        this.enabled = enabled;

        if (enabled) {
            onEnable();
            if (this.enabled) {
                RavenZClient.INSTANCE.getEventBus().subscribe(this);
                registered = true;
                NotificationManager.getInstance().addModuleNotification(this, true);
            }
        } else {
            if (registered) {
                RavenZClient.INSTANCE.getEventBus().unsubscribe(this);
                registered = false;
            }
            onDisable();
            NotificationManager.getInstance().addModuleNotification(this, false);
        }
        
        com.raven.ravenz.utils.AutoSaveManager.getInstance().scheduleSave();
    }
}
