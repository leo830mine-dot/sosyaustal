package dev.flr.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.util.Mth;
import net.neoforged.fml.loading.FMLPaths;

import java.awt.Color;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class Config {
    public static boolean master = true, others = true, halo = true, wings = true, cape = false;
    public static boolean particles = true, aura = false, trail = false, hitSparks = true, ambient = true;
    public static boolean hud = true, autoPerf = true;
    public static int wingStyle = 0, theme = 0, particleType = 0, particleBudget = 40;
    public static double maxDistance = 48, scale = 1, wingSize = 1, speed = 1, hue = 280, fogTint = 0.2, fogDensity = 0.2;
    public static transient int fps = 60;

    public static final String[] THEME_NAMES = {"Purple", "Cyan", "Pink", "Gold", "Green", "Rainbow", "Custom"};
    public static final int[] THEME_COLORS = {0xB070FF, 0x40E0FF, 0xFF6EC7, 0xFFD24A, 0x5CFF8A};
    public static final String[] PARTICLE_NAMES = {"Dust", "End rod", "Hearts", "Glow", "Snow", "Spark"};
    public static final String[] WING_NAMES = {"Angel", "Dark", "Theme"};

    private Config() {}

    /** RGB (no alpha) of the current theme. */
    public static int color() {
        if (theme < THEME_COLORS.length) return THEME_COLORS[theme];
        if (theme == 5) {
            float h = (float) ((System.currentTimeMillis() / (6000.0 / Math.max(0.1, speed))) % 1.0);
            return Color.HSBtoRGB(h, 0.65f, 1f) & 0xFFFFFF;
        }
        return Color.HSBtoRGB((float) (hue / 360.0), 0.7f, 1f) & 0xFFFFFF;
    }

    /** Particles per second after auto-performance scaling. */
    public static int effectiveBudget() {
        int b = particleBudget;
        if (autoPerf) {
            if (fps < 30) b /= 4;
            else if (fps < 50) b /= 2;
        }
        try {
            if (Minecraft.getInstance().options.particles().get() == ParticleStatus.MINIMAL) b /= 3;
        } catch (Exception ignored) {}
        return b;
    }

    public static void preset(int p) {
        halo = true; wings = p > 0; cape = false; particles = p > 0; ambient = p > 0;
        aura = p == 2; trail = p == 2; hitSparks = p > 0; autoPerf = true;
        particleBudget = new int[]{10, 40, 120}[p];
        fogDensity = new double[]{0, 0.2, 0.35}[p];
        fogTint = new double[]{0, 0.2, 0.35}[p];
        save();
    }

    public static void reset() {
        master = true; others = true; halo = true; wings = true; cape = false;
        particles = true; aura = false; trail = false; hitSparks = true; ambient = true;
        hud = true; autoPerf = true; wingStyle = 0; theme = 0; particleType = 0; particleBudget = 40;
        maxDistance = 48; scale = 1; wingSize = 1; speed = 1; hue = 280; fogTint = 0.2; fogDensity = 0.2;
        save();
    }

    private static List<Field> fields() {
        List<Field> out = new ArrayList<>();
        for (Field f : Config.class.getDeclaredFields()) {
            int m = f.getModifiers();
            if (!Modifier.isStatic(m) || Modifier.isFinal(m) || Modifier.isTransient(m)) continue;
            Class<?> t = f.getType();
            if (t == boolean.class || t == int.class || t == double.class) out.add(f);
        }
        return out;
    }

    private static Path file() { return FMLPaths.CONFIGDIR.get().resolve("flrvisuals.properties"); }

    public static void load() {
        Path p = file();
        if (!Files.exists(p)) { save(); return; }
        Properties pr = new Properties();
        try (Reader r = Files.newBufferedReader(p)) { pr.load(r); } catch (Exception e) { return; }
        for (Field f : fields()) {
            String v = pr.getProperty(f.getName());
            if (v == null) continue;
            try {
                Class<?> t = f.getType();
                if (t == boolean.class) f.setBoolean(null, Boolean.parseBoolean(v.trim()));
                else if (t == int.class) f.setInt(null, Integer.parseInt(v.trim()));
                else f.setDouble(null, Double.parseDouble(v.trim()));
            } catch (Exception ignored) {}
        }
        theme = Mth.clamp(theme, 0, THEME_NAMES.length - 1);
        wingStyle = Mth.clamp(wingStyle, 0, WING_NAMES.length - 1);
        particleType = Mth.clamp(particleType, 0, PARTICLE_NAMES.length - 1);
        particleBudget = Mth.clamp(particleBudget, 0, 300);
        fogDensity = Mth.clamp(fogDensity, 0, 1);
        fogTint = Mth.clamp(fogTint, 0, 1);
    }

    public static void save() {
        Properties pr = new Properties();
        try {
            for (Field f : fields()) pr.setProperty(f.getName(), String.valueOf(f.get(null)));
            try (Writer w = Files.newBufferedWriter(file())) { pr.store(w, "FLR Visuals"); }
        } catch (Exception ignored) {}
    }
}
