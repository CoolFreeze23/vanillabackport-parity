package com.coolfreeze23.vbparity.ai;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class AiTestUtil {
    private AiTestUtil() {
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    static void near(double expected, double actual, double tolerance, String what) {
        check(Math.abs(expected - actual) <= tolerance, what + ": expected " + expected + " but was " + actual);
    }

    static <T extends Mob> T still(GameTestHelper helper, EntityType<T> type, Vec3 pos) {
        T mob = helper.spawn(type, pos);
        mob.setNoAi(true);
        return mob;
    }

    static void water(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    /** A survival player that is in level.players(), so brain sensors see it; removed before the timeout. */
    static ServerPlayer survival(GameTestHelper helper, Vec3 relativePos, int timeoutTicks) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "ai-test"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relativePos);
        player.teleportTo(level, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runAtTickTime(timeoutTicks - 2, () -> remove(player));
        return player;
    }

    static void remove(ServerPlayer player) {
        if (!player.isRemoved()) {
            player.server.getPlayerList().remove(player);
        }
    }

    static Field field(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection: " + e);
        }
    }

    static Object get(Object target, Class<?> owner, String name) {
        try {
            return field(owner, name).get(target);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection: " + e);
        }
    }

    static void set(Object target, Class<?> owner, String name, Object value) {
        try {
            field(owner, name).set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection: " + e);
        }
    }

    static Object invoke(Object target, Class<?> owner, String name, Class<?>[] types, Object... args) {
        try {
            Method m = owner.getDeclaredMethod(name, types);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new GameTestAssertException("invoke " + name + ": " + e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection: " + e);
        }
    }

    /** Runs the action with the level's random swapped for {@code random} (single-threaded, restored after). */
    static void withLevelRandom(Level level, RandomSource random, Runnable action) {
        Object old = get(level, Level.class, "random");
        set(level, Level.class, "random", random);
        try {
            action.run();
        } finally {
            set(level, Level.class, "random", old);
        }
    }

    static void setEntityRandom(Entity entity, RandomSource random) {
        set(entity, Entity.class, "random", random);
    }
}
