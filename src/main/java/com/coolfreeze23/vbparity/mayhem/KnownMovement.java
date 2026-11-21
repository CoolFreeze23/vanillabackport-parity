package com.coolfreeze23.vbparity.mayhem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The current game's known-movement reset (MAYHEM-32). The client sends a tick-end packet at the end of every client
 * tick (Minecraft.tick: {@code connection.send(ServerboundClientTickEndPacket.INSTANCE)} unless paused), and the server
 * reads a player's known movement as zero after a tick end that had no movement packet before it
 * (ServerGamePacketListenerImpl.handleClientTickEnd). 1.21.1 has no such packet, so this mod sends its own.
 *
 * <p>Only VanillaBackport's readers use the reset value (spear charge speed, the jab reach bonus and the modern leash
 * physics); the 1.21.1 field behind {@code getKnownMovement()} is left as it is for vanilla and other mods.
 */
public final class KnownMovement {
    private KnownMovement() {
    }

    public static final CustomPacketPayload.Type<ClientTickEnd> TICK_END_TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vbparity", "client_tick_end"));

    public record ClientTickEnd() implements CustomPacketPayload {
        public static final ClientTickEnd INSTANCE = new ClientTickEnd();
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientTickEnd> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TICK_END_TYPE;
        }
    }

    /** Implemented by ServerPlayer (ServerPlayerKnownSpeedMixin). */
    public interface Tracker {
        void vbp$clientTickEnd();

        boolean vbp$knownMovementIsReset();
    }

    /** Server side of the tick-end packet: the native handleClientTickEnd. */
    public static void onClientTickEnd(ServerPlayer player) {
        ((Tracker) player).vbp$clientTickEnd();
    }

    /** Whether {@link #of} can differ from {@code entity.getKnownMovement()}. */
    public static boolean involvesServerPlayer(Entity entity) {
        return entity instanceof ServerPlayer || entity.getControllingPassenger() instanceof ServerPlayer;
    }

    /**
     * {@code entity.getKnownMovement()} as the current game computes it: a server player's own movement reads zero
     * once a tick ended without a movement packet. The vehicle chain is the same as in 1.21.1 and native.
     */
    public static Vec3 of(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null && vehicle.getControllingPassenger() != player) {
                return of(vehicle);
            }
            return ((Tracker) player).vbp$knownMovementIsReset() ? Vec3.ZERO : player.getKnownMovement();
        }
        if (entity.getControllingPassenger() instanceof Player controller && entity.isAlive() && controller instanceof ServerPlayer) {
            return of(controller);
        }
        return entity.getKnownMovement();
    }

    @EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
    public static final class Network {
        private Network() {
        }

        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").optional().playToServer(TICK_END_TYPE, ClientTickEnd.STREAM_CODEC, (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    onClientTickEnd(player);
                }
            });
        }
    }
}
