package com.coolfreeze23.vbparity.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/** TBLK-103: native AxeItem.spawnSoundAndParticle / HoneycombItem.useOn tail for the other half of a double chest. */
public final class CopperChestEvents {
    private CopperChestEvents() {
    }

    public static void partnerChange(Level level, @Nullable Player player, BlockPos pos, BlockState oldState, int particle) {
        if (oldState.getBlock() instanceof ChestBlock && oldState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos neighbour = pos.relative(ChestBlock.getConnectedDirection(oldState));
            level.gameEvent(GameEvent.BLOCK_CHANGE, neighbour, GameEvent.Context.of(player, level.getBlockState(neighbour)));
            level.levelEvent(player, particle, neighbour, 0);
        }
    }
}
