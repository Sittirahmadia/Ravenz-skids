package com.raven.ravenz.gui;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.module.modules.client.ClientSettingsModule;
import com.raven.ravenz.gui.newgui.components.CategoryIcon;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.*;
import com.raven.ravenz.profiles.ProfileManager;
import com.raven.ravenz.utils.render.nanovg.NanoVGRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RichModernGUI extends Screen {

    // ── Dimensions ─────────────────────────────────────────────────────────────
    private static final int   GUI_W   = 760;
    private static final int   GUI_H   = 460;
    private static final int   LEFT_W  = 168;
    private static final int   MID_W   = 236;
    private static final int   RIGHT_W = GUI_W - LEFT_W - MID_W; // 375
    private static final float RADIUS  = 12f;
    private static final int   PAD     = 14;

    // Row / section heights
    private static final int PROFILE_H  = 108;
    private static final int SECTION_H  = 28;
    private static final int CAT_H      = 36;
    private static final int MOD_HDR_H  = 54;
    private static final int MOD_ROW_H  = 44;
    private static final int SET_HDR_H  = 58;
    private static final int SET_BOOL_H = 50;
    private static final int SET_NUM_H  = 54;
    private static final int SET_MODE_H = 50;
    private static final int SET_RNG_H  = 58;
    private static final int SET_CLR_H  = 40;
    private static final int SET_SML_H  = 36;
    private static final int SET_BIND_H = 44;
    private static final int PROF_ROW_H = 44;

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final Color C_BG       = new Color(26,  26,  34);
    private static final Color C_LEFT     = new Color(32,  32,  44);
    private static final Color C_BORDER   = new Color(44,  44,  58);
    private static final Color C_TEXT     = new Color(228, 228, 240);
    private static final Color C_MUTED    = new Color(106, 106, 124);
    private static final Color C_SECT_LN  = new Color(54,  54,  70);
    private static final Color C_MOD_HOV  = new Color(38,  38,  52);
    private static final Color C_MOD_BG   = new Color(30,  30,  42);
    private static final Color C_TOG_OFF  = new Color(52,  52,  70);
    private static final Color C_TRACK    = new Color(46,  46,  64);
    private static final Color C_INPUT    = new Color(20,  20,  30);
    private static final Color C_DROP     = new Color(28,  28,  42);

    // ── State ──────────────────────────────────────────────────────────────────
    private Category selectedCat  = Category.COMBAT;
    private Module   selectedMod  = null;
    private String   search       = "";
    private boolean  searchActive = false;
    private float    modScroll    = 0f;
    private float    setScroll    = 0f;
    private boolean  creditsOpen  = false;
    private boolean  configOpen   = false;

    // Number slider drag
    private NumberSetting draggingNum  = null;
    private float         numTrackX, numTrackW;

    // Range slider drag
    private RangeSetting draggingRange    = null;
    private boolean      draggingRangeMin = false;
    private float        rangeTrackX, rangeTrackW;

    // Mode dropdown
    private ModeSetting expandedMode = null;
    private float       dropX, dropY, dropW;

    // Category hover animations (0..1)
    private final java.util.HashMap<Category, Float> catHover = new java.util.HashMap<>();

    // Open/close animation
    private float   anim    = 0f;
    private boolean closing = false;
    private long    lastMs  = System.currentTimeMillis();

    // Player skin (cached per GL texture ID)
    private static int cachedSkinImage = -1;
    private static String cachedSkinPath = null;

    // Profile manager state
    private final List<String> profileList      = new ArrayList<>();
    private String             selectedProfile  = null;
    private String             deleteConfirmPrf = null;
    private long               deleteConfirmMs  = 0;
    private boolean            namingProfile    = false;
    private String             newProfileName   = "";
    private float              profileScroll    = 0f;
    private static final long  DELETE_TIMEOUT   = 3000;

    // ── Constructor ────────────────────────────────────────────────────────────
    public RichModernGUI() {
        super(Text.literal("Rich Modern"));
        CategoryIcon.init();
        autoSelectMod();
    }

    private void autoSelectMod() {
        selectedMod = null;
        for (Module m : RavenZClient.INSTANCE.getModuleManager().getModulesByCategory(selectedCat)) {
            if (m.getSettings().size() > 1) {
                selectedMod = m;
                return;
            }
        }
    }

    // ── Master render ──────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastMs) / 1000f, 0.1f);
        lastMs = now;

        if (closing) {
            anim -= dt * 6f;
            if (anim <= 0f) { anim = 0f; if (client != null) client.setScreen(null); return; }
        } else {
            anim += dt * 6f;
            if (anim > 1f) anim = 1f;
        }

        float sc = ease(anim) * guiScale();
        float al = anim;
        int cx = width / 2, cy = height / 2;
        float gx = cx - GUI_W / 2f, gy = cy - GUI_H / 2f;
        float mx = tmx((float) mouseX, cx, sc), my = tmy((float) mouseY, cy, sc);

        // Expire delete confirmation
        if (deleteConfirmPrf != null && now - deleteConfirmMs >= DELETE_TIMEOUT) deleteConfirmPrf = null;

        updateCatHovers(dt, mx, my, gx, gy);

        boolean frameReady = NanoVGRenderer.beginFrame(context);
        if (!frameReady && !NanoVGRenderer.isInFrame()) {
            renderNoRendererFallback(context);
            return;
        }

        NanoVGRenderer.save();
        NanoVGRenderer.translate(cx, cy);
        NanoVGRenderer.scale(sc, sc);
        NanoVGRenderer.translate(-cx, -cy);

        // Outer container
        NanoVGRenderer.drawRoundedRect(gx, gy, GUI_W, GUI_H, RADIUS, a(C_BG, al));
        // Left panel background
        NanoVGRenderer.drawRoundedRectVarying(gx, gy, LEFT_W, GUI_H, RADIUS, 0, 0, RADIUS, a(C_LEFT, al));
        // Outer border
        NanoVGRenderer.drawRoundedRectOutline(gx, gy, GUI_W, GUI_H, RADIUS, 1f, a(C_BORDER, al * 0.8f));
        // Vertical dividers
        Color div = a(C_BORDER, al * 0.6f);
        NanoVGRenderer.drawRect(gx + LEFT_W,          gy + 1, 1, GUI_H - 2, div);
        NanoVGRenderer.drawRect(gx + LEFT_W + MID_W,  gy + 1, 1, GUI_H - 2, div);

        renderLeft  (gx,                      gy, mx, my, al);
        renderMiddle(gx + LEFT_W + 1,         gy, mx, my, al);
        renderRight (gx + LEFT_W + MID_W + 1, gy, mx, my, al);

        if (expandedMode != null) renderDropdown(al);

        NanoVGRenderer.restore();
        NanoVGRenderer.endFrame();
    }

    // ── Left panel ─────────────────────────────────────────────────────────────
    private void renderLeft(float px, float py, float mx, float my, float al) {
        float w = LEFT_W;
        Color accent = ClientSettingsModule.getAccentColor();

        // Avatar (player skin face)
        float avsz = 50f;
        float avx = px + (w - avsz) / 2f;
        float avy = py + 22;
        NanoVGRenderer.drawRoundedRect(avx, avy, avsz, avsz, 10f,
                a(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40), al));
        NanoVGRenderer.drawRoundedRectOutline(avx, avy, avsz, avsz, 10f, 1.5f, a(accent, al));
        updateSkinImage();
        renderPlayerHead(avx, avy, avsz, al);
        // Online dot
        NanoVGRenderer.drawCircle(avx + avsz - 7, avy + avsz - 7, 5.5f, a(new Color(26, 26, 36), al));
        NanoVGRenderer.drawCircle(avx + avsz - 7, avy + avsz - 7, 4f,   a(new Color(52, 210, 82), al));

        // Player name
        String name = (RavenZClient.mc != null && RavenZClient.mc.player != null)
                ? RavenZClient.mc.player.getName().getString() : "null";
        float ny = avy + avsz + 9;
        NanoVGRenderer.drawText(name, px + (w - NanoVGRenderer.getTextWidth(name, 11.5f)) / 2f,
                ny, 11.5f, a(C_TEXT, al), true);

        // Divider "Main"
        float dy = py + PROFILE_H;
        renderDivider(px + PAD, dy, w - PAD * 2, "Main", al);

        Category[] main = { Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC };
        float catY = dy + SECTION_H;
        for (Category c : main) { renderCatRow(px, catY, w, c, mx, my, al); catY += CAT_H; }

        // Divider "Other"
        renderDivider(px + PAD, catY + 4, w - PAD * 2, "Other", al);
        catY += SECTION_H + 8;
        renderCatRow(px, catY, w, Category.CLIENT, mx, my, al);

        // Credits + Config buttons at bottom
        float btnH = 28f;
        float btnY = py + GUI_H - 44;
        float btnW = (w - PAD * 2 - 5) / 2f;

        // Credits button
        boolean credHov = mx >= px + PAD && mx <= px + PAD + btnW && my >= btnY && my <= btnY + btnH;
        boolean credSel = creditsOpen;
        NanoVGRenderer.drawRoundedRect(px + PAD, btnY, btnW, btnH, 7f,
                a(credSel ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40)
                          : (credHov ? new Color(36, 36, 52) : new Color(26, 26, 38)), al));
        NanoVGRenderer.drawRoundedRectOutline(px + PAD, btnY, btnW, btnH, 7f, 1f,
                a(credSel ? accent : C_BORDER, al * (credSel ? 1f : 0.7f)));
        String cred = "Credits";
        NanoVGRenderer.drawText(cred,
                px + PAD + (btnW - NanoVGRenderer.getTextWidth(cred, 9.5f)) / 2f,
                btnY + 10, 9.5f, a(credSel ? accent : C_MUTED, al));

        // Config button
        float cfgX = px + PAD + btnW + 5;
        boolean cfgHov = mx >= cfgX && mx <= cfgX + btnW && my >= btnY && my <= btnY + btnH;
        boolean cfgSel = configOpen;
        NanoVGRenderer.drawRoundedRect(cfgX, btnY, btnW, btnH, 7f,
                a(cfgSel ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40)
                         : (cfgHov ? new Color(36, 36, 52) : new Color(26, 26, 38)), al));
        NanoVGRenderer.drawRoundedRectOutline(cfgX, btnY, btnW, btnH, 7f, 1f,
                a(cfgSel ? accent : C_BORDER, al * (cfgSel ? 1f : 0.7f)));
        String cfg = "Config";
        NanoVGRenderer.drawText(cfg,
                cfgX + (btnW - NanoVGRenderer.getTextWidth(cfg, 9.5f)) / 2f,
                btnY + 10, 9.5f, a(cfgSel ? accent : C_MUTED, al));
    }

    private void renderDivider(float x, float y, float w, String label, float al) {
        float lw    = NanoVGRenderer.getTextWidth(label, 8.5f);
        float lineY = y + 7;
        float gap   = 5f;
        float lineW = (w - lw - gap * 2) / 2f;
        NanoVGRenderer.drawRect(x, lineY, lineW, 1, a(C_SECT_LN, al * 0.6f));
        NanoVGRenderer.drawText(label, x + lineW + gap, y, 8.5f, a(C_MUTED, al * 0.75f));
        NanoVGRenderer.drawRect(x + lineW + gap * 2 + lw, lineY, lineW, 1, a(C_SECT_LN, al * 0.6f));
    }

    private void renderCatRow(float px, float y, float w, Category c, float mx, float my, float al) {
        boolean sel = (c == selectedCat);
        Color accent = ClientSettingsModule.getAccentColor();
        float hv = catHover.getOrDefault(c, 0f);

        if (sel) {
            NanoVGRenderer.drawRect(px + 4, y + 2, w - 8, CAT_H - 4,
                    a(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28), al));
            NanoVGRenderer.drawRoundedRect(px + 4, y + (CAT_H - 18) / 2f, 3, 18, 1.5f, a(accent, al));
        } else if (hv > 0.01f) {
            NanoVGRenderer.drawRect(px + 4, y + 2, w - 8, CAT_H - 4,
                    a(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(15 * hv)), al));
        }

        float iconSz = 14f;
        CategoryIcon.drawIcon(c, px + 22, y + (CAT_H - iconSz) / 2f, a(sel ? accent : C_MUTED, al));

        float tx = px + 22 + iconSz + 8, ty = y + (CAT_H - 10f) / 2f;
        Color tc = sel ? a(C_TEXT, al) : a(new Color(155, 155, 175), al * (0.6f + 0.4f * hv));
        NanoVGRenderer.drawText(c.getName(), tx, ty, 10f, tc, sel);
    }

    private void updateCatHovers(float dt, float mx, float my, float gx, float gy) {
        Category[] main = { Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC };
        float y = gy + PROFILE_H + SECTION_H;
        for (Category c : main) {
            boolean h = (c != selectedCat) && mx >= gx && mx <= gx + LEFT_W && my >= y && my <= y + CAT_H;
            catHover.put(c, lerp(catHover.getOrDefault(c, 0f), h ? 1f : 0f, dt * 12f));
            y += CAT_H;
        }
        y += SECTION_H + 8;
        boolean h = (Category.CLIENT != selectedCat) && mx >= gx && mx <= gx + LEFT_W && my >= y && my <= y + CAT_H;
        catHover.put(Category.CLIENT, lerp(catHover.getOrDefault(Category.CLIENT, 0f), h ? 1f : 0f, dt * 12f));
    }

    // ── Middle panel ───────────────────────────────────────────────────────────
    private void renderMiddle(float px, float py, float mx, float my, float al) {
        float w = MID_W - 1;

        // Search bar
        float sbW = w - PAD * 2, sbH = 26;
        float sbX = px + PAD, sbY = py + MOD_HDR_H - sbH - 8;
        Color sbBorder = searchActive
                ? a(ClientSettingsModule.getAccentColor(), al)
                : a(C_BORDER, al * 0.7f);
        NanoVGRenderer.drawRoundedRect(sbX, sbY, sbW, sbH, 6f, a(C_INPUT, al));
        NanoVGRenderer.drawRoundedRectOutline(sbX, sbY, sbW, sbH, 6f, 1f, sbBorder);

        // Search icon
        Color ic = a(C_MUTED, al * 0.9f);
        NanoVGRenderer.drawCircle(sbX + 11, sbY + sbH / 2f - 1, 4.5f, ic);
        NanoVGRenderer.drawCircle(sbX + 11, sbY + sbH / 2f - 1, 2.8f, a(C_INPUT, al));
        NanoVGRenderer.drawLine(sbX + 14, sbY + sbH / 2f + 2, sbX + 17, sbY + sbH / 2f + 5, 1.5f, ic);

        String sd = search.isEmpty() ? "Search Modules..." : search;
        NanoVGRenderer.drawText(sd, sbX + 23, sbY + 9, 9.5f,
                search.isEmpty() ? a(C_MUTED, al * 0.65f) : a(C_TEXT, al));
        if (searchActive && System.currentTimeMillis() % 1000 < 500) {
            float curX = sbX + 23 + NanoVGRenderer.getTextWidth(search, 9.5f);
            NanoVGRenderer.drawRect(curX, sbY + 6, 1.2f, 14f, a(C_TEXT, al));
        }

        // Header divider
        NanoVGRenderer.drawRect(px, py + MOD_HDR_H, w, 1, a(C_BORDER, al * 0.5f));

        // Module list (scissored)
        float listY = py + MOD_HDR_H + 1;
        float listH = GUI_H - MOD_HDR_H - 1;
        NanoVGRenderer.scissor(px, listY, w, listH);

        List<Module> mods = displayMods();
        float totalH = mods.size() * MOD_ROW_H;
        modScroll = clamp(modScroll, 0, Math.max(0, totalH - listH));

        float ry = listY - modScroll;
        for (Module m : mods) {
            renderModRow(m, px, ry, w, mx, my, al);
            ry += MOD_ROW_H;
        }
        NanoVGRenderer.resetScissor();

        if (totalH > listH) {
            float sh = (listH / totalH) * listH;
            float sy = listY + (modScroll / totalH) * listH;
            NanoVGRenderer.drawRoundedRect(px + w - 4, sy, 3, sh, 1.5f,
                    a(ClientSettingsModule.getAccentColor(), al * 0.45f));
        }
    }

    private void renderModRow(Module m, float px, float y, float w, float mx, float my, float al) {
        boolean hov = mx >= px && mx <= px + w && my >= y && my <= y + MOD_ROW_H;
        boolean sel = (m == selectedMod);
        boolean en  = m.isEnabled();
        Color accent = ClientSettingsModule.getAccentColor();

        if (sel) {
            NanoVGRenderer.drawRect(px, y, w, MOD_ROW_H,
                    a(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18), al));
        } else if (hov) {
            NanoVGRenderer.drawRect(px, y, w, MOD_ROW_H, a(C_MOD_HOV, al * 0.5f));
        }

        if (en) NanoVGRenderer.drawRoundedRect(px, y + 6, 3, MOD_ROW_H - 12, 1.5f, a(accent, al));
        NanoVGRenderer.drawRect(px + PAD, y + MOD_ROW_H - 1, w - PAD * 2, 1, a(C_BORDER, al * 0.3f));

        Color nc = en ? a(C_TEXT, al) : a(new Color(155, 155, 175), al * 0.75f);
        NanoVGRenderer.drawText(m.getName(), px + PAD + 4, y + 10, 11f, nc);

        if (m.getDescription() != null && !m.getDescription().isEmpty()) {
            String d = truncate(m.getDescription(), w - PAD * 3 - 50, 8.5f);
            NanoVGRenderer.drawText(d, px + PAD + 4, y + 25, 8.5f, a(C_MUTED, al * 0.65f));
        }

        float tW = 22f, tH = 11f;
        float tX = px + w - PAD - tW - 16, tY = y + (MOD_ROW_H - tH) / 2f;
        miniToggle(tX, tY, tW, tH, en, al, accent);

        float dotX = px + w - PAD - 11;
        Color dc = hov ? a(C_TEXT, al) : a(C_MUTED, al * 0.55f);
        NanoVGRenderer.drawText("...", dotX, y + (MOD_ROW_H - 10f) / 2f, 10f, dc);
    }

    private void miniToggle(float x, float y, float w, float h, boolean on, float al, Color accent) {
        NanoVGRenderer.drawRoundedRect(x, y, w, h, h / 2f, on ? a(accent, al) : a(C_TOG_OFF, al));
        float tr = h / 2f - 1.5f;
        float tx = on ? (x + w - tr - 2.5f) : (x + tr + 2.5f);
        NanoVGRenderer.drawCircle(tx, y + h / 2f, tr, a(Color.WHITE, al));
    }

    private List<Module> displayMods() {
        List<Module> all = RavenZClient.INSTANCE.getModuleManager().getModulesByCategory(selectedCat);
        if (search.isEmpty()) return all;
        String q = search.toLowerCase();
        return all.stream().filter(m -> m.getName().toLowerCase().contains(q)).collect(Collectors.toList());
    }

    // ── Right panel ────────────────────────────────────────────────────────────
    private void renderRight(float px, float py, float mx, float my, float al) {
        if (creditsOpen) { renderCredits(px, py, al); return; }
        if (configOpen)  { renderConfigPanel(px, py, mx, my, al); return; }

        float w = RIGHT_W - 1;
        if (selectedMod == null) {
            String hint = "Select a module for settings";
            NanoVGRenderer.drawText(hint,
                    px + (w - NanoVGRenderer.getTextWidth(hint, 10f)) / 2f,
                    py + GUI_H / 2f, 10f, a(C_MUTED, al * 0.45f));
            return;
        }

        Color accent = ClientSettingsModule.getAccentColor();
        NanoVGRenderer.drawRect(px, py, w, SET_HDR_H, a(new Color(28, 28, 40), al * 0.55f));
        NanoVGRenderer.drawText(selectedMod.getName(), px + PAD, py + 22, 16f, a(C_TEXT, al), true);

        if (selectedMod.isEnabled()) {
            float bw = NanoVGRenderer.getTextWidth("ENABLED", 8f) + 14;
            float bh = 17f, bx = px + w - PAD - bw, by = py + (SET_HDR_H - bh) / 2f;
            NanoVGRenderer.drawRoundedRect(bx, by, bw, bh, bh / 2f,
                    a(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40), al));
            NanoVGRenderer.drawRoundedRectOutline(bx, by, bw, bh, bh / 2f, 1f, a(accent, al * 0.55f));
            NanoVGRenderer.drawText("ENABLED", bx + 7, by + (bh - 8f) / 2f, 8f, a(accent, al));
        }

        NanoVGRenderer.drawRect(px, py + SET_HDR_H, w, 1, a(C_BORDER, al * 0.5f));

        float listY = py + SET_HDR_H + 1;
        float listH = GUI_H - SET_HDR_H - 1;
        float totalH = totalSetH();
        setScroll = clamp(setScroll, 0, Math.max(0, totalH - listH));

        NanoVGRenderer.scissor(px, listY, w, listH);
        float sy = listY + 8 - setScroll;
        for (Setting s : selectedMod.getSettings()) {
            float rh = setH(s);
            if (sy + rh > listY - 5 && sy < listY + listH + 5)
                renderSet(s, px + PAD, sy, w - PAD * 2, mx, my, al, accent);
            sy += rh;
        }
        NanoVGRenderer.resetScissor();

        if (totalH > listH) {
            float sh = (listH / totalH) * listH;
            float ssy = listY + (setScroll / totalH) * listH;
            NanoVGRenderer.drawRoundedRect(px + w - 4, ssy, 3, sh, 1.5f, a(accent, al * 0.45f));
        }
    }

    // ── Credits panel ──────────────────────────────────────────────────────────
    private void renderCredits(float px, float py, float al) {
        float w = RIGHT_W - 1;
        Color accent = ClientSettingsModule.getAccentColor();

        NanoVGRenderer.drawRoundedRect(px + w / 2f - 28, py + 18, 56, 3, 1.5f, a(accent, al * 0.55f));

        String title = "Krypton Client";
        NanoVGRenderer.drawText(title,
                px + (w - NanoVGRenderer.getTextWidth(title, 18f, true)) / 2f,
                py + 36, 18f, a(C_TEXT, al), true);

        String ver = "v1.0.0";
        NanoVGRenderer.drawText(ver,
                px + (w - NanoVGRenderer.getTextWidth(ver, 9.5f)) / 2f,
                py + 58, 9.5f, a(C_MUTED, al * 0.7f));

        float sy = py + 86;
        renderDivider(px + PAD, sy, w - PAD * 2, "Developers", al);
        sy += SECTION_H + 6;
        renderCreditEntry(px, sy, w, "Krypton Developers", "Lead Developer", al, accent);
    }

    private void renderCreditEntry(float px, float y, float w, String name, String role, float al, Color accent) {
        NanoVGRenderer.drawCircle(px + PAD + 4, y + 9, 3f, a(accent, al * 0.75f));
        NanoVGRenderer.drawText(name, px + PAD + 14, y + 4, 11f, a(C_TEXT, al), true);
        NanoVGRenderer.drawText(role, px + PAD + 14, y + 19, 9f, a(C_MUTED, al * 0.65f));
    }

    // ── Config / Profile manager panel ─────────────────────────────────────────
    private void renderConfigPanel(float px, float py, float mx, float my, float al) {
        float w = RIGHT_W - 1;
        Color accent = ClientSettingsModule.getAccentColor();

        // Header
        NanoVGRenderer.drawRect(px, py, w, SET_HDR_H, a(new Color(28, 28, 40), al * 0.55f));
        NanoVGRenderer.drawText("Profiles", px + PAD, py + 22, 16f, a(C_TEXT, al), true);

        // Save button
        float bH = 22f, bW = 42f;
        float saveBX = px + w - PAD - bW, saveBY = py + (SET_HDR_H - bH) / 2f;
        boolean saveHov = mx >= saveBX && mx <= saveBX + bW && my >= saveBY && my <= saveBY + bH;
        NanoVGRenderer.drawRoundedRect(saveBX, saveBY, bW, bH, 5f,
                a(saveHov ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50)
                          : new Color(28, 28, 42), al));
        NanoVGRenderer.drawRoundedRectOutline(saveBX, saveBY, bW, bH, 5f, 1f,
                a(saveHov ? accent : C_BORDER, al * (saveHov ? 1f : 0.7f)));
        String saveLabel = "Save";
        NanoVGRenderer.drawText(saveLabel,
                saveBX + (bW - NanoVGRenderer.getTextWidth(saveLabel, 9f)) / 2f,
                saveBY + 7, 9f, a(saveHov ? accent : C_MUTED, al));

        // Load button (only when a profile is selected)
        if (selectedProfile != null) {
            float loadBX = saveBX - 6 - bW, loadBY = saveBY;
            boolean loadHov = mx >= loadBX && mx <= loadBX + bW && my >= loadBY && my <= loadBY + bH;
            NanoVGRenderer.drawRoundedRect(loadBX, loadBY, bW, bH, 5f,
                    a(loadHov ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50)
                              : new Color(28, 28, 42), al));
            NanoVGRenderer.drawRoundedRectOutline(loadBX, loadBY, bW, bH, 5f, 1f,
                    a(loadHov ? accent : C_BORDER, al * (loadHov ? 1f : 0.7f)));
            String loadLabel = "Load";
            NanoVGRenderer.drawText(loadLabel,
                    loadBX + (bW - NanoVGRenderer.getTextWidth(loadLabel, 9f)) / 2f,
                    loadBY + 7, 9f, a(loadHov ? accent : C_MUTED, al));
        }

        NanoVGRenderer.drawRect(px, py + SET_HDR_H, w, 1, a(C_BORDER, al * 0.5f));

        // Profile list
        float listY = py + SET_HDR_H + 1;
        float listH = GUI_H - SET_HDR_H - 1;
        float totalH = (profileList.size() + 1) * PROF_ROW_H;
        profileScroll = clamp(profileScroll, 0, Math.max(0, totalH - listH));

        NanoVGRenderer.scissor(px, listY, w, listH);
        float ry = listY - profileScroll;
        for (String pName : profileList) {
            renderProfileRow(pName, px, ry, w, mx, my, al, accent);
            ry += PROF_ROW_H;
        }
        renderNewProfileRow(px, ry, w, mx, my, al, accent);
        NanoVGRenderer.resetScissor();

        if (totalH > listH) {
            float sh = (listH / totalH) * listH;
            float ssY = listY + (profileScroll / totalH) * listH;
            NanoVGRenderer.drawRoundedRect(px + w - 4, ssY, 3, sh, 1.5f, a(accent, al * 0.45f));
        }
    }

    private void renderProfileRow(String pName, float px, float y, float w,
                                   float mx, float my, float al, Color accent) {
        boolean hov = mx >= px && mx <= px + w && my >= y && my <= y + PROF_ROW_H;
        boolean sel = pName.equals(selectedProfile);
        boolean del = pName.equals(deleteConfirmPrf);

        if (del) {
            NanoVGRenderer.drawRect(px, y, w, PROF_ROW_H, a(new Color(160, 40, 40, 60), al));
        } else if (sel) {
            NanoVGRenderer.drawRect(px, y, w, PROF_ROW_H,
                    a(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 20), al));
            NanoVGRenderer.drawRoundedRect(px, y + 6, 3, PROF_ROW_H - 12, 1.5f, a(accent, al));
        } else if (hov) {
            NanoVGRenderer.drawRect(px, y, w, PROF_ROW_H, a(C_MOD_HOV, al * 0.5f));
        }
        NanoVGRenderer.drawRect(px + PAD, y + PROF_ROW_H - 1, w - PAD * 2, 1, a(C_BORDER, al * 0.3f));

        if (del) {
            String dt = "Delete?";
            NanoVGRenderer.drawText(dt, px + PAD + 4, y + 10, 11f, a(new Color(255, 100, 100), al), true);
            NanoVGRenderer.drawText("Right-click again to confirm",
                    px + PAD + 4, y + 26, 8f, a(new Color(200, 80, 80), al * 0.75f));
        } else {
            Color tc = sel ? a(C_TEXT, al) : a(new Color(155, 155, 175), al * 0.85f);
            NanoVGRenderer.drawText(pName, px + PAD + 4, y + 10, 11f, tc, sel);
        }
    }

    private void renderNewProfileRow(float px, float y, float w,
                                      float mx, float my, float al, Color accent) {
        boolean hov = mx >= px && mx <= px + w && my >= y && my <= y + PROF_ROW_H;
        if (hov || namingProfile) {
            NanoVGRenderer.drawRect(px, y, w, PROF_ROW_H,
                    a(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                            namingProfile ? 18 : 10), al));
        }
        NanoVGRenderer.drawRect(px + PAD, y + PROF_ROW_H - 1, w - PAD * 2, 1, a(C_BORDER, al * 0.3f));

        if (namingProfile) {
            NanoVGRenderer.drawText(newProfileName, px + PAD + 4, y + 10, 11f, a(C_TEXT, al));
            if (System.currentTimeMillis() % 1000 < 500) {
                float cx = px + PAD + 4 + NanoVGRenderer.getTextWidth(newProfileName, 11f);
                NanoVGRenderer.drawRect(cx, y + 8, 1.2f, 14f, a(C_TEXT, al));
            }
            NanoVGRenderer.drawText("Enter to save | Esc to cancel",
                    px + PAD + 4, y + 27, 8f, a(C_MUTED, al * 0.6f));
        } else {
            NanoVGRenderer.drawText("+ New Profile", px + PAD + 4, y + 10, 11f,
                    a(hov ? accent : C_MUTED, al * (hov ? 0.9f : 0.55f)));
        }
    }

    private void refreshProfiles() {
        profileList.clear();
        ProfileManager pm = RavenZClient.INSTANCE.getProfileManager();
        if (pm == null) return;
        File dir = pm.getProfileDir();
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File f : files) profileList.add(f.getName().replace(".json", ""));
    }

    private void cancelProfileNaming() {
        namingProfile = false;
        newProfileName = "";
    }

    // ── Settings rendering helpers ─────────────────────────────────────────────
    private float totalSetH() {
        if (selectedMod == null) return 0;
        float h = 8;
        for (Setting s : selectedMod.getSettings()) h += setH(s);
        return h + 8;
    }

    private float setH(Setting s) {
        if (s instanceof BooleanSetting) return SET_BOOL_H;
        if (s instanceof NumberSetting)  return SET_NUM_H;
        if (s instanceof ModeSetting)    return SET_MODE_H;
        if (s instanceof RangeSetting)   return SET_RNG_H;
        if (s instanceof ColorSetting)   return SET_CLR_H;
        if (s instanceof KeybindSetting) return SET_BIND_H;
        return SET_SML_H;
    }

    private void renderSet(Setting s, float x, float y, float w,
                           float mx, float my, float al, Color accent) {
        NanoVGRenderer.drawRect(x - 4, y, w + 8, setH(s) - 2, a(C_MOD_BG, al * 0.38f));

        if      (s instanceof BooleanSetting bs)  renderBool   (bs, x, y, w, al, accent);
        else if (s instanceof NumberSetting  ns)  renderNum    (ns, x, y, w, al, accent);
        else if (s instanceof ModeSetting    ms)  renderMode   (ms, x, y, w, al, accent);
        else if (s instanceof RangeSetting   rs)  renderRange  (rs, x, y, w, al, accent);
        else if (s instanceof ColorSetting   cs)  renderColor  (cs, x, y, w, al, accent);
        else if (s instanceof KeybindSetting ks)  renderKeybind(ks, x, y, w, al, accent);
        else NanoVGRenderer.drawText(s.getName(), x, y + 14, 10f, a(C_TEXT, al));

        NanoVGRenderer.drawRect(x - 4, y + setH(s) - 2, w + 8, 1, a(C_BORDER, al * 0.28f));
    }

    private void renderBool(BooleanSetting s, float x, float y, float w, float al, Color accent) {
        NanoVGRenderer.drawText(s.getName(), x, y + 14, 11f, a(C_TEXT, al));
        float tw = ClientSettingsModule.getToggleWidth();
        float th = ClientSettingsModule.getToggleHeight();
        float tx = x + w - tw, ty = y + (SET_BOOL_H - th) / 2f;
        NanoVGRenderer.drawRoundedRect(tx, ty, tw, th, th / 2f,
                s.getValue() ? a(accent, al) : a(C_TOG_OFF, al));
        float tr = th / 2f - 1.5f;
        float tmx = s.getValue() ? (tx + tw - tr - 2f) : (tx + tr + 2f);
        NanoVGRenderer.drawCircle(tmx, ty + th / 2f, tr, a(Color.WHITE, al));
    }

    private void renderNum(NumberSetting s, float x, float y, float w, float al, Color accent) {
        NanoVGRenderer.drawText(s.getName(), x, y + 12, 11f, a(C_TEXT, al));
        double v = s.getValue();
        String vs = (v % 1 == 0) ? String.valueOf((long) v) : String.format("%.2f", v);
        NanoVGRenderer.drawText(vs, x + w - NanoVGRenderer.getTextWidth(vs, 9.5f), y + 12, 9.5f, a(C_MUTED, al));

        float sh = ClientSettingsModule.getSliderHeight();
        float hr = ClientSettingsModule.getSliderHandleSize();
        float sY = y + 32, sX = x;
        NanoVGRenderer.drawRoundedRect(sX, sY + hr - sh / 2f, w, sh, sh / 2f, a(C_TRACK, al));
        double range = s.getMax() - s.getMin();
        float fr = range > 0 ? (float) ((v - s.getMin()) / range) : 0f;
        float fw = fr * w;
        if (fw > sh) NanoVGRenderer.drawRoundedRect(sX, sY + hr - sh / 2f, fw, sh, sh / 2f, a(accent, al));
        NanoVGRenderer.drawCircle(sX + fw, sY + hr, hr,      a(Color.WHITE, al));
        NanoVGRenderer.drawCircle(sX + fw, sY + hr, hr - 2f, a(accent, al));
    }

    private void renderMode(ModeSetting s, float x, float y, float w, float al, Color accent) {
        NanoVGRenderer.drawText(s.getName(), x, y + 15, 11f, a(C_TEXT, al));
        boolean exp = (s == expandedMode);
        String cur = s.getMode();
        float bw = Math.max(80, Math.min(w * 0.52f, NanoVGRenderer.getTextWidth(cur, 9.5f) + 24));
        float bh = 22f, bx = x + w - bw, by = y + (SET_MODE_H - bh) / 2f;
        Color bc = exp ? a(new Color(42, 42, 58), al) : a(C_INPUT, al);
        Color bb = exp ? a(accent, al) : a(C_BORDER, al * 0.8f);
        NanoVGRenderer.drawRoundedRect(bx, by, bw, bh, 5f, bc);
        NanoVGRenderer.drawRoundedRectOutline(bx, by, bw, bh, 5f, 1f, bb);
        NanoVGRenderer.drawText(cur, bx + 8, by + 7, 9.5f, a(C_TEXT, al));
        arrow(bx + bw - 12, by + bh / 2f, exp, al);
        if (exp) { dropX = bx; dropY = by + bh; dropW = bw; }
    }

    private void renderRange(RangeSetting s, float x, float y, float w, float al, Color accent) {
        NanoVGRenderer.drawText(s.getName(), x, y + 12, 11f, a(C_TEXT, al));
        String rv = String.format("%.1f - %.1f", s.getMinValue(), s.getMaxValue());
        NanoVGRenderer.drawText(rv, x + w - NanoVGRenderer.getTextWidth(rv, 9f), y + 12, 9f, a(C_MUTED, al));

        float sh = ClientSettingsModule.getSliderHeight();
        float hr = ClientSettingsModule.getSliderHandleSize();
        float sY = y + 34, sX = x;
        NanoVGRenderer.drawRoundedRect(sX, sY + hr - sh / 2f, w, sh, sh / 2f, a(C_TRACK, al));
        double range = s.getMax() - s.getMin();
        float minR = range > 0 ? (float) ((s.getMinValue() - s.getMin()) / range) : 0f;
        float maxR = range > 0 ? (float) ((s.getMaxValue() - s.getMin()) / range) : 1f;
        float fillX = sX + minR * w, fillW = (maxR - minR) * w;
        if (fillW > 0) NanoVGRenderer.drawRoundedRect(fillX, sY + hr - sh / 2f, fillW, sh, sh / 2f, a(accent, al));
        NanoVGRenderer.drawCircle(sX + minR * w, sY + hr, hr,      a(Color.WHITE, al));
        NanoVGRenderer.drawCircle(sX + minR * w, sY + hr, hr - 2f, a(accent, al));
        NanoVGRenderer.drawCircle(sX + maxR * w, sY + hr, hr,      a(Color.WHITE, al));
        NanoVGRenderer.drawCircle(sX + maxR * w, sY + hr, hr - 2f, a(accent, al));
    }

    private void renderColor(ColorSetting s, float x, float y, float w, float al, Color accent) {
        NanoVGRenderer.drawText(s.getName(), x, y + 14, 11f, a(C_TEXT, al));
        float sz = 18f, sx = x + w - sz, sy = y + (SET_CLR_H - sz) / 2f;
        NanoVGRenderer.drawRoundedRect(sx, sy, sz, sz, 4f, a(s.getValue(), al));
        NanoVGRenderer.drawRoundedRectOutline(sx, sy, sz, sz, 4f, 1f, a(C_BORDER, al));
    }

    private void renderKeybind(KeybindSetting s, float x, float y, float w, float al, Color accent) {
        NanoVGRenderer.drawText(s.getName(), x, y + 15, 11f, a(C_TEXT, al));
        boolean listen = s.isListening();
        String keyLabel = listen ? "Press key..." : keyName(s.getKeyCode());
        float bw = Math.max(70, NanoVGRenderer.getTextWidth(keyLabel, 9f) + 20);
        float bh = 22f, bx = x + w - bw, by = y + (SET_BIND_H - bh) / 2f;
        NanoVGRenderer.drawRoundedRect(bx, by, bw, bh, 5f,
                listen ? a(new Color(42, 42, 58), al) : a(C_INPUT, al));
        NanoVGRenderer.drawRoundedRectOutline(bx, by, bw, bh, 5f, 1f,
                listen ? a(accent, al) : a(C_BORDER, al * 0.8f));
        NanoVGRenderer.drawText(keyLabel,
                bx + (bw - NanoVGRenderer.getTextWidth(keyLabel, 9f)) / 2f,
                by + 7, 9f, listen ? a(accent, al) : a(C_TEXT, al * 0.9f));
    }

    private String keyName(int keyCode) {
        if (keyCode <= -100) {
            int mouseButton = -100 - keyCode;
            if (mouseButton >= GLFW.GLFW_MOUSE_BUTTON_1 && mouseButton <= GLFW.GLFW_MOUSE_BUTTON_8) {
                return "M" + (mouseButton + 1);
            }
        }
        if (keyCode < GLFW.GLFW_KEY_UNKNOWN) {
            int mouseButton = -(keyCode + 1);
            if (mouseButton >= GLFW.GLFW_MOUSE_BUTTON_1 && mouseButton <= GLFW.GLFW_MOUSE_BUTTON_8) {
                return "M" + (mouseButton + 1);
            }
        }
        if (keyCode >= GLFW.GLFW_MOUSE_BUTTON_2 && keyCode <= GLFW.GLFW_MOUSE_BUTTON_8) {
            return "M" + (keyCode + 1);
        }
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode == 0) return "NONE";
        String n = GLFW.glfwGetKeyName(keyCode, 0);
        if (n != null && !n.isBlank()) return n.toUpperCase();
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE         -> "SPACE";
            case GLFW.GLFW_KEY_ESCAPE        -> "ESC";
            case GLFW.GLFW_KEY_ENTER         -> "ENTER";
            case GLFW.GLFW_KEY_TAB           -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE     -> "BKSP";
            case GLFW.GLFW_KEY_LEFT_SHIFT,
                 GLFW.GLFW_KEY_RIGHT_SHIFT   -> "SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL,
                 GLFW.GLFW_KEY_RIGHT_CONTROL -> "CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT,
                 GLFW.GLFW_KEY_RIGHT_ALT     -> "ALT";
            case GLFW.GLFW_KEY_F1  -> "F1";  case GLFW.GLFW_KEY_F2  -> "F2";
            case GLFW.GLFW_KEY_F3  -> "F3";  case GLFW.GLFW_KEY_F4  -> "F4";
            case GLFW.GLFW_KEY_F5  -> "F5";  case GLFW.GLFW_KEY_F6  -> "F6";
            case GLFW.GLFW_KEY_F7  -> "F7";  case GLFW.GLFW_KEY_F8  -> "F8";
            case GLFW.GLFW_KEY_F9  -> "F9";  case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11"; case GLFW.GLFW_KEY_F12 -> "F12";
            default -> "K" + keyCode;
        };
    }

    private boolean captureMouseKeybind(int button) {
        if (button < GLFW.GLFW_MOUSE_BUTTON_1 || button > GLFW.GLFW_MOUSE_BUTTON_8) return false;
        KeybindSetting listening = getListeningKeybind();
        if (listening == null) return false;
        listening.setKeyCode(-100 - button);
        listening.setListening(false);
        return true;
    }

    private KeybindSetting getListeningKeybind() {
        if (selectedMod == null) return null;
        for (Setting s : selectedMod.getSettings()) {
            if (s instanceof KeybindSetting kb && kb.isListening()) return kb;
        }
        return null;
    }

    private void renderDropdown(float al) {
        List<String> modes = expandedMode.getModes();
        float oh = 22f, totalH = modes.size() * oh + 4;
        Color accent = ClientSettingsModule.getAccentColor();
        NanoVGRenderer.drawRoundedRect(dropX, dropY, dropW, totalH, 5f, a(C_DROP, al));
        NanoVGRenderer.drawRoundedRectOutline(dropX, dropY, dropW, totalH, 5f, 1f, a(C_BORDER, al));
        float oy = dropY + 2;
        for (String mode : modes) {
            boolean cur = mode.equals(expandedMode.getMode());
            if (cur) NanoVGRenderer.drawRoundedRect(dropX + 2, oy, dropW - 4, oh - 2, 4f,
                    a(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55), al));
            NanoVGRenderer.drawText(mode, dropX + 8, oy + 7, 9.5f,
                    cur ? a(accent, al) : a(C_TEXT, al * 0.85f));
            oy += oh;
        }
    }

    private void arrow(float cx, float cy, boolean up, float al) {
        float sz = 3.5f;
        Color c = a(C_MUTED, al);
        if (up) {
            NanoVGRenderer.drawLine(cx - sz, cy + sz * 0.5f, cx, cy - sz * 0.5f, 1.3f, c);
            NanoVGRenderer.drawLine(cx, cy - sz * 0.5f, cx + sz, cy + sz * 0.5f, 1.3f, c);
        } else {
            NanoVGRenderer.drawLine(cx - sz, cy - sz * 0.5f, cx, cy + sz * 0.5f, 1.3f, c);
            NanoVGRenderer.drawLine(cx, cy + sz * 0.5f, cx + sz, cy - sz * 0.5f, 1.3f, c);
        }
    }

    // ── Skin loading ───────────────────────────────────────────────────────────
    private void updateSkinImage() {
        try {
            if (client == null || client.player == null) return;
            net.minecraft.util.Identifier skinId = null;
            if (client.getNetworkHandler() != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null && entry.getSkinTextures() != null && entry.getSkinTextures().body() != null) {
                    skinId = entry.getSkinTextures().body().texturePath();
                }
            }
            if (skinId == null) {
                skinId = DefaultSkinHelper.getSkinTextures(client.player.getUuid()).body().texturePath();
            }
            if (skinId == null) return;
            String path = skinId.toString();
            if (!path.equals(cachedSkinPath)) {
                if (cachedSkinImage != -1) NanoVGRenderer.deleteImage(cachedSkinImage);
                cachedSkinImage = NanoVGRenderer.loadImage(path);
                cachedSkinPath = path;
            }
        } catch (Exception ignored) {}
    }

    private void renderPlayerHead(float x, float y, float size, float al) {
        if (cachedSkinImage == -1) return;

        float inset = 3f;
        float headX = x + inset;
        float headY = y + inset;
        float headSize = size - inset * 2f;
        Color tint = a(Color.WHITE, al);

        NanoVGRenderer.save();
        NanoVGRenderer.scissor(headX, headY, headSize, headSize);
        // Base face layer
        NanoVGRenderer.drawImageRegion(cachedSkinImage,
                8, 8, 8, 8, 64, 64,
                headX, headY, headSize, headSize,
                tint);
        // Hat/outer layer for the full player head look
        NanoVGRenderer.drawImageRegion(cachedSkinImage,
                40, 8, 8, 8, 64, 64,
                headX, headY, headSize, headSize,
                tint);
        NanoVGRenderer.resetScissor();
        NanoVGRenderer.restore();
    }

    // ── Input ──────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        float sc = ease(anim) * guiScale();
        int cx = width / 2, cy = height / 2;
        float mx = tmx((float) mouseX, cx, sc), my = tmy((float) mouseY, cy, sc);
        float gx = cx - GUI_W / 2f, gy = cy - GUI_H / 2f;

        if (captureMouseKeybind(button)) return true;

        if (expandedMode != null) {
            if (clickDropdown(mx, my)) return true;
            expandedMode = null;
        }

        if (clickLeft  (mx, my, gx, gy, button))                      return true;
        if (clickMiddle(mx, my, gx + LEFT_W + 1, gy, button))         return true;
        if (clickRight (mx, my, gx + LEFT_W + MID_W + 1, gy, button)) return true;

        searchActive = false;
        return super.mouseClicked(click, doubleClick);
    }

    private boolean clickDropdown(float mx, float my) {
        List<String> modes = expandedMode.getModes();
        float oh = 22f, totalH = modes.size() * oh + 4;
        if (mx >= dropX && mx <= dropX + dropW && my >= dropY && my <= dropY + totalH) {
            int idx = (int) ((my - dropY - 2) / oh);
            if (idx >= 0 && idx < modes.size()) expandedMode.setMode(modes.get(idx));
            expandedMode = null;
            return true;
        }
        expandedMode = null;
        return false;
    }

    private boolean clickLeft(float mx, float my, float gx, float gy, int button) {
        if (mx < gx || mx > gx + LEFT_W) return false;

        if (button == 0) {
            Category[] main = { Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC };
            float y = gy + PROFILE_H + SECTION_H;
            for (Category c : main) {
                if (my >= y && my <= y + CAT_H) { switchCat(c); return true; }
                y += CAT_H;
            }
            y += SECTION_H + 8;
            if (my >= y && my <= y + CAT_H) { switchCat(Category.CLIENT); return true; }
        }

        // Bottom buttons
        float btnH = 28f, btnY = gy + GUI_H - 44;
        float btnW = (LEFT_W - PAD * 2 - 5) / 2f;
        if (my >= btnY && my <= btnY + btnH && button == 0) {
            if (mx >= gx + PAD && mx <= gx + PAD + btnW) {
                creditsOpen = !creditsOpen;
                configOpen  = false;
                cancelProfileNaming();
                deleteConfirmPrf = null;
                if (creditsOpen) selectedMod = null;
                return true;
            }
            float cfgX = gx + PAD + btnW + 5;
            if (mx >= cfgX && mx <= cfgX + btnW) {
                configOpen  = !configOpen;
                creditsOpen = false;
                deleteConfirmPrf = null;
                if (configOpen) {
                    selectedMod = null;
                    refreshProfiles();
                } else {
                    cancelProfileNaming();
                }
                return true;
            }
        }
        return false;
    }

    private void switchCat(Category c) {
        if (c == selectedCat && !creditsOpen && !configOpen) return;
        creditsOpen = false;
        configOpen = false;
        cancelProfileNaming();
        deleteConfirmPrf = null;
        selectedCat = c; modScroll = 0; search = ""; autoSelectMod(); setScroll = 0;
    }

    private boolean clickMiddle(float mx, float my, float px, float gy, int button) {
        float w = MID_W - 1;
        float sbW = w - PAD * 2, sbH = 26;
        float sbX = px + PAD, sbY = gy + MOD_HDR_H - sbH - 8;
        if (button == 0 && mx >= sbX && mx <= sbX + sbW && my >= sbY && my <= sbY + sbH) {
            searchActive = true; return true;
        }
        searchActive = false;

        float listY = gy + MOD_HDR_H + 1, listH = GUI_H - MOD_HDR_H - 1;
        if (mx < px || mx > px + w || my < listY || my > listY + listH) return false;

        List<Module> mods = displayMods();
        float ry = listY - modScroll;
        for (Module m : mods) {
            if (my >= ry && my <= ry + MOD_ROW_H) {
                long window = client != null ? client.getWindow().getHandle() : 0L;
                boolean ctrlDown = window != 0L && (
                        GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
                );
                boolean settingsClick = button == 1 || (button == 0 && ctrlDown);
                if (settingsClick) {
                    openModuleSettings(m);
                    return true;
                }

                if (button != 0) return false;

                float dotX = px + w - PAD - 11;
                if (mx >= dotX - 6) {
                    openModuleSettings(m);
                } else {
                    m.toggle();
                }
                return true;
            }
            ry += MOD_ROW_H;
        }
        return false;
    }

    private void openModuleSettings(Module module) {
        creditsOpen = false;
        configOpen = false;
        cancelProfileNaming();
        deleteConfirmPrf = null;
        selectedMod = module;
        setScroll = 0;
        expandedMode = null;
    }

    private boolean clickRight(float mx, float my, float px, float gy, int button) {
        // Config panel clicks
        if (configOpen) {
            float w = RIGHT_W - 1;
            float bH = 22f, bW = 42f;
            float saveBX = px + w - PAD - bW, saveBY = gy + (SET_HDR_H - bH) / 2f;
            if (button == 0 && mx >= saveBX && mx <= saveBX + bW && my >= saveBY && my <= saveBY + bH) {
                ProfileManager pm = RavenZClient.INSTANCE.getProfileManager();
                if (pm != null) {
                    pm.saveProfile(selectedProfile != null ? selectedProfile : "quicksave", true);
                    refreshProfiles();
                }
                return true;
            }
            if (selectedProfile != null) {
                float loadBX = saveBX - 6 - bW, loadBY = saveBY;
                if (button == 0 && mx >= loadBX && mx <= loadBX + bW && my >= loadBY && my <= loadBY + bH) {
                    ProfileManager pm = RavenZClient.INSTANCE.getProfileManager();
                    if (pm != null) pm.loadProfile(selectedProfile);
                    return true;
                }
            }

            float listY = gy + SET_HDR_H + 1, listH = GUI_H - SET_HDR_H - 1;
            if (mx < px || mx > px + w || my < listY || my > listY + listH) return false;

            float ry = listY - profileScroll;
            for (String pName : profileList) {
                if (my >= ry && my < ry + PROF_ROW_H) {
                    if (button == 0) {
                        selectedProfile = pName;
                        deleteConfirmPrf = null;
                        namingProfile = false;
                    } else if (button == 1) {
                        boolean confirming = pName.equals(deleteConfirmPrf)
                                && System.currentTimeMillis() - deleteConfirmMs < DELETE_TIMEOUT;
                        if (confirming) {
                            ProfileManager pm = RavenZClient.INSTANCE.getProfileManager();
                            if (pm != null) {
                                File f = new File(pm.getProfileDir(), pName + ".json");
                                if (f.delete()) {
                                    if (pName.equals(selectedProfile)) selectedProfile = null;
                                    deleteConfirmPrf = null;
                                    refreshProfiles();
                                }
                            }
                        } else {
                            deleteConfirmPrf = pName;
                            deleteConfirmMs  = System.currentTimeMillis();
                        }
                    }
                    return true;
                }
                ry += PROF_ROW_H;
            }
            // "+ New" row
            if (my >= ry && my < ry + PROF_ROW_H && button == 0) {
                if (!namingProfile) { namingProfile = true; newProfileName = ""; selectedProfile = null; }
                return true;
            }
            return false;
        }

        if (creditsOpen || selectedMod == null) return false;
        float w = RIGHT_W - 1;
        float listY = gy + SET_HDR_H + 1, listH = GUI_H - SET_HDR_H - 1;
        if (mx < px || mx > px + w || my < listY || my > listY + listH) return false;

        float rx = px + PAD, rw = w - PAD * 2;
        float sy = listY + 8 - setScroll;

        for (Setting s : selectedMod.getSettings()) {
            float rh = setH(s);
            if (my >= sy && my < sy + rh) {
                if (s instanceof BooleanSetting bs) { bs.toggle(); return true; }
                if (s instanceof NumberSetting ns) {
                    float hr = ClientSettingsModule.getSliderHandleSize();
                    float sY = sy + 32;
                    if (my >= sY - 5 && my <= sY + hr * 2 + 5) {
                        numTrackX = rx; numTrackW = rw; draggingNum = ns;
                        ns.setValue(ns.getMin() + clamp((mx - rx) / rw, 0, 1) * (ns.getMax() - ns.getMin()));
                        return true;
                    }
                }
                if (s instanceof ModeSetting ms) {
                    float bw = Math.max(80, Math.min(rw * 0.52f, NanoVGRenderer.getTextWidth(ms.getMode(), 9.5f) + 24));
                    float bh = 22f, bx = rx + rw - bw, by = sy + (SET_MODE_H - bh) / 2f;
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                        expandedMode = (expandedMode == ms) ? null : ms;
                        dropX = bx; dropY = by + bh; dropW = bw;
                        return true;
                    }
                }
                if (s instanceof RangeSetting rs) {
                    float hr = ClientSettingsModule.getSliderHandleSize();
                    float sY = sy + 34;
                    if (my >= sY - 5 && my <= sY + hr * 2 + 5) {
                        double range = rs.getMax() - rs.getMin();
                        float minR = range > 0 ? (float) ((rs.getMinValue() - rs.getMin()) / range) : 0f;
                        float maxR = range > 0 ? (float) ((rs.getMaxValue() - rs.getMin()) / range) : 1f;
                        draggingRange = rs;
                        draggingRangeMin = Math.abs(mx - (rx + minR * rw)) <= Math.abs(mx - (rx + maxR * rw));
                        rangeTrackX = rx; rangeTrackW = rw;
                        return true;
                    }
                }
                if (s instanceof KeybindSetting kb) {
                    String kl = kb.isListening() ? "Press key..." : keyName(kb.getKeyCode());
                    float bw = Math.max(70, NanoVGRenderer.getTextWidth(kl, 9f) + 20);
                    float bh = 22f, bx = rx + rw - bw, by = sy + (SET_BIND_H - bh) / 2f;
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                        for (Setting o : selectedMod.getSettings())
                            if (o instanceof KeybindSetting ok && ok != kb) ok.setListening(false);
                        kb.toggleListening();
                        return true;
                    }
                }
                return false;
            }
            sy += rh;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dX, double dY) {
        double mouseX = click.x();
        double mouseY = click.y();
        float sc = ease(anim) * guiScale();
        int cx = width / 2, cy = height / 2;
        float mx = tmx((float) mouseX, cx, sc);
        int button = click.button();
        if (draggingNum != null) {
            float r = clamp((mx - numTrackX) / numTrackW, 0, 1);
            draggingNum.setValue(draggingNum.getMin() + r * (draggingNum.getMax() - draggingNum.getMin()));
            return true;
        }
        if (draggingRange != null) {
            double v = draggingRange.getMin() + clamp((mx - rangeTrackX) / rangeTrackW, 0, 1)
                    * (draggingRange.getMax() - draggingRange.getMin());
            if (draggingRangeMin) draggingRange.setMinValue(v);
            else                  draggingRange.setMaxValue(v);
            return true;
        }
        return super.mouseDragged(click, dX, dY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingNum = null; draggingRange = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        float sc = ease(anim) * guiScale();
        int cx = width / 2, cy = height / 2;
        float mx = tmx((float) mouseX, cx, sc), my = tmy((float) mouseY, cy, sc);
        float gx = cx - GUI_W / 2f, gy = cy - GUI_H / 2f;

        // Config panel scroll
        if (configOpen) {
            float rX = gx + LEFT_W + MID_W + 1;
            float rListY = gy + SET_HDR_H + 1, rListH = GUI_H - SET_HDR_H - 1f;
            if (mx >= rX && mx <= rX + RIGHT_W - 1 && my >= rListY && my <= rListY + rListH) {
                float th = (profileList.size() + 1) * PROF_ROW_H;
                profileScroll = clamp(profileScroll - (float) vAmt * 15, 0, Math.max(0, th - rListH));
                return true;
            }
        }

        // Middle panel scroll
        float midX = gx + LEFT_W + 1, listY = gy + MOD_HDR_H + 1, listH = GUI_H - MOD_HDR_H - 1f;
        if (mx >= midX && mx <= midX + MID_W - 1 && my >= listY && my <= listY + listH) {
            float th = displayMods().size() * MOD_ROW_H;
            modScroll = clamp(modScroll - (float) vAmt * 15, 0, Math.max(0, th - listH));
            return true;
        }

        // Right panel (settings) scroll
        float rX = gx + LEFT_W + MID_W + 1, rListY = gy + SET_HDR_H + 1, rListH = GUI_H - SET_HDR_H - 1f;
        if (!configOpen && mx >= rX && mx <= rX + RIGHT_W - 1 && my >= rListY && my <= rListY + rListH) {
            float th = totalSetH();
            setScroll = clamp(setScroll - (float) vAmt * 15, 0, Math.max(0, th - rListH));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        int modifiers = input.modifiers();
        if (namingProfile) {
            if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-') {
                newProfileName += chr; return true;
            }
            return false;
        }
        if (searchActive) { search += chr; modScroll = 0; return true; }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        // Profile naming
        if (namingProfile) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!newProfileName.isEmpty())
                    newProfileName = newProfileName.substring(0, newProfileName.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (!newProfileName.isEmpty()) {
                    ProfileManager pm = RavenZClient.INSTANCE.getProfileManager();
                    if (pm != null) { pm.saveProfile(newProfileName); refreshProfiles(); }
                    newProfileName = "";
                }
                namingProfile = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { namingProfile = false; newProfileName = ""; return true; }
        }

        if (searchActive) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { searchActive = false; return true; }
        }

        // Keybind listening
        if (selectedMod != null) {
            for (Setting s : selectedMod.getSettings()) {
                if (s instanceof KeybindSetting kb && kb.isListening()) {
                    if (keyCode != GLFW.GLFW_KEY_ESCAPE) kb.setKeyCode(keyCode);
                    kb.setListening(false);
                    return true;
                }
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override public void  close()              { closing = true; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause()      { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (ClientSettingsModule.isGuiBlurEnabled()) super.renderBackground(context, mouseX, mouseY, delta);
    }

    private void renderNoRendererFallback(DrawContext context) {
        if (client == null) return;
        int cx = width / 2;
        int cy = height / 2;
        context.drawCenteredTextWithShadow(client.textRenderer,
                Text.literal("Rich GUI is open, but renderer is unavailable."),
                cx, cy - 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(client.textRenderer,
                Text.literal("This build has NanoVG removed."),
                cx, cy + 6, 0xA8A8A8);
    }

    // ── Utilities ──────────────────────────────────────────────────────────────
    private float ease(float t) { return 1f - (float) Math.pow(1 - t, 3); }
    private float lerp(float a, float b, float t) { return a + (b - a) * Math.min(1f, t); }

    private Color a(Color c, float al) {
        int alpha = Math.max(0, Math.min(255, (int) (c.getAlpha() * al)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private float tmx(float mx, int cx, float sc) { return (mx - cx) / sc + cx; }
    private float tmy(float my, int cy, float sc) { return (my - cy) / sc + cy; }
    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    private float guiScale() {
        return switch (ClientSettingsModule.getGuiScale()) {
            case 0  -> 0.75f;
            case 2  -> 1.25f;
            case 3  -> 1.5f;
            default -> 1.0f;
        };
    }

    private String truncate(String s, float maxW, float fontSize) {
        if (NanoVGRenderer.getTextWidth(s, fontSize) <= maxW) return s;
        while (s.length() > 1 && NanoVGRenderer.getTextWidth(s + "..", fontSize) > maxW)
            s = s.substring(0, s.length() - 1);
        return s + "..";
    }
}
