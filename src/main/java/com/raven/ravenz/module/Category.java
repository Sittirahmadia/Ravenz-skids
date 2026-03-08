package com.raven.ravenz.module;

import lombok.Getter;

@Getter
public enum Category {
    COMBAT("Combat"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    MISC("Misc"),
    CLIENT("Client"),
    CONFIG("Config"),
    CREDITS("Credits");

    private final String name;

    Category(String name) {
        this.name = name;
    }
}
