package dev.flr.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class ClientEvents {
    private static int frames;
    private static long windowStart = System.nanoTime();

    private ClientEvents() {}

    public static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(FlrKeys.MENU);
        e.register(FlrKeys.MASTER);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (PlayerSkin.Model m : e.getSkins()) {
            EntityRenderer<? extends Player> r = e.getSkin(m);
            if (r instanceof PlayerRenderer pr) pr.addLayer(new CosmeticsLayer(pr));
        }
    }

    public static void gui(RenderGuiEvent.Post e) {
        frames++;
        long now = System.nanoTime();
        if (now - windowStart >= 1_000_000_000L) {
            Config.fps = frames;
            frames = 0;
            windowStart = now;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!Config.hud || mc.screen != null || mc.options.hideGui) return;
        String txt = "FLR | " + (Config.master ? Config.THEME_NAMES[Config.theme] : "OFF") + " | " + Config.fps + " fps";
        e.getGuiGraphics().drawString(mc.font, txt, 4, 4, 0xFF000000 | Config.color(), true);
    }

    /** Fog colour tinted towards the theme colour. */
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        if (!Config.master || Config.fogTint <= 0) return;
        if (e.getCamera().getFluidInCamera() != FogType.NONE) return;
        int c = Config.color();
        float k = (float) Config.fogTint * 0.6f;
        e.setRed(Mth.lerp(k, e.getRed(), ((c >> 16) & 255) / 255f));
        e.setGreen(Mth.lerp(k, e.getGreen(), ((c >> 8) & 255) / 255f));
        e.setBlue(Mth.lerp(k, e.getBlue(), (c & 255) / 255f));
    }

    /** Atmospheric fog: pulls the fog distance in so the world looks soft and misty. */
    public static void fogRender(ViewportEvent.RenderFog e) {
        if (!Config.master || Config.fogDensity <= 0) return;
        if (e.getType() != FogType.NONE) return;
        float far = e.getFarPlaneDistance();
        if (far < 40f) return; // blindness / darkness / special fog: leave vanilla alone
        float k = 1f - (float) Config.fogDensity * 0.55f;
        float newFar = far * k;
        float near = Mth.lerp((float) Config.fogDensity, e.getNearPlaneDistance(), newFar * 0.12f);
        if (e.getMode() == FogRenderer.FogMode.FOG_SKY) near = 0f;
        e.setNearPlaneDistance(near);
        e.setFarPlaneDistance(newFar);
        e.setCanceled(true);
    }
}
