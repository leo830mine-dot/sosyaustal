package dev.flr.visuals;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FlrVisuals.MOD_ID, dist = Dist.CLIENT)
public class FlrVisuals {
    public static final String MOD_ID = "flrvisuals";

    public FlrVisuals(IEventBus modBus) {
        Config.load();
        modBus.addListener(ClientEvents::registerKeys);
        modBus.addListener(ClientEvents::addLayers);
        NeoForge.EVENT_BUS.addListener(GameEvents::tick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::gui);
        NeoForge.EVENT_BUS.addListener(ClientEvents::fogColor);
        NeoForge.EVENT_BUS.addListener(ClientEvents::fogRender);
    }
}
