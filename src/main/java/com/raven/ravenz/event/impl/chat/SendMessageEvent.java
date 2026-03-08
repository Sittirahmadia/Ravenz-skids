package com.raven.ravenz.event.impl.chat;

import com.raven.ravenz.event.types.CancellableEvent;

public class SendMessageEvent extends CancellableEvent {
    private final String message;

    public SendMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
