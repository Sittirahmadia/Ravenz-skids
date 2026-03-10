package com.raven.ravenz.event.impl.input;

import com.raven.ravenz.event.types.Event;

public record MouseClickEvent(int button, int action, int mods) implements Event {

}