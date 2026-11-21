package com.coolfreeze23.vbparity.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** AI-CPG-01/02: whether a chest is half of a real double chest (native DoubleBlockCombiner.combineWithNeigbour rules). */
public final class CopperChests {
    private CopperChests() {
    }

    public static boolean isCombinedDouble(BlockEntity blockEntity, Level level) {
        if (level == null) {
            return false;
        }
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return false;
        }
        BlockPos neighbourPos = blockEntity.getBlockPos().relative(ChestBlock.getConnectedDirection(state));
        BlockState neighbour = level.getBlockState(neighbourPos);
        return neighbour.is(state.getBlock())
            && neighbour.getValue(ChestBlock.TYPE) != ChestType.SINGLE
            && neighbour.getValue(ChestBlock.TYPE) != state.getValue(ChestBlock.TYPE)
            && neighbour.getValue(ChestBlock.FACING) == state.getValue(ChestBlock.FACING);
    }
}
