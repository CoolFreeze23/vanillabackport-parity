package com.coolfreeze23.vbparity.misc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** The player being placed by PlayerList.placeNewPlayer, its single data read, and whether VB asked for its pearls. */
public final class PearlLoginContext {
    public final ServerPlayer player;
    public Optional<CompoundTag> tag;
    public boolean spawnPearls;

    public PearlLoginContext(ServerPlayer player) {
        this.player = player;
    }
}
