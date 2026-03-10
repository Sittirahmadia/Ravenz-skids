package com.raven.ravenz.module.modules.player;

import com.raven.ravenz.event.impl.network.PacketEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class PingSpoof extends Module {
    private final NumberSetting msDelay = new NumberSetting("Ms", 1, 500, 60, 1);

    // Single-threaded scheduler so keep-alive packets are sent in order
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PingSpoof");
                t.setDaemon(true);
                return t;
            });

    public PingSpoof() {
        super("Ping Spoof", "Increases your ping by delaying keep-alive responses", -1, Category.PLAYER);
        this.addSetting(msDelay);
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof KeepAliveS2CPacket packet)) return;
        if (isNull()) return;
        // Only intercept inbound (server→client) keep-alive
        if (event.getOrder() != com.raven.ravenz.event.types.TransferOrder.RECEIVE) return;

        // Cancel original so vanilla handler doesn't respond immediately
        event.cancel();

        long delay = Math.max(1L, (long) msDelay.getValue());
        scheduler.schedule(() -> {
            try {
                var handler = mc.getNetworkHandler();
                if (handler != null) {
                    Objects.requireNonNull(handler).getConnection()
                            .send(new KeepAliveC2SPacket(packet.getId()));
                }
            } catch (Exception ignored) {}
        }, delay, TimeUnit.MILLISECONDS);
    }
}
