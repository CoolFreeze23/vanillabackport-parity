package com.coolfreeze23.vbparity.garden;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.core.data.tags.ModBlockTags;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Native CreakingHeartBlockEntity.spreadResin: a breadth-first walk over pale oak logs (depth 2, 64 nodes) that STOPS
 * as soon as one resin face is placed. 1.21.1's traversal predicate can only skip a node, which made VB place a face on
 * every log in range. Random draws happen in native order: node processor shuffle first, then neighbour shuffle.
 */
public final class GardenResin {
    private record Node(BlockPos pos, int depth) {
    }

    private GardenResin() {
    }

    public static Optional<BlockPos> spread(ServerLevel level, BlockPos start) {
        RandomSource random = level.getRandom();
        Queue<Node> nodes = new ArrayDeque<>();
        LongSet visited = new LongOpenHashSet();
        nodes.add(new Node(start, 0));
        int count = 0;

        while (!nodes.isEmpty()) {
            Node node = nodes.poll();
            BlockPos pos = node.pos();
            if (!visited.add(pos.asLong())) {
                continue;
            }

            if (level.getBlockState(pos).is(ModBlockTags.CREAKING_HEART_HOLDERS)) {
                for (Direction dir : Util.shuffledCopy(Direction.values(), random)) {
                    BlockPos neighbourPos = pos.relative(dir);
                    BlockState neighbourState = level.getBlockState(neighbourPos);
                    Direction opposite = dir.getOpposite();
                    if (neighbourState.isAir()) {
                        neighbourState = ModBlocks.RESIN_CLUMP.get().defaultBlockState();
                    } else if (neighbourState.is(Blocks.WATER) && neighbourState.getFluidState().isSource()) {
                        neighbourState = ModBlocks.RESIN_CLUMP.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
                    }

                    if (neighbourState.is(ModBlocks.RESIN_CLUMP.get()) && !MultifaceBlock.hasFace(neighbourState, opposite)) {
                        level.setBlock(neighbourPos, neighbourState.setValue(MultifaceBlock.getFaceProperty(opposite), true), 3);
                        return Optional.of(neighbourPos);
                    }
                }
            }

            if (++count >= 64) {
                break;
            }

            if (node.depth() < 2) {
                for (Direction dir : Util.shuffledCopy(Direction.values(), random)) {
                    BlockPos neighbourPos = pos.relative(dir);
                    if (level.getBlockState(neighbourPos).is(ModBlockTags.CREAKING_HEART_HOLDERS)) {
                        nodes.add(new Node(neighbourPos, node.depth() + 1));
                    }
                }
            }
        }

        return Optional.empty();
    }
}
