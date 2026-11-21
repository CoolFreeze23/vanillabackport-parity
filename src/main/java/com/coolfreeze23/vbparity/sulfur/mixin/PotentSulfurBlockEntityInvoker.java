package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.block_entities.PotentSulfurBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PotentSulfurBlockEntity.class)
public interface PotentSulfurBlockEntityInvoker {
    @Invoker("findNoxiousGasSourceBlock")
    static BlockPos vbp$findNoxiousGasSourceBlock(Level level, BlockPos origin) {
        throw new AssertionError();
    }

    @Invoker("getUnobstructedBlockCount")
    static int vbp$getUnobstructedBlockCount(Level level, BlockPos pos, int waterBlocks) {
        throw new AssertionError();
    }
}
