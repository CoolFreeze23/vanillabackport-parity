package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.level.blocks.StrawBedBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * WILD-21. Native Level.destroyBlock calls Block.spawnDestroyParticles, which StrawBedBlock overrides with level event
 * 2014 (destroy particles, no break sound). 1.21.1 always sends 2001 (particles + break sound). The client side of 2014
 * is LevelRendererMixin.
 */
@Mixin(Level.class)
public abstract class LevelMixin {
    @WrapOperation(
        method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V")
    )
    private void vbparity$wild$strawBedSilentDestroy(Level level, int type, BlockPos pos, int data, Operation<Void> original) {
        if (type == 2001 && Block.stateById(data).getBlock() instanceof StrawBedBlock) {
            type = 2014;
        }
        original.call(level, type, pos, data);
    }
}
