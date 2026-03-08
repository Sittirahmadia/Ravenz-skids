package com.raven.ravenz.gui.theme;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Simple Theme registry. Register default themes here and use ThemeManager.getTheme(name) to retrieve.
 */
public final class ThemeManager {
    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();
    private static final String DEFAULT_NAME;

    static {
        // Default theme: matches previous GUI colours
        Theme defaultTheme = new Theme("Default",
                new Color(0, 0, 0, 200),
                new Color(8, 8, 10, 134),
                new Color(12, 12, 12, 200),
                new Color(18, 18, 20, 255),
                new Color(16, 16, 18, 255),
                new Color(18, 18, 20, 200),
                new Color(28, 28, 32, 200),
                new Color(74, 144, 255, 255),
                Color.WHITE,
                new Color(180, 180, 200));

        Theme blue = new Theme("Blue",
                new Color(4, 10, 20, 200),
                new Color(10, 22, 44, 160),
                new Color(10, 14, 24, 220),
                new Color(12, 22, 38, 240),
                new Color(10, 18, 30, 230),
                new Color(12, 20, 34, 200),
                new Color(18, 30, 50, 200),
                new Color(74, 144, 255, 255),
                Color.WHITE,
                new Color(190, 210, 235));

        Theme solar = new Theme("Solar",
                new Color(10, 16, 10, 200),
                new Color(32, 38, 22, 160),
                new Color(20, 24, 18, 200),
                new Color(34, 36, 26, 255),
                new Color(28, 30, 22, 255),
                new Color(22, 26, 18, 200),
                new Color(30, 34, 24, 200),
                new Color(180, 133, 0, 255),
                Color.WHITE,
                new Color(200, 200, 180));

        register(defaultTheme);
        register(blue);
        register(solar);

        DEFAULT_NAME = defaultTheme.name();
    }

    public static void register(Theme theme) {
        THEMES.put(theme.name(), theme);
    }

    public static Theme getTheme(String name) {
        return THEMES.getOrDefault(name, THEMES.values().iterator().next());
    }

    public static String[] getThemeNamesArray() {
        List<String> keys = new ArrayList<>(THEMES.keySet());
        return keys.toArray(new String[0]);
    }

    public static List<Theme> getAllThemes() {
        return Collections.unmodifiableList(new ArrayList<>(THEMES.values()));
    }

    public static String getDefaultName() {
        return DEFAULT_NAME;
    }
}
