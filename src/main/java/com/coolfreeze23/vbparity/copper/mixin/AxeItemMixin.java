package com.coolfreeze23.vbparity.copper.mixin;

import com.coolfreeze23.vbparity.copper.CopperChestEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** TBLK-103: scraping or waxing off one half of a double chest also signals and shows particles at the other half. */
@Mixin(AxeItem.class)
public abstract class AxeItemMixin {
    @WrapOperation(
        method = "evaluateNewBlockState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V")
    )
    private void vbparity$copper$partnerHalf(
        Level level, Player player, int particle, BlockPos pos, int data, Operation<Void> original, @Local(argsOnly = true) BlockState oldState
    ) {
        original.call(level, player, particle, pos, data);
        CopperChestEvents.partnerChange(level, player, pos, oldState, particle);
    }
}
