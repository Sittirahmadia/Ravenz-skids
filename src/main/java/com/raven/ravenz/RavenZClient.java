package com.raven.ravenz;

import com.raven.ravenz.module.ModuleManager;
import com.raven.ravenz.module.events.MouseModuleHandler;
import com.raven.ravenz.profiles.ProfileManager;
import com.raven.ravenz.utils.jvm.ModMenuHider;
import com.raven.ravenz.utils.notification.NotificationManager;
import com.raven.ravenz.utils.render.font.FontManager;
import io.github.racoondog.norbit.EventBus;
import java.lang.invoke.MethodHandles;
import lombok.Getter;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public final class RavenZClient implements ClientModInitializer {

    public static final String CLIENT_VERSION = "0.2.10";
    public static final boolean shouldUseMouseEvent = System.getProperty(
        "os.name"
    )
        .toLowerCase()
        .contains("windows");
    public static RavenZClient INSTANCE;
    public static MinecraftClient mc;
    public final IEventBus eventBus;
    public final ModuleManager moduleManager;
    public final FontManager fontManager;
    public final ProfileManager profileManager;
    public final MouseModuleHandler mouseModuleHandler;
    public final NotificationManager notificationManager;
    private final Logger logger = LoggerFactory.getLogger("Krypton");

    public RavenZClient() {
        INSTANCE = this;
        mc = MinecraftClient.getInstance();
        eventBus = EventBus.threadSafe();
        eventBus.registerLambdaFactory("com.raven.ravenz", (lookupInMethod, klass) ->
            (MethodHandles.Lookup) lookupInMethod.invoke(
                null,
                klass,
                MethodHandles.lookup()
            )
        );

        this.moduleManager = new ModuleManager();
        this.fontManager = new FontManager();
        this.profileManager = new ProfileManager();
        this.mouseModuleHandler = new MouseModuleHandler();
        this.notificationManager = NotificationManager.getInstance();

        // Mark AutoSaveManager as ready - all static module fields are now initialized
        com.raven.ravenz.utils.AutoSaveManager.markReady();

        eventBus.subscribe(mouseModuleHandler);
        eventBus.subscribe(notificationManager);

        new Thread(() -> {
            try {
                ModMenuHider.hideFromModMenu();
                Thread.sleep(1000);
                ModMenuHider.hideFromModMenu();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        })
            .start();
    }

    @Override
    public void onInitializeClient() {
        // Double initialization prevention, it's already initializing in the constructor
    }
}
