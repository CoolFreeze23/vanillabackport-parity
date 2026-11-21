package com.coolfreeze23.vbparity.ghast.mixin;

import com.coolfreeze23.vbparity.ghast.LeashRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-34: the current game's FenceBlock has no client-side lead shortcut (1.21.1 returned SUCCESS for a lead on the
 * client, swinging the arm even when nothing gets tied). The click now goes to useWithoutItem, which passes on the
 * client, and the server decides.
 */
@Mixin(FenceBlock.class)
public abstract class FenceBlockLeadMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void vbp$noClientLeadShortcut(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide() && stack.is(Items.LEAD) && LeashRules.modernLeashes()) {
            cir.setReturnValue(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
        }
    }
}
