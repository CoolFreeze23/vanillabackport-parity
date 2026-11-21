package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.blocks.WeatheringLightningRodBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TBLK-101: VB makes every LightningRodBlock a WeatheringCopper of age UNAFFECTED, including its four waxed rods (plain
 * LightningRodBlocks). Natively only the weathering rods are ChangeOverTimeBlocks, so waxed rods are not neighbours in
 * the oxidation scan. Here a plain rod other than minecraft:lightning_rod (the unaffected weathering rod) reads as air.
 */
@Mixin(ChangeOverTimeBlock.class)
public interface ChangeOverTimeBlockMixin {
    @WrapOperation(
        method = "getNextState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
    )
    private static Block vbparity$copper$skipWaxedRods(BlockState state, Operation<Block> original) {
        Block block = original.call(state);
        return block instanceof LightningRodBlock && !(block instanceof WeatheringLightningRodBlock) && block != Blocks.LIGHTNING_ROD ? Blocks.AIR : block;
    }
}
