package com.coolfreeze23.vbparity.garden.client;

import com.coolfreeze23.vbparity.garden.GardenRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GardenClient {
    private GardenClient() {
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(GardenRegistry.BLOCK_CRUMBLE, new CrumbleParticleProvider());
    }
}
