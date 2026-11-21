package com.coolfreeze23.vbparity.misc.client;

import com.coolfreeze23.vbparity.misc.EndClock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = "vbparity", value = Dist.CLIENT)
public final class MiscClientGameEvents {
    private MiscClientGameEvents() {}

    /** A new connection starts without an End clock until its server sends one. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EndClock.ClientState.reset();
    }
}
