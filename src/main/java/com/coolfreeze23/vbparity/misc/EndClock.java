package com.coolfreeze23.vbparity.misc;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Port of the native {@code minecraft:the_end} world clock, which drives the End flashes (ClientLevel ticks
 * EndFlashState with {@code getDefaultClockTime()}). Like native ServerClockManager: it starts at 0 (old worlds get a
 * fresh End clock), advances by one each server tick while the tick rate runs normally and doDaylightCycle (native
 * {@code advance_time}) is on, is saved with the world, and is sent to players when they join, respawn or change
 * dimension, and when the rule changes. Clients advance it by the game-time delta between updates (ClientClockManager).
 */
public final class EndClock {
    private EndClock() {}

    public static final String DATA_NAME = "vbparity_end_clock";

    public static final class Data extends SavedData {
        public long totalTicks;

        public static final SavedData.Factory<Data> FACTORY = new SavedData.Factory<>(Data::new, Data::load);

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            data.totalTicks = tag.getLong("total_ticks");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putLong("total_ticks", this.totalTicks);
            return tag;
        }
    }

    /** Clock state sent to the client: total ticks, the server game time it belongs to, and whether it advances. */
    public record Payload(long totalTicks, long gameTime, boolean advancing) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vbparity", "end_clock"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Payload::totalTicks,
                ByteBufCodecs.VAR_LONG, Payload::gameTime,
                ByteBufCodecs.BOOL, Payload::advancing,
                Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client copy (ClientClockManager): advanced by the game-time delta while the server says the clock runs. */
    public static final class ClientState {
        private static boolean synced;
        private static long totalTicks;
        private static long lastGameTime;
        private static boolean advancing;

        public static void handle(Payload payload) {
            synced = true;
            totalTicks = payload.totalTicks();
            lastGameTime = payload.gameTime();
            advancing = payload.advancing();
        }

        public static void reset() {
            synced = false;
        }

        public static boolean isSynced() {
            return synced;
        }

        public static long tick(long gameTime) {
            long delta = gameTime - lastGameTime;
            lastGameTime = gameTime;
            if (advancing && delta > 0) {
                totalTicks += delta;
            }
            return totalTicks;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data.FACTORY, DATA_NAME);
    }

    /** Native ClockInstance.tick at rate 1, under ServerClockManager.tick's conditions. */
    public static void advance(Data data, boolean advanceTime, boolean runsNormally) {
        if (runsNormally && advanceTime) {
            data.totalTicks++;
            data.setDirty();
        }
    }

    static boolean advanceTime(MinecraftServer server) {
        return server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
    }

    static Payload payload(MinecraftServer server) {
        return new Payload(data(server).totalTicks, server.overworld().getGameTime(), advanceTime(server));
    }

    @EventBusSubscriber(modid = "vbparity")
    public static final class ServerEvents {
        private ServerEvents() {}

        private static Boolean lastAdvanceTime;

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Pre event) {
            MinecraftServer server = event.getServer();
            boolean advance = advanceTime(server);
            advance(data(server), advance, server.tickRateManager().runsNormally());
            if (lastAdvanceTime != null && lastAdvanceTime != advance) {
                server.getPlayerList().getPlayers().forEach(ServerEvents::send);
            }
            lastAdvanceTime = advance;
        }

        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            send(event.getEntity());
        }

        @SubscribeEvent
        public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            send(event.getEntity());
        }

        @SubscribeEvent
        public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            send(event.getEntity());
        }

        private static void send(net.minecraft.world.entity.player.Player player) {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null
                    && serverPlayer.connection.hasChannel(Payload.TYPE)) {
                PacketDistributor.sendToPlayer(serverPlayer, payload(serverPlayer.server));
            }
        }
    }

    @EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").optional().playToClient(Payload.TYPE, Payload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> ClientState.handle(payload)));
        }
    }
}
