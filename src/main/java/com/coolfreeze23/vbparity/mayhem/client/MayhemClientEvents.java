package com.coolfreeze23.vbparity.mayhem.client;

import com.coolfreeze23.vbparity.mayhem.KnownMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "vbparity", value = Dist.CLIENT)
public final class MayhemClientEvents {
    private MayhemClientEvents() {
    }

    /** MAYHEM-32: the native client sends a tick-end packet at the end of every unpaused client tick. */
    @SubscribeEvent
    public static void clientTickEnd(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (minecraft.level != null && connection != null && !minecraft.isPaused() && connection.hasChannel(KnownMovement.TICK_END_TYPE)) {
            PacketDistributor.sendToServer(KnownMovement.ClientTickEnd.INSTANCE);
        }
    }
}
