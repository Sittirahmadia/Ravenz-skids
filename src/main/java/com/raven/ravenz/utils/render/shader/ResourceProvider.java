package com.raven.ravenz.utils.render.shader;

import net.minecraft.util.Identifier;

public final class ResourceProvider {
    public static Identifier getShaderIdentifier(String name) {
        return Identifier.of("krypton", "core/" + name);
    }
}
