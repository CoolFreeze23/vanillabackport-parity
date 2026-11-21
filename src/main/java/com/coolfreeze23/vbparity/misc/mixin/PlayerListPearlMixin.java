package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.api.modules.ender_pearl_persistance.EnderPearlLoaderModule;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.misc.PearlLoginContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * VB loads a joining player's saved ender pearls by calling {@code PlayerList.load(player)} a second time from
 * {@code placeNewPlayer}, which runs {@code player.load(tag)} and NeoForge's player-loading event twice, and spawns the
 * pearls before the player is in the level. Native ({@code PrepareSpawnTask.spawn}) reads the player data once and
 * spawns the pearls after the player has been placed, before the player's vehicle. Here VB's second read returns
 * nothing (so it neither reloads the player nor spawns early), and the pearls from the first read are spawned at
 * the native point.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListPearlMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Unique
    private static final ThreadLocal<PearlLoginContext> VBP_PLACING = new ThreadLocal<>();

    @WrapMethod(method = "placeNewPlayer")
    private void vbp$trackPlacing(Connection connection, ServerPlayer player, CommonListenerCookie cookie, Operation<Void> original) {
        PearlLoginContext previous = VBP_PLACING.get();
        VBP_PLACING.set(new PearlLoginContext(player));
        try {
            original.call(connection, player, cookie);
        } finally {
            VBP_PLACING.set(previous);
        }
    }

    @WrapMethod(method = "load")
    private Optional<CompoundTag> vbp$loadOnce(ServerPlayer player, Operation<Optional<CompoundTag>> original) {
        PearlLoginContext placing = VBP_PLACING.get();
        if (placing != null && placing.player == player) {
            if (placing.tag == null) {
                placing.tag = original.call(player);
                return placing.tag;
            }
            if (vbp$calledByVbPearlLoader()) {
                placing.spawnPearls = true;
                return Optional.empty();
            }
        }
        return original.call(player);
    }

    @Unique
    private static boolean vbp$calledByVbPearlLoader() {
        return StackWalker.getInstance().walk(frames -> frames.limit(8)
                .anyMatch(frame -> frame.getMethodName().contains("loadPlayerEnderPearlsOnLogin")));
    }

    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/players/PlayerList;sendActivePlayerEffects(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void vbp$spawnPearlsAfterPlacing(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        PearlLoginContext placing = VBP_PLACING.get();
        if (placing == null || placing.player != player || !placing.spawnPearls || placing.tag == null || placing.tag.isEmpty()) {
            return;
        }
        CompoundTag data = placing.tag.get();
        if (!data.contains("ender_pearls", Tag.TAG_LIST)) {
            return;
        }
        ListTag pearls = data.getList("ender_pearls", Tag.TAG_COMPOUND);
        for (int i = 0; i < pearls.size(); i++) {
            CompoundTag pearlTag = pearls.getCompound(i);
            Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, pearlTag.get("ender_pearl_dimension")).result().ifPresent(key -> {
                ServerLevel pearlLevel = this.server.getLevel(key);
                if (pearlLevel != null) {
                    Entity pearl = EntityType.loadEntityRecursive(pearlTag, pearlLevel, entity -> !pearlLevel.addWithUUID(entity) ? null : entity);
                    if (pearl != null) {
                        EnderPearlLoaderModule.placeEnderPearlTicket(pearlLevel, pearl.chunkPosition());
                    } else {
                        VBParity.LOG.warn("Failed to spawn player ender pearl in level ({}), skipping", key);
                    }
                } else {
                    VBParity.LOG.warn("Trying to load ender pearl without level ({}) being loaded, skipping", key);
                }
            });
        }
    }
}
