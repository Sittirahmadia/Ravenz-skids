package com.raven.ravenz.gui;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.gui.animation.AnimationManager;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.modules.client.ClickGUIModule;
import com.raven.ravenz.module.setting.*;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;

public final class ClickGui extends Screen {

    // Layout
    private static final int COL_W      = 210;
    private static final int COL_GAP    = 6;
    private static final int HEADER_H   = 32;
    private static final int MOD_H      = 22;
    private static final int SETTING_H  = 26;
    private static final int PANEL_PAD  = 10;

    private static final Category[] COLS = {
        Category.COMBAT, Category.PLAYER, Category.MOVEMENT,
        Category.RENDER, Category.MISC, Category.CLIENT
    };

    // Colors
    private static final Color COL_BG         = new Color(18, 22, 42, 215);
    private static final Color HEADER_BG      = new Color(28, 33, 58, 230);
    private static final Color HEADER_LINE    = new Color(75, 95, 180, 200);
    private static final Color MOD_ENABLED    = new Color(230, 235, 255, 255);
    private static final Color MOD_DISABLED   = new Color(130, 140, 175, 255);
    private static final Color MOD_HOVER_BG   = new Color(45, 52, 88, 200);
    private static final Color MOD_ACTIVE_BAR = new Color(100, 125, 240, 255);
    private static final Color SETTINGS_BG    = new Color(14, 18, 36, 240);
    private static final Color SLIDER_BG      = new Color(40, 48, 80, 255);
    private static final Color SLIDER_FILL    = new Color(90, 115, 230, 255);
    private static final Color CHECK_ON       = new Color(90, 115, 230, 255);
    private static final Color ACCENT_TEXT    = new Color(180, 195, 255, 255);
    private static final Color MUTED_TEXT     = new Color(120, 130, 165, 255);
    private static final Color WHITE_C        = new Color(230, 235, 255, 255);
    private static final Color DROP_BG        = new Color(20, 25, 48, 245);

    // State
    private final AnimationManager animationManager;
    private Module openSettingsModule = null;
    private NumberSetting draggingSlider = null;
    private int sliderTrackX = 0;
    private int sliderTrackW = 0;
    private KeybindSetting listeningKeybind = null;
    private String searchQuery = "";
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private ModeSetting openDropdown = null;
    private int dropdownX = 0, dropdownY = 0, dropdownW = 0;

