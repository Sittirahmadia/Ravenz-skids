package com.raven.ravenz.utils;

import com.raven.ravenz.RavenZClient;

import java.util.Timer;
import java.util.TimerTask;

public class AutoSaveManager {
    private static final AutoSaveManager INSTANCE = new AutoSaveManager();
    private static final long DEBOUNCE_DELAY = 1000; // 1 second delay

    /** Set to true by RavenZClient once all modules are fully initialized. */
    private static volatile boolean ready = false;

    private Timer timer;
    private TimerTask pendingTask;
    
    private AutoSaveManager() {
        this.timer = new Timer("AutoSave-Timer", true);
    }
    
    public static AutoSaveManager getInstance() {
        return INSTANCE;
    }

    /** Called by RavenZClient after all modules are loaded. */
    public static void markReady() {
        ready = true;
    }

    public void scheduleSave() {
        // Skip entirely if we're still in static initialization phase
        if (!ready) return;
        try {
            if (!com.raven.ravenz.module.modules.client.ClientSettingsModule.isAutoSaveEnabled()) {
                return;
            }
            
            if (RavenZClient.INSTANCE == null || RavenZClient.INSTANCE.getProfileManager() == null) {
                return;
            }
            
            if (pendingTask != null) {
                pendingTask.cancel();
            }
            
            pendingTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        RavenZClient.INSTANCE.getProfileManager().saveProfile("default", true);
                    } catch (Exception e) {
                    
                    }
                }
            };
            
            timer.schedule(pendingTask, DEBOUNCE_DELAY);
        } catch (Exception e) {
       
        }

    }
    
    public void shutdown() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
