package com.arlight.tetris.client;

import com.arlight.tetris.ArlightTetrisMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/** Respaldo cliente para ocultar las capas GUI de Xaero dentro de la arena. */
@EventBusSubscriber(modid = ArlightTetrisMod.MODID, value = Dist.CLIENT)
public final class XaeroMinimapSuppressor {
    private XaeroMinimapSuppressor() {}

    @SubscribeEvent
    public static void hideXaeroLayers(RenderGuiLayerEvent.Pre event) {
        if (!ClientGameState.isInArena()) return;
        ResourceLocation name = event.getName();
        if (name == null) return;

        String namespace = name.getNamespace().toLowerCase(java.util.Locale.ROOT);
        String path = name.getPath().toLowerCase(java.util.Locale.ROOT);
        boolean xaeroLayer = namespace.contains("xaero")
                || path.contains("xaero")
                || namespace.equals("xaerominimap");
        boolean mapLayer = path.contains("minimap")
                || path.contains("waypoint")
                || path.contains("radar")
                || path.contains("map");
        if (xaeroLayer && mapLayer) event.setCanceled(true);
    }
}
