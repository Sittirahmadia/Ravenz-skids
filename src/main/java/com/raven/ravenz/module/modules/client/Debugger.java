package com.raven.ravenz.module.modules.client;

import com.raven.ravenz.event.impl.network.PacketEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.utils.mc.ChatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;

public class Debugger extends Module {
    public Debugger() {
        super("Debugger", "Debugs inv packets (dev purposes)", -1, Category.CLIENT);
    }

    @EventHandler
    public void onPacketSend(PacketEvent e) {
        if (isNull()) return;
        if (e.getPacket() == null) return;
        if (!(e.getPacket() instanceof ClickSlotC2SPacket packet))
            return;

        ChatUtil.addChatMessage("""
                ClickSlotPacket
                  syncId: %s
                  revision: %s
                  slot: %s
                  button: %s
                  actionType: %s
                  modifiedItems: %s
                  cursor: %s
                """.formatted(
                packet.syncId(),
                packet.revision(),
                packet.slot(),
                packet.button(),
                packet.actionType(),
                packet.modifiedStacks(),
                packet.cursor()
        ));
    }
}
