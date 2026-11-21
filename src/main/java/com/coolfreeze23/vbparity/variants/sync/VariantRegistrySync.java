package com.coolfreeze23.vbparity.variants.sync;

import com.blackgear.platform.core.BuiltInCoreRegistry;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.WolfSoundVariant;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.WolfSoundVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cat.CatDataVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cat.CatDataVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.chicken.ChickenVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.chicken.ChickenVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.frog.FrogDataVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.frog.FrogDataVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilusVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilusVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.wolf.WolfDataVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.wolf.WolfDataVariants;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.variants.mixin.access.BuiltInCoreRegistryAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * VARIANTS-22: native mob variants ({@code cow_variant}, {@code pig_variant}, {@code chicken_variant},
 * {@code wolf_sound_variant}, {@code cat_variant}, {@code frog_variant}, {@code wolf_variant},
 * {@code zombie_nautilus_variant}) are synchronized registries, so datapack variants reach every client.
 * VanillaBackport loads them through Platform's server-only reload listener and only ever sends a variant id, so on a
 * dedicated server a client cannot resolve a datapack variant and draws the default one.
 * <p>This sends the datapack entries of each of those registries to every player on login and after {@code /reload}
 * ({@link OnDatapackSyncEvent}); the client replaces its own datapack entries with them. Spawn conditions are emptied
 * (they hold server-only registry references), as native's network codecs leave them out. In singleplayer and for the
 * LAN host the client shares the server's registries, so nothing is applied there.
 */
@EventBusSubscriber(modid = VBParity.MOD_ID)
public final class VariantRegistrySync {
    private static final String SPAWN_CONDITIONS = "spawn_conditions";

    /** One synchronized VanillaBackport registry, named by the folder its datapack entries load from. */
    public record Synced<T>(String name, BuiltInCoreRegistry<T> registry, Codec<T> codec) {
    }

    /** Built on first use: the registries' own classes may only load once the game's registries are filled. */
    private static final class Holder {
        private static final List<Synced<?>> ALL = List.of(
            new Synced<>("wolf_sound_variant", WolfSoundVariants.REGISTRIES, WolfSoundVariant.CODEC),
            new Synced<>("cow_variant", CowVariants.REGISTRIES, CowVariant.CODEC),
            new Synced<>("chicken_variant", ChickenVariants.REGISTRIES, ChickenVariant.CODEC),
            new Synced<>("pig_variant", PigVariants.REGISTRIES, PigVariant.CODEC),
            new Synced<>("wolf_variants", WolfDataVariants.REGISTRIES, WolfDataVariant.CODEC),
            new Synced<>("frog_variants", FrogDataVariants.REGISTRIES, FrogDataVariant.CODEC),
            new Synced<>("cat_variants", CatDataVariants.REGISTRIES, CatDataVariant.CODEC),
            new Synced<>("zombie_nautilus_variant", ZombieNautilusVariants.REGISTRIES, ZombieNautilusVariant.CODEC)
        );
    }

    public static List<Synced<?>> registries() {
        return Holder.ALL;
    }

    private VariantRegistrySync() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(VariantSyncPayload.TYPE, VariantSyncPayload.STREAM_CODEC, (payload, context) -> {
            if (FMLEnvironment.dist.isClient()) {
                VariantSyncClient.apply(payload);
            }
        });
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        VariantSyncPayload payload = null;
        for (ServerPlayer player : event.getRelevantPlayers().toList()) {
            if (player.connection.hasChannel(VariantSyncPayload.TYPE)) {
                if (payload == null) {
                    payload = build(event.getPlayerList().getServer().registryAccess());
                }
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /** The datapack entries of every synchronized registry, encoded against the server's registries. */
    public static VariantSyncPayload build(HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Map<String, List<VariantSyncPayload.Entry>> out = new LinkedHashMap<>();
        for (Synced<?> synced : registries()) {
            out.put(synced.name(), encode(synced, ops));
        }
        return new VariantSyncPayload(out);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<VariantSyncPayload.Entry> encode(Synced<T> synced, RegistryOps<JsonElement> ops) {
        List<VariantSyncPayload.Entry> entries = new ArrayList<>();
        Map<ResourceLocation, Object> dataDriven = ((BuiltInCoreRegistryAccessor) (Object) synced.registry()).vbp$getDataDriven();
        for (Map.Entry<ResourceLocation, Object> entry : new LinkedHashMap<>(dataDriven).entrySet()) {
            synced.codec().encodeStart(ops, (T) entry.getValue())
                .resultOrPartial(error -> VBParity.LOG.warn("Cannot send {} {}: {}", synced.name(), entry.getKey(), error))
                .ifPresent(json -> {
                    if (json.isJsonObject() && json.getAsJsonObject().has(SPAWN_CONDITIONS)) {
                        json.getAsJsonObject().add(SPAWN_CONDITIONS, new JsonArray());
                    }
                    entries.add(new VariantSyncPayload.Entry(entry.getKey(), json.toString()));
                });
        }
        return entries;
    }

    /** Decodes one registry's entries from a payload against the given (client) registries; entries that fail are skipped. */
    public static <T> Map<ResourceLocation, T> decode(Synced<T> synced, List<VariantSyncPayload.Entry> entries, HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Map<ResourceLocation, T> out = new LinkedHashMap<>();
        for (VariantSyncPayload.Entry entry : entries) {
            try {
                synced.codec().parse(ops, JsonParser.parseString(entry.json()))
                    .resultOrPartial(error -> VBParity.LOG.warn("Cannot read synced {} {}: {}", synced.name(), entry.id(), error))
                    .ifPresent(value -> out.put(entry.id(), value));
            } catch (RuntimeException e) {
                VBParity.LOG.warn("Cannot read synced {} {}", synced.name(), entry.id(), e);
            }
        }
        return out;
    }

    /** Replaces a registry's datapack entries with the decoded ones. */
    public static <T> void replaceDataDriven(Synced<T> synced, Map<ResourceLocation, T> values) {
        synced.registry().clearDataDrivenEntries();
        values.forEach(synced.registry()::registerDataDriven);
    }
}
