package com.coolfreeze23.vbparity.copper;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Test-only listener: a fake player put into the server's player list for the duration of one synchronous call, so
 * the packets the server broadcasts near a position (level events, sounds) can be counted.
 */
final class PacketCapture implements AutoCloseable {
    final List<Packet<?>> packets = new ArrayList<>();
    private final ServerLevel level;
    private final ServerPlayer player;
    private final List<ServerPlayer> players;

    @SuppressWarnings("unchecked")
    PacketCapture(ServerLevel level, Vec3 at) {
        this.level = level;
        GameProfile profile = new GameProfile(UUID.randomUUID(), "vbp-copper-capture");
        this.player = new FakePlayer(level, profile);
        this.player.setPos(at);
        this.player.connection = new ServerGamePacketListenerImpl(
            level.getServer(), new Connection(PacketFlow.SERVERBOUND), this.player, CommonListenerCookie.createInitial(profile, false)
        ) {
            @Override
            public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
                PacketCapture.this.packets.add(packet);
            }
        };
        try {
            Field field = PlayerList.class.getDeclaredField("players");
            field.setAccessible(true);
            this.players = (List<ServerPlayer>) field.get(level.getServer().getPlayerList());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        this.players.add(this.player);
    }

    <T> List<T> of(Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Packet<?> packet : this.packets) {
            if (type.isInstance(packet)) {
                out.add(type.cast(packet));
            }
        }
        return out;
    }

    @Override
    public void close() {
        this.players.remove(this.player);
    }
}
