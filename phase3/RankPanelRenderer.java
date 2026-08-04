package com.arlight.tetris.portraitfix;

import com.arlight.tetris.ArlightTetrisMod;
import com.arlight.tetris.client.ClientGameState;
import com.arlight.tetris.network.ClientboundArenaSidebarPacket;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;
import java.util.UUID;

/**
 * Renderiza cada ranking como una sola composición fija en el muro:
 * fondo, retratos, nombres y valores. No usa Painting ni ArmorStand, por lo
 * que no desaparece por el culling de entidades al alejarse.
 */
@EventBusSubscriber(modid = ArlightTetrisMod.MODID, value = Dist.CLIENT)
public final class RankPanelRenderer {

    private static final ResourceLocation POINTS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ArlightTetrisMod.MODID, "textures/painting/top_points.png");
    private static final ResourceLocation LINES_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ArlightTetrisMod.MODID, "textures/painting/top_lines.png");
    private static final ResourceLocation WINS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ArlightTetrisMod.MODID, "textures/painting/top_wins.png");

    private static final int TEXTURE_WIDTH = 640;
    private static final int TEXTURE_HEIGHT = 554;
    private static final float PANEL_WIDTH = 15.0F;
    private static final float PANEL_HEIGHT = 13.0F;
    private static final float PANEL_Z_FROM_CENTER = 39.96875F;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int WHITE = 0xFFFFFFFF;

    private RankPanelRenderer() {}

    @SubscribeEvent
    public static void renderPanels(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        ClientboundArenaSidebarPacket sidebar = ClientGameState.arenaSidebar;
        Minecraft minecraft = Minecraft.getInstance();
        if (sidebar == null || !sidebar.inArena() || minecraft.level == null || minecraft.player == null) return;

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        float panelZ = sidebar.arenaCenterZ() + PANEL_Z_FROM_CENTER;
        if (camera.z > panelZ + 1.0D) return;

        renderPanel(pose, buffers, camera,
                sidebar.arenaCenterX() - 22 + 0.5F,
                sidebar.arenaCenterY() + 11 + 0.5F,
                panelZ,
                POINTS_TEXTURE,
                sidebar.topScores(),
                "pts",
                0xFFFFD166);

        renderPanel(pose, buffers, camera,
                sidebar.arenaCenterX() + 0.5F,
                sidebar.arenaCenterY() + 11 + 0.5F,
                panelZ,
                LINES_TEXTURE,
                sidebar.topLines(),
                "líneas",
                0xFF69E8E2);

        renderPanel(pose, buffers, camera,
                sidebar.arenaCenterX() + 22 + 0.5F,
                sidebar.arenaCenterY() + 11 + 0.5F,
                panelZ,
                WINS_TEXTURE,
                sidebar.topWins(),
                "victorias",
                0xFFFF70D7);

        buffers.endBatch();
    }

    private static void renderPanel(PoseStack pose,
                                    MultiBufferSource.BufferSource buffers,
                                    Vec3 camera,
                                    float centerX,
                                    float centerY,
                                    float panelZ,
                                    ResourceLocation texture,
                                    List<ClientboundArenaSidebarPacket.TopEntry> entries,
                                    String unit,
                                    int accentColor) {
        float left = centerX - PANEL_WIDTH / 2.0F;
        float bottom = centerY - PANEL_HEIGHT / 2.0F;
        float right = left + PANEL_WIDTH;
        float top = bottom + PANEL_HEIGHT;

        drawPanelQuad(pose, buffers, camera, texture, left, bottom, right, top, panelZ);

        ClientboundArenaSidebarPacket.TopEntry first = entries.size() > 0 ? entries.get(0) : null;
        ClientboundArenaSidebarPacket.TopEntry second = entries.size() > 1 ? entries.get(1) : null;
        ClientboundArenaSidebarPacket.TopEntry third = entries.size() > 2 ? entries.get(2) : null;

        drawRankSlot(pose, buffers, camera, left, bottom, panelZ, first,
                320, 419, 196, 207, 447, unit, accentColor);
        drawRankSlot(pose, buffers, camera, left, bottom, panelZ, second,
                145, 419, 154, 246, 447, unit, 0xFFD8D8E8);
        drawRankSlot(pose, buffers, camera, left, bottom, panelZ, third,
                495, 419, 154, 246, 447, unit, 0xFFFFA33D);
    }

    private static void drawRankSlot(PoseStack pose,
                                     MultiBufferSource.BufferSource buffers,
                                     Vec3 camera,
                                     float panelLeft,
                                     float panelBottom,
                                     float panelZ,
                                     ClientboundArenaSidebarPacket.TopEntry entry,
                                     int portraitCenterXPx,
                                     int portraitBottomYPx,
                                     int portraitHeightPx,
                                     int nameYPx,
                                     int valueYPx,
                                     String unit,
                                     int color) {
        if (entry == null) {
            drawPanelText(pose, buffers, camera, panelLeft, panelBottom, panelZ - 0.012F,
                    portraitCenterXPx, nameYPx + 54, "Sin registro", 0xFF8C8497, 0.88F);
            return;
        }

        PlayerSkin skin = resolveSkin(entry.playerId(), entry.name());
        if (skin != null && skin.texture() != null) {
            drawSkin(pose, buffers, camera, panelLeft, panelBottom, panelZ - 0.008F,
                    portraitCenterXPx, portraitBottomYPx, portraitHeightPx, skin.texture());
        }

        drawPanelText(pose, buffers, camera, panelLeft, panelBottom, panelZ - 0.014F,
                portraitCenterXPx, nameYPx, trim(entry.name(), 14), color, 0.90F);
        drawPanelText(pose, buffers, camera, panelLeft, panelBottom, panelZ - 0.014F,
                portraitCenterXPx, valueYPx, entry.value() + " " + unit, color, 0.86F);
    }

    private static void drawPanelQuad(PoseStack pose,
                                      MultiBufferSource.BufferSource buffers,
                                      Vec3 camera,
                                      ResourceLocation texture,
                                      float left,
                                      float bottom,
                                      float right,
                                      float top,
                                      float z) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        pose.pushPose();
        try {
            pose.translate(-camera.x, -camera.y, -camera.z);
            PoseStack.Pose current = pose.last();
            vertex(vertices, current, left, bottom, z, 0.0F, 1.0F);
            vertex(vertices, current, right, bottom, z, 1.0F, 1.0F);
            vertex(vertices, current, right, top, z, 1.0F, 0.0F);
            vertex(vertices, current, left, top, z, 0.0F, 0.0F);
        } finally {
            pose.popPose();
        }
    }

    private static void drawSkin(PoseStack pose,
                                 MultiBufferSource.BufferSource buffers,
                                 Vec3 camera,
                                 float panelLeft,
                                 float panelBottom,
                                 float z,
                                 int centerXPx,
                                 int bottomYPx,
                                 int heightPx,
                                 ResourceLocation skinTexture) {
        float targetHeight = imageHeightToWorld(heightPx);
        float pixel = targetHeight / 32.0F;
        float width = 16.0F * pixel;
        float centerX = panelLeft + imageXToWorld(centerXPx);
        float feetY = panelBottom + imageYFromTopToWorld(bottomYPx);

        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(skinTexture));
        pose.pushPose();
        try {
            pose.translate(centerX - width / 2.0F - camera.x, feetY - camera.y, z - camera.z);
            PoseStack.Pose current = pose.last();

            part(vertices, current, 4, 24, 8, 8, 8, 8, pixel, 0.0000F);
            part(vertices, current, 4, 12, 8, 12, 20, 20, pixel, 0.0000F);
            part(vertices, current, 0, 12, 4, 12, 44, 20, pixel, 0.0000F);
            part(vertices, current, 12, 12, 4, 12, 36, 52, pixel, 0.0000F);
            part(vertices, current, 4, 0, 4, 12, 4, 20, pixel, 0.0000F);
            part(vertices, current, 8, 0, 4, 12, 20, 52, pixel, 0.0000F);

            float overlayZ = -0.0015F;
            part(vertices, current, 4, 24, 8, 8, 40, 8, pixel, overlayZ);
            part(vertices, current, 4, 12, 8, 12, 20, 36, pixel, overlayZ);
            part(vertices, current, 0, 12, 4, 12, 44, 36, pixel, overlayZ);
            part(vertices, current, 12, 12, 4, 12, 52, 52, pixel, overlayZ);
            part(vertices, current, 4, 0, 4, 12, 4, 36, pixel, overlayZ);
            part(vertices, current, 8, 0, 4, 12, 4, 52, pixel, overlayZ);
        } finally {
            pose.popPose();
        }
    }

    private static void drawPanelText(PoseStack pose,
                                      MultiBufferSource.BufferSource buffers,
                                      Vec3 camera,
                                      float panelLeft,
                                      float panelBottom,
                                      float z,
                                      int centerXPx,
                                      int centerYPx,
                                      String text,
                                      int color,
                                      float sizeMultiplier) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        float worldX = panelLeft + imageXToWorld(centerXPx);
        float worldY = panelBottom + imageYFromTopToWorld(centerYPx);
        float scale = (PANEL_WIDTH / TEXTURE_WIDTH) * sizeMultiplier;

        pose.pushPose();
        try {
            pose.translate(worldX - camera.x, worldY - camera.y, z - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.scale(-scale, -scale, scale);
            float x = -font.width(text) / 2.0F;
            float y = -font.lineHeight / 2.0F;
            font.drawInBatch(text, x, y, color, false, pose.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
        } finally {
            pose.popPose();
        }
    }

    private static PlayerSkin resolveSkin(UUID playerId, String name) {
        Minecraft minecraft = Minecraft.getInstance();
        GameProfile profile = new GameProfile(playerId, name);

        if (minecraft.player != null && playerId.equals(minecraft.player.getUUID())) {
            profile = minecraft.player.getGameProfile();
        } else if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(playerId);
            if (info != null) profile = info.getProfile();
        }

        PlayerSkin skin = minecraft.getSkinManager().getInsecureSkin(profile);
        return skin == null ? DefaultPlayerSkin.get(profile) : skin;
    }

    private static float imageXToWorld(int xPx) {
        return (xPx / (float) TEXTURE_WIDTH) * PANEL_WIDTH;
    }

    private static float imageYFromTopToWorld(int yPx) {
        return ((TEXTURE_HEIGHT - yPx) / (float) TEXTURE_HEIGHT) * PANEL_HEIGHT;
    }

    private static float imageHeightToWorld(int heightPx) {
        return (heightPx / (float) TEXTURE_HEIGHT) * PANEL_HEIGHT;
    }

    private static void part(VertexConsumer vertices,
                             PoseStack.Pose pose,
                             int x, int y, int width, int height,
                             int u, int v,
                             float pixel,
                             float z) {
        float x0 = x * pixel;
        float x1 = (x + width) * pixel;
        float y0 = y * pixel;
        float y1 = (y + height) * pixel;

        float u0 = u / 64.0F;
        float u1 = (u + width) / 64.0F;
        float v0 = v / 64.0F;
        float v1 = (v + height) / 64.0F;

        vertex(vertices, pose, x0, y0, z, u0, v1);
        vertex(vertices, pose, x1, y0, z, u1, v1);
        vertex(vertices, pose, x1, y1, z, u1, v0);
        vertex(vertices, pose, x0, y1, z, u0, v0);
    }

    private static void vertex(VertexConsumer vertices,
                               PoseStack.Pose pose,
                               float x, float y, float z,
                               float u, float v) {
        vertices.addVertex(pose, x, y, z)
                .setColor(WHITE)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    private static String trim(String value, int max) {
        if (value == null) return "Jugador";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }
}