    public ClickGui() {
        super(Text.empty());
        this.animationManager = new AnimationManager();
        animationManager.initializeGuiAnimation();
        for (Module m : RavenZClient.INSTANCE.getModuleManager().getModules()) {
            animationManager.initializeModuleAnimations(m);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        animationManager.updateAnimations(delta);
        animationManager.updateGuiAnimations(delta);

        if (animationManager.shouldCloseGui()) {
            RavenZClient.INSTANCE.getModuleManager()
                .getModule(ClickGUIModule.class)
                .ifPresent(m -> m.setEnabled(false));
            super.close();
            return;
        }

        float anim = animationManager.getGuiAnimation();
        int alpha = (int)(anim * 255);
        if (alpha <= 0) return;

        // Full-screen dark overlay (since ScreenMixin cancels default background)
        ctx.fill(0, 0, width, height, new Color(0, 0, 0, (int)(anim * 140)).getRGB());

        int numCols = COLS.length;
        int totalW  = numCols * COL_W + (numCols - 1) * COL_GAP;
        int startX  = Math.max(PANEL_PAD, (width - totalW) / 2);

        // Compute max column height for scroll
        int maxH = 0;
        for (Category cat : COLS) {
            List<Module> mods = filteredModules(cat);
            int h = HEADER_H + mods.size() * MOD_H;
            if (openSettingsModule != null && openSettingsModule.getModuleCategory() == cat) {
                h += settingsHeight(openSettingsModule);
            }
            if (h > maxH) maxH = h;
        }
        maxScrollOffset = Math.max(0, maxH - (height - PANEL_PAD * 2));
        scrollOffset = Math.min(scrollOffset, maxScrollOffset);

        int startY = PANEL_PAD - scrollOffset;

        for (int ci = 0; ci < COLS.length; ci++) {
            int cx = startX + ci * (COL_W + COL_GAP);
            renderColumn(ctx, COLS[ci], cx, startY, mouseX, mouseY, alpha);
        }

        if (openDropdown != null) {
            renderDropdown(ctx, mouseX, mouseY, alpha);
        }
    }

    private void renderColumn(DrawContext ctx, Category cat, int cx, int cy,
                              int mouseX, int mouseY, int alpha) {
        List<Module> mods = filteredModules(cat);

        int colH = HEADER_H + mods.size() * MOD_H;
        if (openSettingsModule != null && openSettingsModule.getModuleCategory() == cat) {
            colH += settingsHeight(openSettingsModule);
        }

        fill(ctx, cx, cy, cx + COL_W, cy + colH, applyA(COL_BG, alpha));
        fill(ctx, cx, cy, cx + COL_W, cy + HEADER_H, applyA(HEADER_BG, alpha));
        fill(ctx, cx, cy + HEADER_H - 1, cx + COL_W, cy + HEADER_H, applyA(HEADER_LINE, alpha));

        String label = cat.getName();
        int lw = textRenderer.getWidth(label);
        ctx.drawText(textRenderer, label,
            cx + (COL_W - lw) / 2, cy + (HEADER_H - 8) / 2,
            applyA(WHITE_C, alpha).getRGB(), false);

        int my = cy + HEADER_H;
        for (Module mod : mods) {
            boolean enabled = mod.isEnabled();
            boolean hovered = inBox(mouseX, mouseY, cx, my, COL_W, MOD_H);
            boolean isOpen  = mod == openSettingsModule;

            if (hovered || isOpen) {
                fill(ctx, cx, my, cx + COL_W, my + MOD_H, applyA(MOD_HOVER_BG, alpha));
            }
            if (enabled) {
                fill(ctx, cx, my, cx + 3, my + MOD_H, applyA(MOD_ACTIVE_BAR, alpha));
            }

            Color tc = enabled ? applyA(MOD_ENABLED, alpha) : applyA(MOD_DISABLED, alpha);
            ctx.drawText(textRenderer, mod.getName(), cx + 9, my + (MOD_H - 8) / 2,
                tc.getRGB(), false);

            if (hasSettings(mod)) {
                String arrow = isOpen ? "v" : ">";
                ctx.drawText(textRenderer, arrow,
                    cx + COL_W - 14, my + (MOD_H - 8) / 2,
                    applyA(MUTED_TEXT, alpha).getRGB(), false);
            }

            my += MOD_H;

            if (isOpen) {
                int sh = renderSettings(ctx, mod, cx, my, mouseX, mouseY, alpha);
                my += sh;
            }
        }
    }

    private int renderSettings(DrawContext ctx, Module mod, int sx, int sy,
                               int mouseX, int mouseY, int alpha) {
        List<Setting> settings = mod.getSettings();
        if (settings == null || settings.isEmpty()) return 0;
        int totalH = settings.size() * SETTING_H + 6;

        fill(ctx, sx, sy, sx + COL_W, sy + totalH, applyA(SETTINGS_BG, alpha));
        fill(ctx, sx, sy, sx + 3, sy + totalH, applyA(HEADER_LINE, alpha));
        fill(ctx, sx, sy + totalH - 1, sx + COL_W, sy + totalH, applyA(HEADER_LINE, alpha));

        int ry = sy + 3;
        for (Setting s : settings) {
            renderSetting(ctx, s, sx, ry, mouseX, mouseY, alpha);
            ry += SETTING_H;
        }
        return totalH;
    }

    private void renderSetting(DrawContext ctx, Setting s, int sx, int sy,
                               int mouseX, int mouseY, int alpha) {
        int nameMaxW = COL_W / 2 - 4;
        String name  = truncate(s.getName(), nameMaxW);
        int textY    = sy + (SETTING_H - 8) / 2;
        ctx.drawText(textRenderer, name, sx + 8, textY,
            applyA(MUTED_TEXT, alpha).getRGB(), false);

        int ctrlX = sx + COL_W / 2 + 2;
        int ctrlW = COL_W / 2 - 10;
        int ctrlY = sy + (SETTING_H - 12) / 2;

        if (s instanceof BooleanSetting bs) {
            boolean val = bs.getValue();
            fill(ctx, ctrlX + ctrlW - 14, ctrlY + 1, ctrlX + ctrlW, ctrlY + 13,
                applyA(val ? CHECK_ON : SLIDER_BG, alpha));
            if (val) {
                ctx.drawText(textRenderer, "x",
                    ctrlX + ctrlW - 11, ctrlY + 3,
                    applyA(WHITE_C, alpha).getRGB(), false);
            }
        } else if (s instanceof NumberSetting ns) {
            fill(ctx, ctrlX, ctrlY + 3, ctrlX + ctrlW, ctrlY + 9, applyA(SLIDER_BG, alpha));
            double pct = (ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin());
            int fillW = (int)(pct * ctrlW);
            if (fillW > 0) fill(ctx, ctrlX, ctrlY + 3, ctrlX + fillW, ctrlY + 9, applyA(SLIDER_FILL, alpha));
            String val = formatDouble(ns.getValue());
            int vw = textRenderer.getWidth(val);
            ctx.drawText(textRenderer, val,
                ctrlX + (ctrlW - vw) / 2, ctrlY - 1,
                applyA(ACCENT_TEXT, alpha).getRGB(), false);
        } else if (s instanceof ModeSetting ms) {
            String mode = ms.getMode();
            int mw = textRenderer.getWidth(mode);
            fill(ctx, ctrlX, ctrlY, ctrlX + ctrlW, ctrlY + 14, applyA(SLIDER_BG, alpha));
            boolean hov = inBox(mouseX, mouseY, ctrlX, ctrlY, ctrlW, 14);
            ctx.drawText(textRenderer, mode,
                ctrlX + (ctrlW - mw) / 2, ctrlY + 3,
                applyA(hov ? WHITE_C : ACCENT_TEXT, alpha).getRGB(), false);
        } else if (s instanceof KeybindSetting ks) {
            boolean listening = ks == listeningKeybind;
            String keyText = listening ? "..." : (ks.getKeyCode() == -1 ? "NONE" : KeyUtils.getKey(ks.getKeyCode()));
            int kw = textRenderer.getWidth(keyText);
            fill(ctx, ctrlX, ctrlY, ctrlX + ctrlW, ctrlY + 14, applyA(SLIDER_BG, alpha));
            Color kc = listening ? applyA(new Color(255, 200, 80, 255), alpha) : applyA(ACCENT_TEXT, alpha);
            ctx.drawText(textRenderer, keyText,
                ctrlX + (ctrlW - kw) / 2, ctrlY + 3, kc.getRGB(), false);
        } else if (s instanceof ColorSetting cs) {
            Color col = cs.getValue();
            if (col != null) {
                fill(ctx, ctrlX, ctrlY, ctrlX + ctrlW, ctrlY + 14,
                    new Color(col.getRed(), col.getGreen(), col.getBlue(), Math.min(alpha, 200)));
            }
        }
    }

    private void renderDropdown(DrawContext ctx, int mouseX, int mouseY, int alpha) {
        List<String> modes = openDropdown.getModes();
        int dh = modes.size() * 16 + 4;
        fill(ctx, dropdownX, dropdownY, dropdownX + dropdownW, dropdownY + dh, applyA(DROP_BG, alpha));
        int ry = dropdownY + 2;
        for (String mode : modes) {
            boolean hov = inBox(mouseX, mouseY, dropdownX, ry, dropdownW, 16);
            boolean sel = mode.equals(openDropdown.getMode());
            if (hov || sel) fill(ctx, dropdownX, ry, dropdownX + dropdownW, ry + 16, applyA(MOD_HOVER_BG, alpha));
            ctx.drawText(textRenderer, mode, dropdownX + 6, ry + 4,
                applyA(sel ? WHITE_C : MUTED_TEXT, alpha).getRGB(), false);
            ry += 16;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx = click.x(), my = click.y();
        int btn = click.button();
        if (btn < 0 || btn > 8) return false;

        if (openDropdown != null) {
            if (handleDropdownClick(mx, my)) return true;
            openDropdown = null;
            return true;
        }

        if (listeningKeybind != null) {
            listeningKeybind.setKeyCode(-100 - btn);
            listeningKeybind.setListening(false);
            listeningKeybind = null;
            return true;
        }

        int numCols = COLS.length;
        int totalW  = numCols * COL_W + (numCols - 1) * COL_GAP;
        int startX  = Math.max(PANEL_PAD, (width - totalW) / 2);
        int startY  = PANEL_PAD - scrollOffset;

        for (int ci = 0; ci < COLS.length; ci++) {
            Category cat = COLS[ci];
            int cx = startX + ci * (COL_W + COL_GAP);
            if (mx < cx || mx > cx + COL_W) continue;

            List<Module> mods = filteredModules(cat);
            int rowY = startY + HEADER_H;

            for (Module mod : mods) {
                if (mod == openSettingsModule) {
                    int sh = settingsHeight(mod);
                    if (my >= rowY + MOD_H && my < rowY + MOD_H + sh) {
                        handleSettingClick(mx, my, mod, cx, rowY + MOD_H);
                        return true;
                    }
                }
                if (my >= rowY && my < rowY + MOD_H) {
                    if (btn == 1) {
                        if (hasSettings(mod)) {
                            openSettingsModule = (mod == openSettingsModule) ? null : mod;
                            openDropdown = null;
                        }
                    } else {
                        mod.toggle();
                    }
                    return true;
                }
                rowY += MOD_H;
                if (mod == openSettingsModule) rowY += settingsHeight(mod);
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    private boolean handleDropdownClick(double mx, double my) {
        if (openDropdown == null) return false;
        List<String> modes = openDropdown.getModes();
        int dh = modes.size() * 16 + 4;
        if (!inBox(mx, my, dropdownX, dropdownY, dropdownW, dh)) return false;
        int ry = dropdownY + 2;
        for (String mode : modes) {
            if (my >= ry && my < ry + 16) {
                openDropdown.setMode(mode);
                openDropdown = null;
                return true;
            }
            ry += 16;
        }
        return false;
    }

    private void handleSettingClick(double mx, double my, Module mod, int sx, int sy) {
        List<Setting> settings = mod.getSettings();
        if (settings == null) return;

        int ctrlX = sx + COL_W / 2 + 2;
        int ctrlW = COL_W / 2 - 10;
        int ry = sy + 3;

        for (Setting s : settings) {
            int ctrlY = ry + (SETTING_H - 12) / 2;

            if (s instanceof BooleanSetting bs) {
                if (inBox(mx, my, ctrlX + ctrlW - 14, ctrlY + 1, 14, 12)) {
                    bs.toggle();
                    return;
                }
            } else if (s instanceof NumberSetting ns) {
                if (inBox(mx, my, ctrlX, ctrlY + 3, ctrlW, 9)) {
                    double pct = Math.max(0, Math.min(1, (mx - ctrlX) / ctrlW));
                    ns.setValue(ns.getMin() + pct * (ns.getMax() - ns.getMin()));
                    draggingSlider = ns;
                    sliderTrackX   = ctrlX;
                    sliderTrackW   = ctrlW;
                    return;
                }
            } else if (s instanceof ModeSetting ms) {
                if (inBox(mx, my, ctrlX, ctrlY, ctrlW, 14)) {
                    if (openDropdown == ms) {
                        openDropdown = null;
                    } else {
                        openDropdown = ms;
                        dropdownX    = ctrlX;
                        dropdownY    = ry + SETTING_H;
                        dropdownW    = ctrlW;
                    }
                    return;
                }
            } else if (s instanceof KeybindSetting ks) {
                if (inBox(mx, my, ctrlX, ctrlY, ctrlW, 14)) {
                    if (listeningKeybind != null) listeningKeybind.setListening(false);
                    listeningKeybind = ks;
                    ks.setListening(true);
                    return;
                }
            }

            ry += SETTING_H;
        }
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingSlider != null && click.button() == 0) {
            double pct = Math.max(0, Math.min(1, (click.x() - sliderTrackX) / (double) sliderTrackW));
            draggingSlider.setValue(draggingSlider.getMin() + pct * (draggingSlider.getMax() - draggingSlider.getMin()));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) draggingSlider = null;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        scrollOffset = Math.max(0, Math.min(maxScrollOffset, (int)(scrollOffset - vAmt * 15)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();

        if (listeningKeybind != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                listeningKeybind.setKeyCode(-1);
            } else {
                listeningKeybind.setKeyCode(key);
            }
            listeningKeybind.setListening(false);
            listeningKeybind = null;
            return true;
        }

        if (openDropdown != null && key == GLFW.GLFW_KEY_ESCAPE) {
            openDropdown = null;
            return true;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char c = (char) input.codepoint();
        if (c >= 32 && c < 127 && searchQuery.length() < 30) {
            searchQuery += c;
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        animationManager.startClosingAnimation();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private List<Module> filteredModules(Category cat) {
        List<Module> all = RavenZClient.INSTANCE.getModuleManager().getModulesInCategory(cat);
        if (searchQuery.isEmpty()) return all;
        String q = searchQuery.toLowerCase();
        List<Module> out = new ArrayList<>();
        for (Module m : all) {
            if (m.getName().toLowerCase().contains(q)) out.add(m);
        }
        return out;
    }

    private boolean hasSettings(Module mod) {
        List<Setting> s = mod.getSettings();
        return s != null && !s.isEmpty();
    }

    private int settingsHeight(Module mod) {
        List<Setting> s = mod.getSettings();
        if (s == null || s.isEmpty()) return 0;
        return s.size() * SETTING_H + 6;
    }

    private boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void fill(DrawContext ctx, int x1, int y1, int x2, int y2, Color c) {
        if (x2 <= x1 || y2 <= y1) return;
        ctx.fill(x1, y1, x2, y2, c.getRGB());
    }

    private Color applyA(Color base, int alpha) {
        int a = Math.max(0, Math.min(255, (base.getAlpha() * alpha) / 255));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
    }

    private String formatDouble(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((int) v);
        return String.format("%.2f", v);
    }

    private String truncate(String s, int maxW) {
        if (textRenderer.getWidth(s) <= maxW) return s;
        while (s.length() > 1 && textRenderer.getWidth(s + "..") > maxW)
            s = s.substring(0, s.length() - 1);
        return s + "..";
    }
}
