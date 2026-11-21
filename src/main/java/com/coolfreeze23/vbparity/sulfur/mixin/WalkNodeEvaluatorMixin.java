package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.core.data.tags.ModBlockTags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TPHYS-B02. Native WalkNodeEvaluator.getPathTypeFromState treats every #speleothems block (pointed dripstone and
 * sulfur spikes) like pointed dripstone: DAMAGE_CAUTIOUS, which also marks the node on top and the 26 neighbours.
 * 1.21.1 checks only Blocks.POINTED_DRIPSTONE, so a sulfur spike falls through to BLOCKED.
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
    @WrapOperation(
        method = "getPathTypeFromState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private static boolean vbp$speleothemsAreCautious(BlockState state, Block block, Operation<Boolean> original) {
        boolean is = original.call(state, block);
        return block == Blocks.POINTED_DRIPSTONE ? is || state.is(ModBlockTags.SPELEOTHEMS) : is;
    }
}
