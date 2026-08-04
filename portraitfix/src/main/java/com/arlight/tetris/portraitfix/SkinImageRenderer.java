package com.arlight.tetris.portraitfix;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Dibuja directamente la cara frontal de una skin 64x64 como una imagen 2D.
 * No depende de que Minecraft intente renderizar el ArmorStand invisible.
 */
@EventBusSubscriber(modid = ArlightTetrisPortraitFix.MODID, value = Dist.CLIENT)
public final class SkinImageRenderer {
    private static final String MARKER_LARGE = "arlighttetris:portrait_large";
    private static final String MARKER_SMALL = "arlighttetris:portrait_small";
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int WHITE = 0xFFFFFFFF;

    private SkinImageRenderer() {
    }

    @SubscribeEvent
    public static void renderPortraits(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.renderBuffers() == null || minecraft.getSkinManager() == null) return;

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        boolean drewAnything = false;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)) continue;

            ItemStack carrier = stand.getItemBySlot(EquipmentSlot.HEAD);
            Component marker = carrier.get(DataComponents.CUSTOM_NAME);
            if (marker == null) continue;

            String markerText = marker.getString();
            boolean large = MARKER_LARGE.equals(markerText);
            if (!large && !MARKER_SMALL.equals(markerText)) continue;

            ResolvableProfile resolvable = carrier.get(DataComponents.PROFILE);
            if (resolvable == null) continue;

            GameProfile profile = resolvable.gameProfile();
            PlayerSkin skin = minecraft.getSkinManager().getInsecureSkin(profile);
            if (skin == null) skin = DefaultPlayerSkin.get(profile);
            if (skin == null || skin.texture() == null) continue;

            renderPortrait(pose, buffers, camera, stand, skin, large);
            drewAnything = true;
        }

        if (drewAnything) buffers.endBatch();
    }

    private static void renderPortrait(PoseStack pose,
                                       MultiBufferSource.BufferSource buffers,
                                       Vec3 camera,
                                       ArmorStand stand,
                                       PlayerSkin skin,
                                       boolean large) {
        float pixel = large ? 0.1325F : 0.105F;
        float width = 16.0F * pixel;

        RenderType type = RenderType.entityCutoutNoCull(skin.texture());
        VertexConsumer vertices = buffers.getBuffer(type);

        pose.pushPose();
        try {
            // El ancla representa los pies del retrato. El plano queda fijo y
            // paralelo al panel, como si fuera parte de su imagen.
            pose.translate(
                    stand.getX() - camera.x - width / 2.0F,
                    stand.getY() - camera.y,
                    stand.getZ() - camera.z - 0.035D
            );

            PoseStack.Pose current = pose.last();

            // Capa base frontal.
            part(vertices, current, 4, 24, 8, 8, 8, 8, pixel, 0.0000F);
            part(vertices, current, 4, 12, 8, 12, 20, 20, pixel, 0.0000F);
            part(vertices, current, 0, 12, 4, 12, 44, 20, pixel, 0.0000F);
            part(vertices, current, 12, 12, 4, 12, 36, 52, pixel, 0.0000F);
            part(vertices, current, 4, 0, 4, 12, 4, 20, pixel, 0.0000F);
            part(vertices, current, 8, 0, 4, 12, 20, 52, pixel, 0.0000F);

            // Segunda capa de la skin.
            float overlayZ = -0.0020F;
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
}
