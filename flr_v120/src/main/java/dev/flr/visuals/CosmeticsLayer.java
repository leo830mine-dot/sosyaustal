package dev.flr.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Solid (non-transparent) cosmetics: angel wings, halo, cape. */
public class CosmeticsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WINGS =
            ResourceLocation.fromNamespaceAndPath(FlrVisuals.MOD_ID, "textures/entity/wings.png");

    public CosmeticsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer pl, float limb,
                       float limbAmt, float pt, float age, float yaw, float pitch) {
        if (!Config.master || pl.isInvisible()) return;
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return;
        double d = pl.distanceToSqr(me);
        if (pl != me) {
            if (!Config.others) return;
            if (d > Config.maxDistance * Config.maxDistance) return;
        }
        // LOD: far players or low fps get simpler geometry
        boolean far = Config.autoPerf && (d > 24 * 24 || Config.fps < 30);
        boolean crouch = pl.isCrouching();
        float t = age * (float) Config.speed;
        if (Config.halo) halo(ps, buf, t, far, crouch);
        if (Config.cape) cape(ps, buf, t, limbAmt, crouch);
        if (Config.wings) wings(ps, buf, light, pl, t, crouch, far);
    }

    // ---------- helpers ----------
    private static int shade(int rgb, float f) {
        int r = (int) (((rgb >> 16) & 255) * f), g = (int) (((rgb >> 8) & 255) * f), b = (int) ((rgb & 255) * f);
        return 0xFF000000 | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int c,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        vc.addVertex(m, x0, y0, z0).setColor(c);
        vc.addVertex(m, x1, y1, z1).setColor(c);
        vc.addVertex(m, x2, y2, z2).setColor(c);
        vc.addVertex(m, x3, y3, z3).setColor(c);
    }

    // ---------- halo: solid ring ----------
    private void halo(PoseStack ps, MultiBufferSource buf, float t, boolean far, boolean crouch) {
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        int base = Config.wingStyle == 0 ? 0xFFD84A : Config.color();
        int top = shade(base, 1.0f), side = shade(base, 0.8f), bottom = shade(base, 0.6f);
        float s = (float) Config.scale;
        ps.pushPose();
        ps.translate(0, -0.7f + Mth.sin(t * 0.1f) * 0.02f + (crouch ? 0.08f : 0f), 0);
        ps.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.07f) * 4f));
        Matrix4f m = ps.last().pose();
        float ro = 0.30f * s, ri = 0.21f * s, h = 0.022f * s;
        int n = far ? 12 : 28;
        for (int i = 0; i < n; i++) {
            float a0 = (float) (Math.PI * 2 * i / n), a1 = (float) (Math.PI * 2 * (i + 1) / n);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(vc, m, top, ro * c0, -h, ro * s0, ro * c1, -h, ro * s1, ri * c1, -h, ri * s1, ri * c0, -h, ri * s0);
            quad(vc, m, bottom, ro * c0, h, ro * s0, ro * c1, h, ro * s1, ri * c1, h, ri * s1, ri * c0, h, ri * s0);
            quad(vc, m, side, ro * c0, -h, ro * s0, ro * c1, -h, ro * s1, ro * c1, h, ro * s1, ro * c0, h, ro * s0);
            quad(vc, m, side, ri * c0, -h, ri * s0, ri * c1, -h, ri * s1, ri * c1, h, ri * s1, ri * c0, h, ri * s0);
        }
        ps.popPose();
    }

    // ---------- cape: solid gradient with centre stripe ----------
    private void cape(PoseStack ps, MultiBufferSource buf, float t, float limbAmt, boolean crouch) {
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        int base = Config.color();
        float s = (float) Config.scale;
        ps.pushPose();
        ps.translate(0, crouch ? 0.05f : 0f, 0.13f);
        ps.mulPose(Axis.XP.rotationDegrees(6f + Mth.clamp(limbAmt, 0f, 1f) * 35f + (crouch ? 25f : 0f) + Mth.sin(t * 0.1f) * 2f));
        Matrix4f m = ps.last().pose();
        float hw = 0.30f * s, st = 0.045f * s, H = 1.05f * s;
        int rows = 7;
        for (int r = 0; r < rows; r++) {
            float y0 = H * r / rows, y1 = H * (r + 1) / rows;
            int c = shade(base, 0.35f + 0.55f * (r + 0.5f) / rows);
            int cs = shade(base, 0.7f + 0.3f * (r + 0.5f) / rows);
            quad(vc, m, c, -hw, y0, 0, -st, y0, 0, -st, y1, 0, -hw, y1, 0);
            quad(vc, m, cs, -st, y0, 0, st, y0, 0, st, y1, 0, -st, y1, 0);
            quad(vc, m, c, st, y0, 0, hw, y0, 0, hw, y1, 0, st, y1, 0);
        }
        ps.popPose();
    }

    // ---------- angel wings: textured feathers, opaque cutout, flap animation ----------
    private void wings(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer pl, float t,
                       boolean crouch, boolean far) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WINGS));
        int col = switch (Config.wingStyle) {
            case 0 -> 0xFFFFFFFF;
            case 1 -> 0xFF4A3A5A;
            default -> 0xFF000000 | Config.color();
        };
        int lit = LightTexture.pack(Math.max(LightTexture.block(light), 10), LightTexture.sky(light));
        boolean air = !pl.onGround();
        float flap = air ? Mth.sin(t * 0.45f) * 1f : Mth.sin(t * 0.08f) * 0.25f;
        float size = (float) (Config.wingSize * Config.scale);
        float span = 1.15f * size, height = 1.3f * size;
        int seg = far ? 3 : 7;
        float w = span / seg;
        float open = crouch ? 52f : 24f + flap * 14f;
        float roll = crouch ? -4f : -16f - flap * 22f;

        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 0.05f, crouch ? 0.08f : 0.03f, 0.15f);
            ps.scale(side, 1f, 1f);
            ps.mulPose(Axis.ZP.rotationDegrees(roll));
            ps.mulPose(Axis.YP.rotationDegrees(-open));
            for (int i = 0; i < seg; i++) {
                PoseStack.Pose p = ps.last();
                float u0 = (float) i / seg, u1 = (float) (i + 1) / seg;
                float yt = -height * 0.62f, yb = height * 0.38f;
                v(vc, p, 0, yt, u0, 0, col, lit);
                v(vc, p, 0, yb, u0, 1, col, lit);
                v(vc, p, w, yb, u1, 1, col, lit);
                v(vc, p, w, yt, u1, 0, col, lit);
                ps.translate(w, 0, 0);
                ps.mulPose(Axis.YP.rotationDegrees(-(3.5f + flap * 3f)));
                ps.mulPose(Axis.ZP.rotationDegrees(1.5f));
            }
            ps.popPose();
        }
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float u, float v, int col, int light) {
        vc.addVertex(p, x, y, 0f).setColor(col).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(p, 0f, 0f, 1f);
    }
}
