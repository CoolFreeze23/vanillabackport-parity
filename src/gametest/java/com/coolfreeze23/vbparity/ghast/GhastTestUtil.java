package com.coolfreeze23.vbparity.ghast;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.phys.Vec3;

final class GhastTestUtil {
    private GhastTestUtil() {
    }

    /** A server player in the level whose creative and spectator state the test controls. */
    static final class TestPlayer extends ServerPlayer {
        boolean creative;
        boolean spectator;

        TestPlayer(GameTestHelper helper, CommonListenerCookie cookie) {
            super(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        }

        @Override
        public boolean isSpectator() {
            return this.spectator;
        }

        @Override
        public boolean isCreative() {
            return this.creative;
        }
    }

    static TestPlayer player(GameTestHelper helper, Vec3 relative, boolean creative) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "vbp-ghast-test"), false);
        TestPlayer player = new TestPlayer(helper, cookie);
        player.creative = creative;
        player.getAbilities().instabuild = creative;
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        Vec3 at = helper.absoluteVec(relative);
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = creative;
        return player;
    }

    static void remove(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    static HappyGhast ghast(GameTestHelper helper, Vec3 relative, boolean baby) {
        HappyGhast ghast = helper.spawn(ModEntityTypes.HAPPY_GHAST.get(), relative);
        if (baby) {
            ghast.setBaby(true);
        }

        return ghast;
    }

    static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-5;
    }
}
