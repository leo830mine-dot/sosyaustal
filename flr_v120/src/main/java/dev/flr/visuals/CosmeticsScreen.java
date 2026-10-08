package dev.flr.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class CosmeticsScreen extends Screen {
    private static final String[] TABS = {"Cosmetics", "Effects", "Style", "Performance"};
    private static int tab;

    private static final class Row {
        String label;
        int type; // 0 toggle, 1 slider, 2 choice, 3 button
        BooleanSupplier bget; Consumer<Boolean> bset;
        double min, max; DoubleSupplier dget; DoubleConsumer dset; DoubleFunction<String> fmt;
        String[] opts; IntSupplier iget; IntConsumer iset;
        Runnable run;
        int y;
    }

    private final List<List<Row>> pages = new ArrayList<>();
    private int px, py, pw, ph, rx, rw;
    private Row drag;

    public CosmeticsScreen() { super(Component.literal("FLR VISUALS")); }

    private static Row toggle(String l, BooleanSupplier g, Consumer<Boolean> s) {
        Row r = new Row(); r.type = 0; r.label = l; r.bget = g; r.bset = s; return r;
    }
    private static Row slider(String l, double min, double max, DoubleSupplier g, DoubleConsumer s, DoubleFunction<String> f) {
        Row r = new Row(); r.type = 1; r.label = l; r.min = min; r.max = max; r.dget = g; r.dset = s; r.fmt = f; return r;
    }
    private static Row choice(String l, String[] o, IntSupplier g, IntConsumer s) {
        Row r = new Row(); r.type = 2; r.label = l; r.opts = o; r.iget = g; r.iset = s; return r;
    }
    private static Row button(String l, Runnable run) {
        Row r = new Row(); r.type = 3; r.label = l; r.run = run; return r;
    }

    @Override
    protected void init() {
        pages.clear();
        pw = Math.min(width - 10, 340);
        ph = Math.min(height - 10, 232);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        rx = px + 92;
        rw = pw - 100;
        DoubleFunction<String> x2 = v -> String.format("%.2f", v);
        DoubleFunction<String> pct = v -> (int) Math.round(v * 100) + "%";

        pages.add(List.of(
                toggle("Halo", () -> Config.halo, v -> Config.halo = v),
                toggle("Wings", () -> Config.wings, v -> Config.wings = v),
                toggle("Cape", () -> Config.cape, v -> Config.cape = v),
                choice("Wing style", Config.WING_NAMES, () -> Config.wingStyle, v -> Config.wingStyle = v),
                slider("Wing size", 0.6, 1.8, () -> Config.wingSize, v -> Config.wingSize = v, x2),
                slider("Effect scale", 0.6, 1.6, () -> Config.scale, v -> Config.scale = v, x2),
                toggle("Show on other players", () -> Config.others, v -> Config.others = v)));
        pages.add(List.of(
                toggle("Particles", () -> Config.particles, v -> Config.particles = v),
                toggle("Aura", () -> Config.aura, v -> Config.aura = v),
                toggle("Trail", () -> Config.trail, v -> Config.trail = v),
                toggle("Hit sparks", () -> Config.hitSparks, v -> Config.hitSparks = v),
                toggle("Ambient fireflies", () -> Config.ambient, v -> Config.ambient = v),
                choice("Particle style", Config.PARTICLE_NAMES, () -> Config.particleType, v -> Config.particleType = v)));
        pages.add(List.of(
                choice("Theme", Config.THEME_NAMES, () -> Config.theme, v -> Config.theme = v),
                slider("Custom hue", 0, 360, () -> Config.hue, v -> Config.hue = v, v -> (int) v + "\u00b0"),
                slider("Animation speed", 0.2, 3, () -> Config.speed, v -> Config.speed = v, x2),
                slider("Fog tint", 0, 1, () -> Config.fogTint, v -> Config.fogTint = v, pct),
                slider("Fog density", 0, 1, () -> Config.fogDensity, v -> Config.fogDensity = v, pct)));
        pages.add(List.of(
                toggle("Auto performance", () -> Config.autoPerf, v -> Config.autoPerf = v),
                slider("Particle budget", 0, 200, () -> Config.particleBudget, v -> Config.particleBudget = (int) Math.round(v), v -> (int) v + "/s"),
                slider("Max distance", 8, 96, () -> Config.maxDistance, v -> Config.maxDistance = v, v -> (int) v + "m"),
                toggle("HUD badge", () -> Config.hud, v -> Config.hud = v),
                button("Preset: Minimal", () -> Config.preset(0)),
                button("Preset: Balanced", () -> Config.preset(1)),
                button("Preset: Max", () -> Config.preset(2)),
                button("Reset to defaults", Config::reset)));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int accent = 0xFF000000 | Config.color();
        g.fill(0, 0, width, height, 0x90000000);
        g.fill(px, py, px + pw, py + ph, 0xF0101018);
        g.fill(px, py, px + pw, py + 1, accent);
        g.fill(px, py + ph - 1, px + pw, py + ph, accent);
        g.fill(px, py, px + 1, py + ph, accent);
        g.fill(px + pw - 1, py, px + pw, py + ph, accent);
        g.drawString(font, "FLR VISUALS", px + 8, py + 8, accent, true);
        g.drawString(font, "v1.2.0", px + 8, py + 18, 0xFF888888, false);

        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 34 + i * 24;
            boolean sel = i == tab;
            g.fill(px + 6, ty, px + 84, ty + 20, sel ? 0xFF2A2A3C : 0xFF18181F);
            if (sel) g.fill(px + 6, ty, px + 9, ty + 20, accent);
            g.drawString(font, TABS[i], px + 14, ty + 6, sel ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        }

        int y = py + 8;
        for (Row r : pages.get(tab)) {
            r.y = y;
            g.fill(rx, y, rx + rw, y + 22, 0xFF1A1A24);
            if (r.type == 3) {
                g.drawCenteredString(font, r.label, rx + rw / 2, y + 7, 0xFFFFFFFF);
            } else {
                g.drawString(font, r.label, rx + 6, y + 7, 0xFFFFFFFF, false);
                int cx = rx + rw - 6;
                if (r.type == 0) {
                    boolean on = r.bget.getAsBoolean();
                    g.fill(cx - 30, y + 5, cx, y + 17, on ? accent : 0xFF3A3A44);
                    g.drawCenteredString(font, on ? "ON" : "OFF", cx - 15, y + 7, on ? 0xFF000000 : 0xFFCCCCCC);
                } else if (r.type == 1) {
                    int bx = rx + rw / 2, bw = rw / 2 - 10;
                    double f = Mth.clamp((r.dget.getAsDouble() - r.min) / (r.max - r.min), 0, 1);
                    g.fill(bx, y + 14, bx + bw, y + 16, 0xFF44444E);
                    g.fill(bx, y + 14, bx + (int) (bw * f), y + 16, accent);
                    g.fill(bx + (int) (bw * f) - 2, y + 11, bx + (int) (bw * f) + 2, y + 19, 0xFFFFFFFF);
                    String s = r.fmt.apply(r.dget.getAsDouble());
                    g.drawString(font, s, bx + bw - font.width(s), y + 3, 0xFFCCCCCC, false);
                } else {
                    String s = "< " + r.opts[r.iget.getAsInt()] + " >";
                    g.drawString(font, s, cx - font.width(s), y + 7, accent, false);
                }
            }
            y += 24;
        }
    }

    private void applySlider(Row r, double mouseX) {
        int bx = rx + rw / 2, bw = rw / 2 - 10;
        double f = Mth.clamp((mouseX - bx) / bw, 0, 1);
        r.dset.accept(r.min + (r.max - r.min) * f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 34 + i * 24;
            if (mx >= px + 6 && mx <= px + 84 && my >= ty && my <= ty + 20) { tab = i; return true; }
        }
        for (Row r : pages.get(tab)) {
            if (mx < rx || mx > rx + rw || my < r.y || my > r.y + 22) continue;
            switch (r.type) {
                case 0 -> r.bset.accept(!r.bget.getAsBoolean());
                case 1 -> { drag = r; applySlider(r, mx); }
                case 2 -> {
                    int n = r.opts.length;
                    r.iset.accept(Math.floorMod(r.iget.getAsInt() + (btn == 1 ? -1 : 1), n));
                }
                default -> r.run.run();
            }
            Config.save();
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (drag != null) { applySlider(drag, mx); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (drag != null) { drag = null; Config.save(); }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public void onClose() { Config.save(); super.onClose(); }

    @Override
    public boolean isPauseScreen() { return false; }
}
