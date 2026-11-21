package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.items.SulfurCubeBucketItem;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** SULFUR-17. Native BucketItem fires placed_block only when the bucket holds a fluid; the sulfur cube bucket holds none. */
@Mixin(SulfurCubeBucketItem.class)
public abstract class SulfurCubeBucketItemMixin {
    @Redirect(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/critereon/ItemUsedOnLocationTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void vbp$noPlacedBlockForMobBucket(ItemUsedOnLocationTrigger trigger, ServerPlayer player, BlockPos pos, ItemStack stack) {
    }
}
