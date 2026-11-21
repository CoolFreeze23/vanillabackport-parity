package com.coolfreeze23.vbparity.variants.sync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * VARIANTS-22: the datapack entries of VanillaBackport's variant registries, per registry, as JSON in the registry's
 * own codec format with the server-only {@code spawn_conditions} emptied (native's {@code NETWORK_CODEC}s leave them out too).
 */
public record VariantSyncPayload(Map<String, List<Entry>> registries) implements CustomPacketPayload {
    public static final Type<VariantSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vbparity", "variant_registries"));
    private static final int MAX_JSON = 1 << 20;
    public static final StreamCodec<FriendlyByteBuf, VariantSyncPayload> STREAM_CODEC = StreamCodec.of(VariantSyncPayload::write, VariantSyncPayload::read);

    public record Entry(ResourceLocation id, String json) {
    }

    private static void write(FriendlyByteBuf buf, VariantSyncPayload payload) {
        buf.writeVarInt(payload.registries.size());
        payload.registries.forEach((name, entries) -> {
            buf.writeUtf(name);
            buf.writeVarInt(entries.size());
            for (Entry entry : entries) {
                buf.writeResourceLocation(entry.id());
                buf.writeUtf(entry.json(), MAX_JSON);
            }
        });
    }

    private static VariantSyncPayload read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<String, List<Entry>> registries = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf();
            int size = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(size);
            for (int j = 0; j < size; j++) {
                entries.add(new Entry(buf.readResourceLocation(), buf.readUtf(MAX_JSON)));
            }
            registries.put(name, entries);
        }
        return new VariantSyncPayload(registries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
