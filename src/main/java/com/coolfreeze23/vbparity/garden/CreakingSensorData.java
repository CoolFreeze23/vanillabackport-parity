package com.coolfreeze23.vbparity.garden;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Stand-in for native's NEAREST_VISIBLE_ATTACKABLE_PLAYERS memory (1.21.1 has no plural memory type).
 * Implemented on VB's Creaking by a mixin and filled by the player sensor.
 */
public interface CreakingSensorData {
    @Nullable
    List<Player> vbparity$garden$getVisibleAttackablePlayers();

    void vbparity$garden$setVisibleAttackablePlayers(@Nullable List<Player> players);
}
