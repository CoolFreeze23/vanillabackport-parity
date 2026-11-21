package com.coolfreeze23.vbparity.garden;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Survival mock players that are in level.players(), so brain sensors see them. */
final class GardenTestPlayers {
    private GardenTestPlayers() {
    }

    static ServerPlayer survival(GameTestHelper helper, Vec3 relativePos, int timeoutTicks) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "garden-test"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relativePos);
        player.teleportTo(level, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Never leave a survival player behind when the test times out.
        helper.runAtTickTime(timeoutTicks - 2, () -> remove(player));
        return player;
    }

    static void lookAt(ServerPlayer player, Vec3 target) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
    }

    static void lookAwayFrom(ServerPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        player.lookAt(EntityAnchorArgument.Anchor.EYES, eye.add(eye.subtract(target)));
    }

    static void remove(ServerPlayer player) {
        if (!player.isRemoved()) {
            player.server.getPlayerList().remove(player);
        }
    }
}
