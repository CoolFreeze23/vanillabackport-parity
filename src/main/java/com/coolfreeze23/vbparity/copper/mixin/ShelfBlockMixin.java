package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.block_entities.ShelfBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.ShelfBlock;
import com.blackgear.vanillabackport.common.level.components.UseEffects;
import com.blackgear.vanillabackport.common.registries.items.ModDataComponents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TBLK-104: native ShelfBlock.swapSingleItem sends no game event when the item going into the player's hand has
 * use_effects with interact_vibrations false (every spear).
 */
@Mixin(ShelfBlock.class)
public abstract class ShelfBlockMixin {
    @WrapOperation(
        method = "swapSingleItem",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/block_entities/ShelfBlockEntity;setChanged(Lnet/minecraft/core/Holder$Reference;)V"
        )
    )
    private void vbparity$copper$quietSwap(ShelfBlockEntity shelf, Holder.Reference<GameEvent> event, Operation<Void> original, @Local(argsOnly = true) Inventory inventory) {
        ItemStack newInventoryItem = inventory.getItem(inventory.selected);
        UseEffects effects = newInventoryItem.get(ModDataComponents.USE_EFFECTS.get());
        original.call(shelf, effects != null && !effects.interactVibrations() ? null : event);
    }
}
