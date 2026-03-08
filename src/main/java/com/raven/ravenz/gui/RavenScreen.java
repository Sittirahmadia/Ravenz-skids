package com.raven.ravenz.gui;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ColorSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.module.setting.RangeSetting;
import com.raven.ravenz.module.setting.Setting;
import com.raven.ravenz.module.setting.StringSetting;
import com.raven.ravenz.utils.AutoSaveManager;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RavenScreen extends Screen {

    private static final Identifier SMH_LOGO_ID = Identifier.of("raven-z-", "smh.png");
    private static final int RADIUS = 10;

    private static Identifier loadedLogoId;
    private static boolean triedLoadLogo;

    private static final int CHIP_PADDING = 12;
    private static final int CHIP_HEIGHT = 18;
    private static final int CHIP_SPACING = 8;

    private static final int MODULE_ROW_HEIGHT = 24;
    private static final int MODULE_ROW_SPACING = 4;

    private static final int SETTING_SPACING = 6;
    private static final int SETTING_NORMAL_HEIGHT = 28;
    private static final int SETTING_COLOR_HEIGHT = 64;

    private final List<Category> categories = new ArrayList<>();
    private final List<Module> visibleModules = new ArrayList<>();

    private final Map<Module, Rect> moduleRowBounds = new LinkedHashMap<>();
    private final Map<Module, Rect> moduleToggleBounds = new HashMap<>();

    private final Map<Setting, Rect> settingBounds = new LinkedHashMap<>();
    private final Map<BooleanSetting, Rect> booleanBounds = new HashMap<>();
    private final Map<ModeSetting, Rect> modeBounds = new HashMap<>();
    private final Map<NumberSetting, Rect> numberSliderBounds = new HashMap<>();
    private final Map<RangeSetting, Rect> rangeSliderBounds = new HashMap<>();
    private final Map<ColorSliderKey, Rect> colorSliderBounds = new HashMap<>();
    private final Map<KeybindSetting, Rect> holdModeBounds = new HashMap<>();
    private final Map<KeybindSetting, Rect> keybindBounds = new HashMap<>();
    private final Map<StringSetting, Rect> stringInputBounds = new HashMap<>();

    private Category selectedCategory;
    private Module selectedModule;

    private NumberSetting draggingNumberSetting;
    private RangeSetting draggingRangeSetting;
    private boolean draggingRangeMax;
    private ColorSetting draggingColorSetting;
    private int draggingColorChannel = -1;

    private KeybindSetting listeningKeybind;
    private StringSetting editingStringSetting;

    private int centerX;
    private int centerY;
    private int cardX;
    private int cardY;
    private int cardWidth;
    private int cardHeight;
    private int chipsY;

    private int modulePanelX;
    private int modulePanelY;
    private int modulePanelW;
    private int modulePanelH;
    private int moduleListX;
    private int moduleListY;
    private int moduleListW;
    private int moduleListH;

    private int settingsPanelX;
    private int settingsPanelY;
    private int settingsPanelW;
    private int settingsPanelH;
    private int settingsListX;
    private int settingsListY;
    private int settingsListW;
    private int settingsListH;

    private int moduleScroll;
    private int maxModuleScroll;
    private int settingsScroll;
    private int maxSettingsScroll;

    public RavenScreen() {
        super(Text.literal("RidhoXNoqwd"));
    }

    @Override
    protected void init() {
        loadCustomLogoIfNeeded();
        refreshCategories();
        updateLayout();
        rebuildHitboxes();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateLayout();
        rebuildHitboxes();

        context.fill(0, 0, width, height, 0xCC0A0A12);
        context.fillGradient(0, 0, width, height, 0x66000000, 0x00000000);

        drawCard(context);
        drawLogo(context);
        drawTitle(context);
        drawCategories(context, mouseX, mouseY);
        drawModulePanel(context, mouseX, mouseY);
        drawSettingsPanel(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void refreshCategories() {
        categories.clear();
        if (RavenZClient.INSTANCE != null && RavenZClient.INSTANCE.getModuleManager() != null) {
            for (Category category : Category.values()) {
                if (!RavenZClient.INSTANCE.getModuleManager().getModulesInCategory(category).isEmpty()) {
                    categories.add(category);
                }
            }
        }

        if (categories.isEmpty()) {
            categories.add(Category.COMBAT);
        }

        if (selectedCategory == null || !categories.contains(selectedCategory)) {
            selectedCategory = categories.get(0);
        }

        ensureSelectedModule();
    }

    private void ensureSelectedModule() {
        visibleModules.clear();
        if (RavenZClient.INSTANCE != null && RavenZClient.INSTANCE.getModuleManager() != null && selectedCategory != null) {
            visibleModules.addAll(RavenZClient.INSTANCE.getModuleManager().getModulesInCategory(selectedCategory));
        }

        if (selectedModule == null || !visibleModules.contains(selectedModule)) {
            selectedModule = visibleModules.isEmpty() ? null : visibleModules.get(0);
            settingsScroll = 0;
        }

        if (listeningKeybind != null && (selectedModule == null || !selectedModule.getSettings().contains(listeningKeybind))) {
            listeningKeybind = null;
        }

        if (editingStringSetting != null && (selectedModule == null || !selectedModule.getSettings().contains(editingStringSetting))) {
            editingStringSetting = null;
        }
    }

    private void updateLayout() {
        centerX = width / 2;
        centerY = height / 2;

        cardWidth = Math.min(width - 40, 760);
        cardHeight = Math.min(height - 40, 430);
        cardX = centerX - (cardWidth / 2);
        cardY = centerY - (cardHeight / 2);

        chipsY = cardY + 98;

        int bodyY = chipsY + CHIP_HEIGHT + 12;
        int bodyH = cardY + cardHeight - bodyY - 14;

        modulePanelX = cardX + 14;
        modulePanelY = bodyY;
        modulePanelW = Math.max(220, (cardWidth - 38) * 40 / 100);
        modulePanelH = bodyH;

        settingsPanelX = modulePanelX + modulePanelW + 10;
        settingsPanelY = bodyY;
        settingsPanelW = cardX + cardWidth - settingsPanelX - 14;
        settingsPanelH = bodyH;

        moduleListX = modulePanelX + 6;
        moduleListY = modulePanelY + 26;
        moduleListW = modulePanelW - 12;
        moduleListH = modulePanelH - 32;

        settingsListX = settingsPanelX + 8;
        settingsListY = settingsPanelY + 34;
        settingsListW = settingsPanelW - 16;
        settingsListH = settingsPanelH - 42;
    }

    private void rebuildHitboxes() {
        ensureSelectedModule();

        moduleRowBounds.clear();
        moduleToggleBounds.clear();

        settingBounds.clear();
        booleanBounds.clear();
        modeBounds.clear();
        numberSliderBounds.clear();
        rangeSliderBounds.clear();
        colorSliderBounds.clear();
        holdModeBounds.clear();
        keybindBounds.clear();
        stringInputBounds.clear();

        int totalModuleHeight = visibleModules.size() * (MODULE_ROW_HEIGHT + MODULE_ROW_SPACING);
        maxModuleScroll = Math.max(0, totalModuleHeight - moduleListH + 8);
        moduleScroll = MathHelper.clamp(moduleScroll, 0, maxModuleScroll);

        int rowY = moduleListY + 4 - moduleScroll;
        for (Module module : visibleModules) {
            Rect rowRect = new Rect(moduleListX, rowY, moduleListW, MODULE_ROW_HEIGHT);
            moduleRowBounds.put(module, rowRect);
            moduleToggleBounds.put(module, new Rect(rowRect.right() - 48, rowRect.y + 4, 42, rowRect.h - 8));
            rowY += MODULE_ROW_HEIGHT + MODULE_ROW_SPACING;
        }

        if (selectedModule == null) {
            maxSettingsScroll = 0;
            settingsScroll = 0;
            return;
        }

        int totalSettingsHeight = 0;
        for (Setting setting : selectedModule.getSettings()) {
            totalSettingsHeight += getSettingHeight(setting) + SETTING_SPACING;
        }

        maxSettingsScroll = Math.max(0, totalSettingsHeight - settingsListH + 8);
        settingsScroll = MathHelper.clamp(settingsScroll, 0, maxSettingsScroll);

        int y = settingsListY + 4 - settingsScroll;
        for (Setting setting : selectedModule.getSettings()) {
            int height = getSettingHeight(setting);
            Rect rowRect = new Rect(settingsListX, y, settingsListW, height);
            settingBounds.put(setting, rowRect);

            int controlX = rowRect.x + (rowRect.w / 2);
            int controlW = rowRect.w - (rowRect.w / 2) - 8;

            if (setting instanceof BooleanSetting booleanSetting) {
                booleanBounds.put(booleanSetting, new Rect(controlX + controlW - 38, rowRect.y + 6, 36, 16));
            } else if (setting instanceof ModeSetting modeSetting) {
                modeBounds.put(modeSetting, new Rect(controlX, rowRect.y + 6, controlW, 16));
            } else if (setting instanceof NumberSetting numberSetting) {
                numberSliderBounds.put(numberSetting, new Rect(controlX, rowRect.y + 14, controlW, 8));
            } else if (setting instanceof RangeSetting rangeSetting) {
                rangeSliderBounds.put(rangeSetting, new Rect(controlX, rowRect.y + 14, controlW, 8));
            } else if (setting instanceof ColorSetting colorSetting) {
                int channels = colorSetting.isHasAlpha() ? 4 : 3;
                int channelY = rowRect.y + 16;
                for (int channel = 0; channel < channels; channel++) {
                    colorSliderBounds.put(new ColorSliderKey(colorSetting, channel), new Rect(controlX, channelY + (channel * 12), controlW, 8));
                }
            } else if (setting instanceof KeybindSetting keybindSetting) {
                holdModeBounds.put(keybindSetting, new Rect(controlX, rowRect.y + 6, 52, 16));
                keybindBounds.put(keybindSetting, new Rect(controlX + 58, rowRect.y + 6, controlW - 58, 16));
            } else if (setting instanceof StringSetting stringSetting) {
                stringInputBounds.put(stringSetting, new Rect(controlX, rowRect.y + 6, controlW, 16));
            }

            y += height + SETTING_SPACING;
        }
    }

    private int getSettingHeight(Setting setting) {
        if (setting instanceof ColorSetting) {
            return SETTING_COLOR_HEIGHT;
        }
        return SETTING_NORMAL_HEIGHT;
    }

    private void drawCard(DrawContext context) {
        fillRoundedRect(context, cardX, cardY, cardWidth, cardHeight, RADIUS, 0xCC151526);
        fillRoundedRectOutline(context, cardX, cardY, cardWidth, cardHeight, RADIUS, 0xFF6B3AFF);
    }

    private void drawLogo(DrawContext context) {
        int size = 48;
        int x = centerX - (size / 2);
        int y = cardY + 14;

        if (loadedLogoId != null) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, loadedLogoId, x, y, 0f, 0f, size, size, size, size);
            return;
        }

        fillRoundedRect(context, x, y, size, size, 8, 0x66322244);
        String missing = "SMH";
        int w = textRenderer.getWidth(missing);
        context.drawText(textRenderer, missing, x + (size - w) / 2, y + 19, 0xFFEEC3FF, false);
    }

    private void loadCustomLogoIfNeeded() {
        if (triedLoadLogo) {
            return;
        }

        triedLoadLogo = true;
        loadedLogoId = SMH_LOGO_ID;
    }

    private void drawTitle(DrawContext context) {
        String title = "RidhoXNoqwd";
        String subtitle = "Integrated module control";
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, centerX - (titleWidth / 2), cardY + 66, 0xFFEEC3FF, false);
        int subtitleWidth = textRenderer.getWidth(subtitle);
        context.drawText(textRenderer, subtitle, centerX - (subtitleWidth / 2), cardY + 80, 0xFFC8B8FF, false);
    }

    private void drawCategories(DrawContext context, int mouseX, int mouseY) {
        int totalWidth = 0;
        for (Category category : categories) {
            totalWidth += textRenderer.getWidth(category.getName()) + (CHIP_PADDING * 2) + CHIP_SPACING;
        }
        totalWidth -= CHIP_SPACING;

        int x = centerX - (totalWidth / 2);
        for (Category category : categories) {
            int chipWidth = textRenderer.getWidth(category.getName()) + (CHIP_PADDING * 2);
            boolean hovered = mouseX >= x && mouseX <= x + chipWidth && mouseY >= chipsY && mouseY <= chipsY + CHIP_HEIGHT;
            boolean selected = selectedCategory == category;

            int backgroundColor = selected ? 0xFF6B3AFF : hovered ? 0x802F1F4A : 0x66151526;
            fillRoundedRect(context, x, chipsY, chipWidth, CHIP_HEIGHT, 9, backgroundColor);
            context.drawText(textRenderer, category.getName(), x + CHIP_PADDING, chipsY + 5, selected ? 0xFFFFFFFF : 0xFFEBD7FF, false);
            x += chipWidth + CHIP_SPACING;
        }
    }

    private void drawModulePanel(DrawContext context, int mouseX, int mouseY) {
        fillRoundedRect(context, modulePanelX, modulePanelY, modulePanelW, modulePanelH, 8, 0x55101018);
        context.drawText(textRenderer, selectedCategory.getName() + " Modules", modulePanelX + 8, modulePanelY + 8, 0xFFEBD7FF, false);

        context.enableScissor(moduleListX, moduleListY, moduleListX + moduleListW, moduleListY + moduleListH);
        for (Module module : visibleModules) {
            Rect row = moduleRowBounds.get(module);
            Rect toggle = moduleToggleBounds.get(module);
            if (row == null || toggle == null) {
                continue;
            }

            if (row.bottom() < moduleListY || row.y > moduleListY + moduleListH) {
                continue;
            }

            boolean hovered = row.contains(mouseX, mouseY);
            boolean selected = module == selectedModule;
            boolean enabled = module.isEnabled();

            int rowColor;
            if (selected) {
                rowColor = hovered ? 0xAA6B3AFF : 0x886B3AFF;
            } else if (enabled) {
                rowColor = hovered ? 0x803A2B59 : 0x66302447;
            } else {
                rowColor = hovered ? 0x5525253A : 0x44171726;
            }

            fillRoundedRect(context, row.x, row.y, row.w, row.h, 6, rowColor);

            int nameColor = selected ? 0xFFFFFFFF : enabled ? 0xFFE9E1F2 : 0xFFD3C7E0;
            context.drawText(textRenderer, module.getName(), row.x + 8, row.y + 8, nameColor, false);

            int toggleColor = enabled ? 0xFF5DFF8A : 0xFF806A90;
            fillRoundedRect(context, toggle.x, toggle.y, toggle.w, toggle.h, 5, enabled ? 0x5534A055 : 0x553D2E4D);
            String toggleText = enabled ? "ON" : "OFF";
            int toggleTextW = textRenderer.getWidth(toggleText);
            context.drawText(textRenderer, toggleText, toggle.x + (toggle.w - toggleTextW) / 2, toggle.y + 4, toggleColor, false);
        }
        context.disableScissor();
    }

    private void drawSettingsPanel(DrawContext context, int mouseX, int mouseY) {
        fillRoundedRect(context, settingsPanelX, settingsPanelY, settingsPanelW, settingsPanelH, 8, 0x55101018);

        if (selectedModule == null) {
            String empty = "No module selected";
            int w = textRenderer.getWidth(empty);
            context.drawText(textRenderer, empty, settingsPanelX + (settingsPanelW - w) / 2, settingsPanelY + settingsPanelH / 2, 0xFFBFA9FF, false);
            return;
        }

        String header = selectedModule.getName() + " Settings";
        context.drawText(textRenderer, header, settingsPanelX + 8, settingsPanelY + 8, 0xFFEBD7FF, false);

        String state = selectedModule.isEnabled() ? "Enabled" : "Disabled";
        int stateWidth = textRenderer.getWidth(state);
        context.drawText(textRenderer, state, settingsPanelX + settingsPanelW - 8 - stateWidth, settingsPanelY + 8,
                selectedModule.isEnabled() ? 0xFF9FFFB9 : 0xFFBFA9CF, false);

        context.enableScissor(settingsListX, settingsListY, settingsListX + settingsListW, settingsListY + settingsListH);

        for (Setting setting : selectedModule.getSettings()) {
            Rect row = settingBounds.get(setting);
            if (row == null) {
                continue;
            }

            if (row.bottom() < settingsListY || row.y > settingsListY + settingsListH) {
                continue;
            }

            boolean hovered = row.contains(mouseX, mouseY);
            fillRoundedRect(context, row.x, row.y, row.w, row.h, 5, hovered ? 0x55332745 : 0x44221A31);

            context.drawText(textRenderer, setting.getName(), row.x + 8, row.y + 7, 0xFFE8DDF5, false);

            if (setting instanceof BooleanSetting booleanSetting) {
                renderBooleanSetting(context, booleanSetting);
            } else if (setting instanceof ModeSetting modeSetting) {
                renderModeSetting(context, modeSetting);
            } else if (setting instanceof NumberSetting numberSetting) {
                renderNumberSetting(context, numberSetting);
            } else if (setting instanceof RangeSetting rangeSetting) {
                renderRangeSetting(context, rangeSetting);
            } else if (setting instanceof ColorSetting colorSetting) {
                renderColorSetting(context, colorSetting);
            } else if (setting instanceof KeybindSetting keybindSetting) {
                renderKeybindSetting(context, keybindSetting);
            } else if (setting instanceof StringSetting stringSetting) {
                renderStringSetting(context, stringSetting);
            }
        }

        context.disableScissor();
    }

    private void renderBooleanSetting(DrawContext context, BooleanSetting setting) {
        Rect rect = booleanBounds.get(setting);
        if (rect == null) {
            return;
        }

        boolean value = setting.getValue();
        fillRoundedRect(context, rect.x, rect.y, rect.w, rect.h, 8, value ? 0xAA6B3AFF : 0x66413952);
        int knobX = value ? rect.x + rect.w - 14 : rect.x + 2;
        fillRoundedRect(context, knobX, rect.y + 2, 12, rect.h - 4, 6, 0xFFFFFFFF);
    }

    private void renderModeSetting(DrawContext context, ModeSetting setting) {
        Rect rect = modeBounds.get(setting);
        if (rect == null) {
            return;
        }

        fillRoundedRect(context, rect.x, rect.y, rect.w, rect.h, 5, 0x66352A4F);
        String modeText = setting.getMode();
        int modeWidth = textRenderer.getWidth(modeText);
        context.drawText(textRenderer, modeText, rect.x + Math.max(4, (rect.w - modeWidth) / 2), rect.y + 4, 0xFFFFFFFF, false);
    }

    private void renderNumberSetting(DrawContext context, NumberSetting setting) {
        Rect rect = numberSliderBounds.get(setting);
        if (rect == null) {
            return;
        }

        double percent = normalize(setting.getValue(), setting.getMin(), setting.getMax());
        drawSlider(context, rect, percent);

        String valueText = formatDouble(setting.getValue());
        int valueW = textRenderer.getWidth(valueText);
        context.drawText(textRenderer, valueText, rect.right() - valueW, rect.y - 9, 0xFFD8C8F0, false);
    }

    private void renderRangeSetting(DrawContext context, RangeSetting setting) {
        Rect rect = rangeSliderBounds.get(setting);
        if (rect == null) {
            return;
        }

        fillRoundedRect(context, rect.x, rect.y, rect.w, rect.h, 4, 0x55342749);

        double minPercent = normalize(setting.getMinValue(), setting.getMin(), setting.getMax());
        double maxPercent = normalize(setting.getMaxValue(), setting.getMin(), setting.getMax());

        int minX = rect.x + (int) Math.round(minPercent * rect.w);
        int maxX = rect.x + (int) Math.round(maxPercent * rect.w);
        if (maxX < minX) {
            int temp = minX;
            minX = maxX;
            maxX = temp;
        }

        fillRoundedRect(context, minX, rect.y, Math.max(1, maxX - minX), rect.h, 4, 0xCC6B3AFF);
        fillRoundedRect(context, minX - 2, rect.y - 2, 4, rect.h + 4, 2, 0xFFFFFFFF);
        fillRoundedRect(context, maxX - 2, rect.y - 2, 4, rect.h + 4, 2, 0xFFFFFFFF);

        String valueText = formatDouble(setting.getMinValue()) + " - " + formatDouble(setting.getMaxValue());
        int valueW = textRenderer.getWidth(valueText);
        context.drawText(textRenderer, valueText, rect.right() - valueW, rect.y - 9, 0xFFD8C8F0, false);
    }

    private void renderColorSetting(DrawContext context, ColorSetting setting) {
        Rect row = settingBounds.get(setting);
        if (row == null) {
            return;
        }

        Color value = setting.getValue();
        fillRoundedRect(context, row.x + 8, row.y + 26, 18, 18, 4, value.getRGB());
        fillRoundedRectOutline(context, row.x + 8, row.y + 26, 18, 18, 4, 0xFFFFFFFF);

        int channels = setting.isHasAlpha() ? 4 : 3;
        for (int channel = 0; channel < channels; channel++) {
            Rect slider = colorSliderBounds.get(new ColorSliderKey(setting, channel));
            if (slider == null) {
                continue;
            }

            int channelValue = getColorChannel(setting, channel);
            double percent = channelValue / 255.0;
            drawSlider(context, slider, percent);

            String label = switch (channel) {
                case 0 -> "R";
                case 1 -> "G";
                case 2 -> "B";
                default -> "A";
            };
            context.drawText(textRenderer, label, slider.x - 10, slider.y, 0xFFD8C8F0, false);
        }
    }

    private void renderKeybindSetting(DrawContext context, KeybindSetting setting) {
        Rect holdRect = holdModeBounds.get(setting);
        Rect keyRect = keybindBounds.get(setting);
        if (holdRect == null || keyRect == null) {
            return;
        }

        fillRoundedRect(context, holdRect.x, holdRect.y, holdRect.w, holdRect.h, 5, setting.isHoldMode() ? 0xAA6B3AFF : 0x553F3151);
        context.drawText(textRenderer, setting.isHoldMode() ? "Hold" : "Toggle", holdRect.x + 7, holdRect.y + 4, 0xFFFFFFFF, false);

        fillRoundedRect(context, keyRect.x, keyRect.y, keyRect.w, keyRect.h, 5, 0x66352A4F);
        String keyText = listeningKeybind == setting ? "..." : KeyUtils.getKey(setting.getKeyCode());
        int keyWidth = textRenderer.getWidth(keyText);
        context.drawText(textRenderer, keyText, keyRect.x + Math.max(4, (keyRect.w - keyWidth) / 2), keyRect.y + 4, 0xFFFFFFFF, false);
    }

    private void renderStringSetting(DrawContext context, StringSetting setting) {
        Rect rect = stringInputBounds.get(setting);
        if (rect == null) {
            return;
        }

        fillRoundedRect(context, rect.x, rect.y, rect.w, rect.h, 5, 0x66352A4F);
        String value = setting.getValue();
        if (value == null) {
            value = "";
        }

        boolean editing = editingStringSetting == setting;
        String draw = value;
        int maxTextWidth = rect.w - 8;
        while (textRenderer.getWidth(draw) > maxTextWidth && draw.length() > 0) {
            draw = draw.substring(1);
        }

        context.drawText(textRenderer, draw, rect.x + 4, rect.y + 4, 0xFFFFFFFF, false);

        if (editing && (System.currentTimeMillis() / 400L) % 2L == 0L) {
            int cursorX = rect.x + 4 + textRenderer.getWidth(draw);
            context.fill(cursorX, rect.y + 3, cursorX + 1, rect.y + rect.h - 3, 0xFFFFFFFF);
        }
    }

    private void drawSlider(DrawContext context, Rect rect, double percent) {
        double clamped = MathHelper.clamp(percent, 0.0, 1.0);
        fillRoundedRect(context, rect.x, rect.y, rect.w, rect.h, 4, 0x55342749);

        int fillWidth = (int) Math.round(rect.w * clamped);
        if (fillWidth > 0) {
            fillRoundedRect(context, rect.x, rect.y, fillWidth, rect.h, 4, 0xCC6B3AFF);
        }

        int knobX = rect.x + fillWidth;
        fillRoundedRect(context, knobX - 2, rect.y - 2, 4, rect.h + 4, 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        updateLayout();
        rebuildHitboxes();

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (listeningKeybind != null) {
            listeningKeybind.setKeyCode(button);
            listeningKeybind = null;
            return true;
        }

        int totalWidth = 0;
        for (Category category : categories) {
            totalWidth += textRenderer.getWidth(category.getName()) + (CHIP_PADDING * 2) + CHIP_SPACING;
        }
        totalWidth -= CHIP_SPACING;

        int chipX = centerX - (totalWidth / 2);
        for (Category category : categories) {
            int chipWidth = textRenderer.getWidth(category.getName()) + (CHIP_PADDING * 2);
            if (mouseX >= chipX && mouseX <= chipX + chipWidth && mouseY >= chipsY && mouseY <= chipsY + CHIP_HEIGHT) {
                selectedCategory = category;
                selectedModule = null;
                moduleScroll = 0;
                settingsScroll = 0;
                rebuildHitboxes();
                return true;
            }
            chipX += chipWidth + CHIP_SPACING;
        }

        for (Module module : visibleModules) {
            Rect row = moduleRowBounds.get(module);
            Rect toggle = moduleToggleBounds.get(module);
            if (row == null || toggle == null || !row.contains(mouseX, mouseY)) {
                continue;
            }

            if (button == 0 && toggle.contains(mouseX, mouseY)) {
                module.toggle();
            } else if (button == 0) {
                selectedModule = module;
                settingsScroll = 0;
            } else if (button == 1) {
                module.toggle();
            }

            rebuildHitboxes();
            return true;
        }

        if (selectedModule != null) {
            for (Setting setting : selectedModule.getSettings()) {
                Rect row = settingBounds.get(setting);
                if (row == null || !row.contains(mouseX, mouseY)) {
                    continue;
                }

                if (setting instanceof BooleanSetting booleanSetting) {
                    if (button == 0) {
                        booleanSetting.toggle();
                        return true;
                    }
                }

                if (setting instanceof ModeSetting modeSetting) {
                    if (button == 0) {
                        modeSetting.cycle();
                        return true;
                    }
                    if (button == 1) {
                        cycleModeBackward(modeSetting);
                        return true;
                    }
                }

                if (setting instanceof NumberSetting numberSetting) {
                    Rect slider = numberSliderBounds.get(numberSetting);
                    if (button == 0 && slider != null && slider.contains(mouseX, mouseY)) {
                        draggingNumberSetting = numberSetting;
                        applyNumberFromMouse(numberSetting, mouseX);
                        return true;
                    }
                }

                if (setting instanceof RangeSetting rangeSetting) {
                    Rect slider = rangeSliderBounds.get(rangeSetting);
                    if (button == 0 && slider != null && slider.contains(mouseX, mouseY)) {
                        double minPercent = normalize(rangeSetting.getMinValue(), rangeSetting.getMin(), rangeSetting.getMax());
                        double maxPercent = normalize(rangeSetting.getMaxValue(), rangeSetting.getMin(), rangeSetting.getMax());
                        int minX = slider.x + (int) Math.round(minPercent * slider.w);
                        int maxX = slider.x + (int) Math.round(maxPercent * slider.w);
                        draggingRangeMax = Math.abs(mouseX - maxX) <= Math.abs(mouseX - minX);
                        draggingRangeSetting = rangeSetting;
                        applyRangeFromMouse(rangeSetting, mouseX, draggingRangeMax);
                        return true;
                    }
                }

                if (setting instanceof ColorSetting colorSetting) {
                    if (button == 0) {
                        int channels = colorSetting.isHasAlpha() ? 4 : 3;
                        for (int channel = 0; channel < channels; channel++) {
                            Rect slider = colorSliderBounds.get(new ColorSliderKey(colorSetting, channel));
                            if (slider != null && slider.contains(mouseX, mouseY)) {
                                draggingColorSetting = colorSetting;
                                draggingColorChannel = channel;
                                applyColorFromMouse(colorSetting, channel, mouseX);
                                return true;
                            }
                        }
                    }
                }

                if (setting instanceof KeybindSetting keybindSetting) {
                    Rect holdRect = holdModeBounds.get(keybindSetting);
                    Rect keyRect = keybindBounds.get(keybindSetting);

                    if (button == 0 && holdRect != null && holdRect.contains(mouseX, mouseY)) {
                        keybindSetting.toggleHoldMode();
                        return true;
                    }

                    if (keyRect != null && keyRect.contains(mouseX, mouseY)) {
                        if (button == 0) {
                            listeningKeybind = keybindSetting;
                            return true;
                        }
                        if (button == 1) {
                            keybindSetting.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
                            return true;
                        }
                    }
                }

                if (setting instanceof StringSetting stringSetting) {
                    Rect inputRect = stringInputBounds.get(stringSetting);
                    if (button == 0 && inputRect != null && inputRect.contains(mouseX, mouseY)) {
                        editingStringSetting = stringSetting;
                        return true;
                    }
                }
            }
        }

        if (editingStringSetting != null) {
            Rect active = stringInputBounds.get(editingStringSetting);
            if (active == null || !active.contains(mouseX, mouseY)) {
                editingStringSetting = null;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        updateLayout();
        rebuildHitboxes();

        double mouseX = click.x();

        if (draggingNumberSetting != null) {
            applyNumberFromMouse(draggingNumberSetting, mouseX);
            return true;
        }

        if (draggingRangeSetting != null) {
            applyRangeFromMouse(draggingRangeSetting, mouseX, draggingRangeMax);
            return true;
        }

        if (draggingColorSetting != null && draggingColorChannel >= 0) {
            applyColorFromMouse(draggingColorSetting, draggingColorChannel, mouseX);
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean consumed = draggingNumberSetting != null || draggingRangeSetting != null || draggingColorSetting != null;

        draggingNumberSetting = null;
        draggingRangeSetting = null;
        draggingColorSetting = null;
        draggingColorChannel = -1;

        return consumed || super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        updateLayout();
        rebuildHitboxes();

        if (mouseX >= moduleListX && mouseX <= moduleListX + moduleListW && mouseY >= moduleListY && mouseY <= moduleListY + moduleListH) {
            moduleScroll = MathHelper.clamp(moduleScroll - (int) (verticalAmount * 18.0), 0, maxModuleScroll);
            return true;
        }

        if (mouseX >= settingsListX && mouseX <= settingsListX + settingsListW && mouseY >= settingsListY && mouseY <= settingsListY + settingsListH) {
            settingsScroll = MathHelper.clamp(settingsScroll - (int) (verticalAmount * 18.0), 0, maxSettingsScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.getKeycode();

        if (listeningKeybind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningKeybind.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                listeningKeybind.setKeyCode(keyCode);
            }
            listeningKeybind = null;
            return true;
        }

        if (editingStringSetting != null) {
            String value = editingStringSetting.getValue() == null ? "" : editingStringSetting.getValue();

            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                editingStringSetting = null;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!value.isEmpty()) {
                    editingStringSetting.setValue(value.substring(0, value.length() - 1));
                    AutoSaveManager.getInstance().scheduleSave();
                }
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (editingStringSetting != null) {
            char chr = (char) input.codepoint();
            if (chr >= 32 && chr < 127) {
                String current = editingStringSetting.getValue() == null ? "" : editingStringSetting.getValue();
                if (current.length() < 48) {
                    editingStringSetting.setValue(current + chr);
                    AutoSaveManager.getInstance().scheduleSave();
                }
                return true;
            }
        }

        return super.charTyped(input);
    }

    private void cycleModeBackward(ModeSetting setting) {
        List<String> modes = setting.getModes();
        if (modes.isEmpty()) {
            return;
        }

        int current = modes.indexOf(setting.getMode());
        int next = current <= 0 ? modes.size() - 1 : current - 1;
        setting.setMode(modes.get(next));
    }

    private void applyNumberFromMouse(NumberSetting setting, double mouseX) {
        Rect rect = numberSliderBounds.get(setting);
        if (rect == null) {
            return;
        }

        double percent = MathHelper.clamp((mouseX - rect.x) / rect.w, 0.0, 1.0);
        double value = setting.getMin() + (setting.getMax() - setting.getMin()) * percent;
        setting.setValue(value);
    }

    private void applyRangeFromMouse(RangeSetting setting, double mouseX, boolean maxHandle) {
        Rect rect = rangeSliderBounds.get(setting);
        if (rect == null) {
            return;
        }

        double percent = MathHelper.clamp((mouseX - rect.x) / rect.w, 0.0, 1.0);
        double value = setting.getMin() + (setting.getMax() - setting.getMin()) * percent;

        if (maxHandle) {
            setting.setMaxValue(value);
        } else {
            setting.setMinValue(value);
        }

        AutoSaveManager.getInstance().scheduleSave();
    }

    private void applyColorFromMouse(ColorSetting setting, int channel, double mouseX) {
        Rect rect = colorSliderBounds.get(new ColorSliderKey(setting, channel));
        if (rect == null) {
            return;
        }

        double percent = MathHelper.clamp((mouseX - rect.x) / rect.w, 0.0, 1.0);
        int value = MathHelper.clamp((int) Math.round(percent * 255.0), 0, 255);

        Color current = setting.getValue();
        int r = current.getRed();
        int g = current.getGreen();
        int b = current.getBlue();
        int a = current.getAlpha();

        switch (channel) {
            case 0 -> r = value;
            case 1 -> g = value;
            case 2 -> b = value;
            case 3 -> a = value;
            default -> {
                return;
            }
        }

        setting.setValue(r, g, b, a);
    }

    private int getColorChannel(ColorSetting setting, int channel) {
        return switch (channel) {
            case 0 -> setting.getRed();
            case 1 -> setting.getGreen();
            case 2 -> setting.getBlue();
            case 3 -> setting.getAlpha();
            default -> 0;
        };
    }

    private static double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0.0;
        }
        return MathHelper.clamp((value - min) / (max - min), 0.0, 1.0);
    }

    private static String formatDouble(double value) {
        if (Math.abs(value - Math.round(value)) < 0.00001) {
            return Integer.toString((int) Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++) {
            int dx = radius - i;
            context.fill(x + radius - dx, y + i, x + width - radius + dx, y + i + 1, color);
            context.fill(x + radius - dx, y + height - i - 1, x + width - radius + dx, y + height - i, color);
        }
    }

    private void fillRoundedRectOutline(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        context.fill(x + radius, y, x + width - radius, y + 1, color);
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + 1, y + height - radius, color);
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++) {
            int dx = radius - i;
            context.fill(x + radius - dx, y + i, x + radius - dx + 1, y + i + 1, color);
            context.fill(x + width - radius + dx - 1, y + i, x + width - radius + dx, y + i + 1, color);
            context.fill(x + radius - dx, y + height - i - 1, x + radius - dx + 1, y + height - i, color);
            context.fill(x + width - radius + dx - 1, y + height - i - 1, x + width - radius + dx, y + height - i, color);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private static final class Rect {
        private final int x;
        private final int y;
        private final int w;
        private final int h;

        private Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = Math.max(0, w);
            this.h = Math.max(0, h);
        }

        private int right() {
            return x + w;
        }

        private int bottom() {
            return y + h;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= bottom();
        }
    }

    private record ColorSliderKey(ColorSetting setting, int channel) {
    }
}
