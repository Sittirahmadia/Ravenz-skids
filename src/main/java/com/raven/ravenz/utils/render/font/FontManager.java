package com.raven.ravenz.utils.render.font;


import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.utils.render.font.fonts.FontRenderer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FontManager {

    private final Map<FontKey, FontRenderer> fontCache = new HashMap<>();

    public void initialize() {
        for (Type type : Type.values()) {
            for (int size = 4; size <= 32; size++) {
                fontCache.put(new FontKey(size, type), create(size, type.getType()));
            }
        }
    }

    @SneakyThrows
    public FontRenderer create(float size, String name) {
        String fileName = name.endsWith(".ttf") || name.endsWith(".otf") ? name : name + ".ttf";
        String path = "assets/krypton/fonts/" + fileName;

        try (InputStream inputStream = RavenZClient.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing font resource: " + path);
            }
            Font[] font = Font.createFonts(Objects.requireNonNull(inputStream));

            return new FontRenderer(font, size, 256, 2);
        }
    }

    public FontRenderer getSize(int size, Type type) {
        return fontCache.computeIfAbsent(new FontKey(size, type), k -> create(size, type.getType()));
    }

    @Getter
    public enum Type {
        Inter("Inter"),
        JetbrainsMono("JetbrainsMono"),
        Poppins("Poppins-Medium");

        private final String type;

        Type(String type) {
            this.type = type;
        }
    }

    private record FontKey(int size, Type type) {

        @Override
        public @NotNull String toString() {
            return "FontKey[" +
                    "size=" + size + ", " +
                    "type=" + type + ']';
        }

    }
}
