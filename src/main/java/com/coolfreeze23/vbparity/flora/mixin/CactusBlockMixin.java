package com.coolfreeze23.vbparity.flora.mixin;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * FLORA-04: native CactusBlock.randomTick ages the top block up to 15 at any column height (a 3-tall top passes age 8 and
 * gets its 25% flower roll), and still ages the block in the tick a flower appears. 1.21.1 only ages columns under 3 tall,
 * and VB's flower roll (kept as is) leaves the age at 8. This runs around the whole method, VB's roll included.
 */
@Mixin(CactusBlock.class)
public abstract class CactusBlockMixin {
    @WrapMethod(method = "randomTick")
    private void vbparity$nativeAgeing(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        BlockPos above = pos.above();
        boolean aboveWasEmpty = level.isEmptyBlock(above);
        original.call(state, level, pos, random);
        if (!aboveWasEmpty) {
            return;
        }
        Block self = (Block) (Object) this;
        int age = state.getValue(CactusBlock.AGE);
        int height = 1;
        while (level.getBlockState(pos.below(height)).is(self)) {
            if (++height == 3 && age == 15) {
                return;
            }
        }
        BlockState now = level.getBlockState(pos);
        if (age >= 15 || !now.is(self) || now.getValue(CactusBlock.AGE) != age) {
            return; // 1.21.1 already aged or regrew it
        }
        if (height >= 3 || (age == 8 && level.getBlockState(above).is(ModBlocks.CACTUS_FLOWER.get()))) {
            level.setBlock(pos, now.setValue(CactusBlock.AGE, age + 1), Block.UPDATE_INVISIBLE);
        }
    }
}
