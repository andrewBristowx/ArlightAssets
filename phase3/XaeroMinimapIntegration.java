package com.arlight.tetris.integration;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.logging.Logger;

/** Integración opcional con Xaero's Minimap usando su efecto oficial. */
public final class XaeroMinimapIntegration {
    private static final Logger LOGGER = Logger.getLogger("ArlightTetris");
    private static final ResourceLocation NO_MINIMAP_ID =
            ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_minimap");
    private static final ResourceKey<MobEffect> NO_MINIMAP_KEY =
            ResourceKey.create(Registries.MOB_EFFECT, NO_MINIMAP_ID);

    private static boolean availabilityLogged;
    private static boolean available;

    private XaeroMinimapIntegration() {}

    public static void keepHidden(ServerPlayer player) {
        if (player == null) return;
        try {
            Registry<MobEffect> registry = player.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
            Holder.Reference<MobEffect> holder = registry.getHolder(NO_MINIMAP_KEY).orElse(null);
            if (holder == null) {
                logAvailability(false);
                return;
            }
            logAvailability(true);
            player.addEffect(new MobEffectInstance(holder, 60, 0, true, false, false));
        } catch (Throwable ignored) {
            logAvailability(false);
        }
    }

    private static void logAvailability(boolean found) {
        available = available || found;
        if (availabilityLogged && (!found || available)) return;
        availabilityLogged = true;
        if (found) {
            LOGGER.info("Xaero's Minimap detectado: se ocultará en el mundo de ArlightTetris.");
        } else {
            LOGGER.info("Xaero's Minimap no está presente en el servidor; la integración queda inactiva.");
        }
    }
}
