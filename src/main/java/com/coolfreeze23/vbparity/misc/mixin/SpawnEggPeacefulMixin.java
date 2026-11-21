package com.coolfreeze23.vbparity.misc.mixin;

import com.coolfreeze23.vbparity.misc.MiscContent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Native SpawnEggItem.useOn / use: {@code if (type == null || !type.canSpawn(level)) return FAIL} on both sides, so a
 * monster egg in Peaceful does nothing (it does not set a spawner either). 1.21.1 would set the spawner, or silently
 * spawn nothing and still report CONSUME.
 */
@Mixin(SpawnEggItem.class)
public abstract class SpawnEggPeacefulMixin {
    @Shadow
    public abstract EntityType<?> getType(ItemStack stack);

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void vbp$failInPeaceful(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (vbp$refused(context.getLevel(), context.getItemInHand())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void vbp$failInPeaceful(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (vbp$refused(level, stack)) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private boolean vbp$refused(Level level, ItemStack stack) {
        return level.getDifficulty() == Difficulty.PEACEFUL && MiscContent.notAllowedInPeaceful(this.getType(stack));
    }
}
