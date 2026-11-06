package com.tamid.lahapsid;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ClickGui extends Screen {
    // window size in "local" units; everything is drawn relative to the window's top-left
    static final int W = 420, H = 290, HDR = 28, SIDE = 96;
    static final int BG = 0xFF0A1020, HDRC = 0xFF111E3A, CARD = 0xFF0D162B, BORDER = 0xFF2563EB, LINE = 0xFF1B2F63,
            ACC = 0xFF3B82F6, ACC2 = 0xFF1D4ED8, OFF = 0xFF1E2A44, TXT = 0xFFE6ECFF, DIM = 0xFF8A96B5;
    static final int[] TABC = {0xFFEF4444, 0xFFF59E0B, 0xFF10B981, 0xFFA78BFA};
    static final String[] TABD = {"Auto clicker", "Aim tracker", "HUD and overlays", "Safety and extras"};

    final String[] tabNames = {"Bot", "Aa", "Visuals", "Misc"};
    final List<List<Widget>> pages = new ArrayList<>();
    int tab = 0;
    float tabY = -1;
    float sx, sy;               // window top-left on screen
    float scale = 1f, curS = 1f, cx, cy;
    long openAt = System.currentTimeMillis(), lastNs = System.nanoTime();
    float dt;
    double hx, hy;              // mouse in local coords (for hover)
    boolean moving;
    float moveDX, moveDY;
    Widget dragging;
    Bind listening;

    public ClickGui() { super(Text.literal("Tamid Lahapsid")); }

    // ---------- setup ----------
    @Override
    protected void init() {
        scale = Math.min(1f, Math.min((width - 8f) / W, (height - 8f) / H));
        if (Config.guiX < 0) {
            sx = (width - W * scale) / 2f;
            sy = (height - H * scale) / 2f;
        } else {
            sx = Config.guiX;
            sy = Config.guiY;
        }
        clampPos();

        pages.clear();
        int x = SIDE + 20, w = W - x - 22, y = HDR + 36, st = 21;

        List<Widget> bot = new ArrayList<>();
        bot.add(new Toggle("Activated", x, y, w, () -> Config.botActive, v -> Config.botActive = v));
        bot.add(new Slider("Delay (milliseconds)", x, y + st, w, 100, 1000, 10, () -> Config.botMs, v -> Config.botMs = v, null, ""));
        bot.add(new Toggle("Criticals", x, y + st * 2, w, () -> Config.botCrits, v -> Config.botCrits = v));
        bot.add(new Toggle("Full Charge", x, y + st * 3, w, () -> Config.botCharge, v -> Config.botCharge = v));
        bot.add(new Slider("Slot", x, y + st * 4, w, 0, 9, 1, () -> Config.botSlot, v -> Config.botSlot = v, "Any", ""));
        bot.add(new Bind("Bind", x, y + st * 5, w, () -> Config.botBind, v -> Config.botBind = v, true));
        pages.add(bot);

        List<Widget> aa = new ArrayList<>();
        aa.add(new Toggle("Activated", x, y, w, () -> Config.aimActive, v -> Config.aimActive = v));
        aa.add(new Slider("Radius", x, y + st, w, 5, 100, 1, () -> Config.aimRadius, v -> Config.aimRadius = v, null, ""));
        aa.add(new Slider("FOV", x, y + st * 2, w, 80, 360, 10, () -> Config.aimFov, v -> Config.aimFov = v, null, ""));
        aa.add(new Toggle("Stick Aim", x, y + st * 3, w, () -> Config.stickAim, v -> Config.stickAim = v));
        aa.add(new Toggle("No Aim Through Walls", x, y + st * 4, w, () -> Config.noWalls, v -> Config.noWalls = v));
        aa.add(new Slider("Smooth", x, y + st * 5, w, 0, 10, 1, () -> Config.aimSmooth, v -> Config.aimSmooth = v, "Off", ""));
        aa.add(new Slider("Vertical Speed", x, y + st * 6, w, 0, 10, 1, () -> Config.vSpeed, v -> Config.vSpeed = v, null, ""));
        aa.add(new Slider("Horizontal Speed", x, y + st * 7, w, 0, 10, 1, () -> Config.hSpeed, v -> Config.hSpeed = v, null, ""));
        aa.add(new Slider("Slot", x, y + st * 8, w, 0, 9, 1, () -> Config.aimSlot, v -> Config.aimSlot = v, "Any", ""));
        aa.add(new Bind("Bind", x, y + st * 9, w, () -> Config.aimBind, v -> Config.aimBind = v, true));
        pages.add(aa);

        List<Widget> vis = new ArrayList<>();
        vis.add(new Toggle("HUD", x, y, w, () -> Config.hud, v -> Config.hud = v));
        vis.add(new Toggle("See Armor", x, y + st, w, () -> Config.seeArmor, v -> Config.seeArmor = v));
        vis.add(new Bind("GUI Open Bind", x, y + st * 2, w, () -> Config.guiBind, v -> Config.guiBind = v, false));
        pages.add(vis);

        List<Widget> misc = new ArrayList<>();
        misc.add(new Toggle("Off on death", x, y, w, () -> Config.offOnDeath, v -> Config.offOnDeath = v));
        misc.add(new Toggle("Off on teleport", x, y + st, w, () -> Config.offOnTp, v -> Config.offOnTp = v));
        misc.add(new Toggle("Off on kill", x, y + st * 2, w, () -> Config.offOnKill, v -> Config.offOnKill = v));
        misc.add(new Toggle("Auto Firework", x, y + st * 3, w, () -> Config.fwActive, v -> Config.fwActive = v));
        misc.add(new Slider("Firework Delay", x, y + st * 4, w, 500, 5000, 100, () -> Config.fwDelay, v -> Config.fwDelay = v, null, " ms"));
        misc.add(new Bind("Firework Bind", x, y + st * 5, w, () -> Config.fwBind, v -> Config.fwBind = v, true));
        pages.add(misc);
    }

    void clampPos() {
        float ww = W * scale, hh = H * scale;
        sx = MathHelper.clamp(sx, 0, Math.max(0, width - ww));
        sy = MathHelper.clamp(sy, 0, Math.max(0, height - hh));
    }

    double toLX(double mx) { return (mx - cx) / curS + W / 2.0; }
    double toLY(double my) { return (my - cy) / curS + H / 2.0; }
    int tabTop(int i) { return HDR + 12 + i * 28; }

    // ---------- drawing helpers ----------
    static void rr(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double d = r - i - 0.5;
            int dx = r - (int) Math.round(Math.sqrt(r * r - d * d));
            c.fill(x + dx, y + i, x + w - dx, y + i + 1, col);
            c.fill(x + dx, y + h - 1 - i, x + w - dx, y + h - i, col);
        }
    }

    static int lerpColor(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t), g = (int) (ag + (bg - ag) * t), bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    // ---------- render ----------
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        long nowN = System.nanoTime();
        dt = Math.min((nowN - lastNs) / 1_000_000_000f, 0.1f);
        lastNs = nowN;

        // open animation (grows from 90% to 100%)
        float t = MathHelper.clamp((System.currentTimeMillis() - openAt) / 180f, 0f, 1f);
        float ease = 1f - (1f - t) * (1f - t);
        curS = scale * (0.9f + 0.1f * ease);
        cx = sx + W * scale / 2f;
        cy = sy + H * scale / 2f;
        hx = toLX(mx);
        hy = toLY(my);

        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(cx, cy, 0f);
        m.scale(curS, curS, 1f);
        m.translate(-W / 2f, -H / 2f, 0f);

        // NOTE: no dark overlay behind the window, the game stays fully visible

        // soft glow
        rr(c, -3, -3, W + 6, H + 6, 9, 0x142563EB);
        rr(c, -2, -2, W + 4, H + 4, 8, 0x1C2563EB);
        rr(c, -1, -1, W + 2, H + 2, 7, 0x2A2563EB);

        // panel + border + gradient body
        rr(c, 0, 0, W, H, 6, BORDER);
        rr(c, 1, 1, W - 2, H - 2, 5, BG);
        c.fillGradient(1, HDR + 1, W - 1, H - 7, 0xFF101C38, 0xFF0A1020);

        // header
        rr(c, 1, 1, W - 2, HDR, 5, HDRC);
        c.fill(1, HDR - 5, W - 1, HDR, HDRC);
        c.fill(1, HDR, W - 1, HDR + 1, LINE);
        rr(c, 10, 8, 12, 12, 3, ACC);
        rr(c, 13, 11, 6, 6, 2, 0xFF0A1020);
        c.drawText(textRenderer, "Tamid Lahapsid", 28, 10, TXT, false);

        boolean overClose = hx >= W - 28 && hx <= W - 6 && hy >= 5 && hy <= 23;
        if (overClose) rr(c, W - 28, 5, 22, 18, 4, 0xFFB91C1C);
        c.drawText(textRenderer, "x", W - 19, 10, TXT, false);

        // sidebar with sliding highlight
        float target = tabTop(tab);
        if (tabY < 0) tabY = target;
        tabY += (target - tabY) * Math.min(1f, dt * 18f);
        rr(c, 8, Math.round(tabY), SIDE - 14, 22, 5, 0xFF16275A);
        rr(c, 8, Math.round(tabY) + 4, 3, 14, 1, TABC[tab]);
        for (int i = 0; i < tabNames.length; i++) {
            int ty = tabTop(i);
            boolean over = hx >= 8 && hx <= SIDE - 6 && hy >= ty && hy <= ty + 22;
            if (over && i != tab) rr(c, 8, ty, SIDE - 14, 22, 5, 0x203B82F6);
            rr(c, 17, ty + 8, 6, 6, 3, i == tab ? TABC[i] : lerpColor(OFF, TABC[i], over ? 0.7f : 0.45f));
            c.drawText(textRenderer, tabNames[i], 29, ty + 7, i == tab ? 0xFFFFFFFF : (over ? TXT : DIM), false);
        }

        // content card
        int cardX = SIDE + 6, cardY = HDR + 8, cardW = W - SIDE - 14, cardH = H - HDR - 16;
        rr(c, cardX, cardY, cardW, cardH, 6, LINE);
        rr(c, cardX + 1, cardY + 1, cardW - 2, cardH - 2, 5, CARD);

        // page heading
        int hxp = SIDE + 20;
        c.drawText(textRenderer, tabNames[tab], hxp, HDR + 17, TABC[tab], false);
        c.drawText(textRenderer, TABD[tab], hxp + textRenderer.getWidth(tabNames[tab]) + 8, HDR + 17, DIM, false);
        c.fill(hxp - 6, HDR + 30, W - 18, HDR + 31, LINE);

        for (Widget wd : pages.get(tab)) wd.render(c);

        m.pop();
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float d) { /* clear background, no blur, no dim */ }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (listening != null) { listening = null; return true; }
        if (button != 0) return true;
        double lx = toLX(mx), ly = toLY(my);
        if (lx < 0 || lx > W || ly < 0 || ly > H) return true;

        if (lx >= W - 28 && lx <= W - 6 && ly >= 5 && ly <= 23) { close(); return true; }
        if (ly < HDR) { moving = true; moveDX = (float) mx - sx; moveDY = (float) my - sy; return true; }

        for (int i = 0; i < tabNames.length; i++) {
            int ty = tabTop(i);
            if (lx >= 8 && lx <= SIDE - 6 && ly >= ty && ly <= ty + 22) { tab = i; return true; }
        }
        for (Widget wd : pages.get(tab)) if (wd.click(lx, ly)) return true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (moving) {
            sx = (float) mx - moveDX;
            sy = (float) my - moveDY;
            clampPos();
            return true;
        }
        if (dragging != null) { dragging.drag(toLX(mx)); return true; }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        moving = false;
        dragging = null;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { if (listening.canClear) listening.set.accept(-1); }
            else listening.set.accept(key);
            listening = null;
            return true;
        }
        if (key == Config.guiBind) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        Config.guiX = Math.round(sx);
        Config.guiY = Math.round(sy);
        Config.save();
    }

    @Override public boolean shouldPause() { return false; }

    // ---------- widgets ----------
    abstract class Widget {
        String label; int x, y, w;
        Widget(String l, int x, int y, int w) { label = l; this.x = x; this.y = y; this.w = w; }
        abstract void render(DrawContext c);
        boolean click(double lx, double ly) { return false; }
        void drag(double lx) {}
        boolean inRow(double lx, double ly) { return lx >= x - 6 && lx <= x + w + 4 && ly >= y && ly < y + 18; }
        void hover(DrawContext c) {
            if (inRow(hx, hy)) rr(c, x - 6, y - 1, w + 10, 20, 4, 0x183B82F6);
            c.fill(x - 6, y + 19, x + w + 4, y + 20, 0x0CFFFFFF);
        }
    }

    class Toggle extends Widget {
        BooleanSupplier get; Consumer<Boolean> set;
        float pos; boolean init;
        Toggle(String l, int x, int y, int w, BooleanSupplier g, Consumer<Boolean> s) { super(l, x, y, w); get = g; set = s; }
        void render(DrawContext c) {
            hover(c);
            float target = get.getAsBoolean() ? 1f : 0f;
            if (!init) { pos = target; init = true; }
            pos += (target - pos) * Math.min(1f, dt * 16f);
            c.drawText(textRenderer, label, x, y + 5, lerpColor(DIM, TXT, pos), false);
            int sx2 = x + w - 30;
            rr(c, sx2, y + 2, 30, 14, 7, lerpColor(OFF, ACC, pos));
            rr(c, sx2 + 2 + Math.round(pos * 16), y + 4, 10, 10, 5, 0xFFFFFFFF);
        }
        boolean click(double lx, double ly) {
            if (lx >= x + w - 32 && lx <= x + w + 2 && ly >= y && ly < y + 18) { set.accept(!get.getAsBoolean()); return true; }
            return false;
        }
    }

    class Slider extends Widget {
        int min, max, step; IntSupplier get; IntConsumer set; String zero, suffix;
        float shown; boolean init;
        Slider(String l, int x, int y, int w, int min, int max, int step, IntSupplier g, IntConsumer s, String zero, String suffix) {
            super(l, x, y, w); this.min = min; this.max = max; this.step = step; get = g; set = s;
            this.zero = zero; this.suffix = suffix;
        }
        int tx() { return x + 138; }
        int tw() { return w - 138 - 58; }
        void render(DrawContext c) {
            hover(c);
            c.drawText(textRenderer, label, x, y + 5, TXT, false);
            float target = (get.getAsInt() - min) / (float) (max - min);
            if (!init) { shown = target; init = true; }
            shown += (target - shown) * Math.min(1f, dt * 22f);
            int fill = Math.max(4, Math.round(tw() * shown));
            rr(c, tx(), y + 7, tw(), 4, 2, OFF);
            rr(c, tx(), y + 7, fill, 4, 2, ACC);
            int kx = tx() + Math.round(tw() * shown);
            rr(c, kx - 6, y + 3, 12, 12, 6, ACC);
            rr(c, kx - 4, y + 5, 8, 8, 4, 0xFFFFFFFF);
            boolean z = zero != null && get.getAsInt() == min;
            String v = z ? zero : get.getAsInt() + suffix;
            int pw = textRenderer.getWidth(v) + 10;
            rr(c, x + w - pw, y + 1, pw, 16, 4, OFF);
            c.drawText(textRenderer, v, x + w - pw + 5, y + 5, z ? DIM : TXT, false);
        }
        boolean click(double lx, double ly) {
            if (inRow(lx, ly) && lx >= tx() - 8 && lx <= tx() + tw() + 8) { dragging = this; drag(lx); return true; }
            return false;
        }
        void drag(double lx) {
            double f = MathHelper.clamp((lx - tx()) / tw(), 0, 1);
            int v = (int) Math.round((min + f * (max - min)) / step) * step;
            set.accept(MathHelper.clamp(v, min, max));
        }
    }

    class Bind extends Widget {
        IntSupplier get; IntConsumer set; boolean canClear;
        Bind(String l, int x, int y, int w, IntSupplier g, IntConsumer s, boolean canClear) {
            super(l, x, y, w); get = g; set = s; this.canClear = canClear;
        }
        void render(DrawContext c) {
            hover(c);
            c.drawText(textRenderer, label, x, y + 5, TXT, false);
            int bx = x + w - 84;
            boolean on = listening == this;
            rr(c, bx, y + 1, 84, 16, 4, on ? ACC : LINE);
            rr(c, bx + 1, y + 2, 82, 14, 3, on ? ACC2 : OFF);
            String s;
            if (on) s = "press a key";
            else if (get.getAsInt() < 0) s = "None";
            else s = InputUtil.fromKeyCode(get.getAsInt(), 0).getLocalizedText().getString();
            if (s.length() > 12) s = s.substring(0, 12);
            c.drawText(textRenderer, s, bx + 42 - textRenderer.getWidth(s) / 2, y + 5, TXT, false);
        }
        boolean click(double lx, double ly) {
            if (lx >= x + w - 86 && lx <= x + w + 2 && ly >= y && ly < y + 18) { listening = this; return true; }
            return false;
        }
    }
}
