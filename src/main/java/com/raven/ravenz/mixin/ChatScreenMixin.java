package com.raven.ravenz.mixin;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.event.impl.chat.ChatEvent;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        ChatEvent event = new ChatEvent(chatText);
        RavenZClient.INSTANCE.getEventBus().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}