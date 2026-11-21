package com.coolfreeze23.vbparity.variants.sync;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** VARIANTS-22, client side: applies the server's datapack variants, and drops them again when leaving that server. */
@EventBusSubscriber(modid = "vbparity", value = Dist.CLIENT)
public final class VariantSyncClient {
    private static boolean applied;

    private VariantSyncClient() {
    }

    static void apply(VariantSyncPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        // Singleplayer and the LAN host share the server's static registries: they already hold these entries, with
        // their spawn conditions, and must keep them.
        if (connection == null || minecraft.isLocalServer()) {
            return;
        }
        for (VariantRegistrySync.Synced<?> synced : VariantRegistrySync.registries()) {
            apply(synced, payload.registries().getOrDefault(synced.name(), List.of()), connection);
        }
        applied = true;
    }

    private static <T> void apply(VariantRegistrySync.Synced<T> synced, List<VariantSyncPayload.Entry> entries, ClientPacketListener connection) {
        VariantRegistrySync.replaceDataDriven(synced, VariantRegistrySync.decode(synced, entries, connection.registryAccess()));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (applied) {
            applied = false;
            for (VariantRegistrySync.Synced<?> synced : VariantRegistrySync.registries()) {
                synced.registry().clearDataDrivenEntries();
            }
        }
    }
}
