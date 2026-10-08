package dev.flr.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

public final class GameEvents {
    private static int phase;
    private static double acc;
    private static Vec3 lastPos = Vec3.ZERO;

    private GameEvents() {}

    private static ParticleOptions option() {
        return switch (Config.particleType) {
            case 1 -> ParticleTypes.END_ROD;
            case 2 -> ParticleTypes.HEART;
            case 3 -> ParticleTypes.GLOW;
            case 4 -> ParticleTypes.SNOWFLAKE;
            case 5 -> ParticleTypes.ELECTRIC_SPARK;
            default -> {
                int c = Config.color();
                yield new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 0.8f);
            }
        };
    }

    private static void spawn(Minecraft mc, double x, double y, double z, double vx, double vy, double vz) {
        mc.level.addParticle(option(), x, y, z, vx, vy, vz);
    }

    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        while (FlrKeys.MASTER.consumeClick()) {
            Config.master = !Config.master;
            Config.save();
            p.displayClientMessage(Component.literal("FLR Visuals: " + (Config.master ? "ON" : "OFF")), true);
        }
        while (FlrKeys.MENU.consumeClick()) mc.setScreen(new CosmeticsScreen());

        Vec3 pos = p.position();
        boolean off = !Config.master || !Config.particles || mc.isPaused() || p.isSpectator() || p.isInvisible();
        double moved = pos.distanceToSqr(lastPos);
        lastPos = pos;
        if (off) return;

        phase++;
        acc += Config.effectiveBudget() / 20.0;
        int n = Math.min((int) acc, 6);
        acc -= (int) acc;
        if (acc > 3) acc = 3;
        RandomSource r = mc.level.random;
        double sc = Config.scale;

        // halo sparkles
        if (Config.halo && n > 0 && phase % 3 == 0) {
            double a = phase * 0.4;
            spawn(mc, pos.x + Math.cos(a) * 0.3 * sc, pos.y + p.getBbHeight() + 0.25, pos.z + Math.sin(a) * 0.3 * sc, 0, 0.01, 0);
            n--;
        }
        // aura: orbiting ring
        if (Config.aura && n > 0) {
            for (int i = 0; i < Math.max(1, n / 2); i++) {
                double a = phase * 0.15 + i * 2.4;
                spawn(mc, pos.x + Math.cos(a) * 0.8 * sc, pos.y + 0.2 + (phase % 40) / 40.0 * p.getBbHeight(),
                        pos.z + Math.sin(a) * 0.8 * sc, 0, 0.01, 0);
            }
        }
        // trail
        if (Config.trail && moved > 0.0025 && n > 0) {
            for (int i = 0; i < Math.max(1, n / 2); i++)
                spawn(mc, pos.x + (r.nextDouble() - 0.5) * 0.4, pos.y + 0.1, pos.z + (r.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
        // ambient fireflies around the player (atmosphere)
        if (Config.ambient && n > 0 && phase % 2 == 0) {
            for (int i = 0; i < Math.max(1, n / 3); i++) {
                spawn(mc, pos.x + (r.nextDouble() - 0.5) * 14, pos.y + r.nextDouble() * 4 - 0.5, pos.z + (r.nextDouble() - 0.5) * 14,
                        (r.nextDouble() - 0.5) * 0.02, 0.015, (r.nextDouble() - 0.5) * 0.02);
            }
        }
        // hit sparks
        if (Config.hitSparks && p.swinging && p.swingTime == 0 && mc.hitResult instanceof EntityHitResult h) {
            Vec3 l = h.getLocation();
            for (int i = 0; i < 6; i++)
                spawn(mc, l.x, l.y, l.z, (r.nextDouble() - 0.5) * 0.3, r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.3);
        }
    }
}
