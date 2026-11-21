package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.integrations.interactions.GhastHarnessInteraction;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GhastHarnessInteraction.class)
public abstract class GhastHarnessInteractionMixin {
    /** GHAST-02: the ghast wears the player's actual harness (name, enchantments and other components kept). */
    @WrapOperation(method = "onInteract", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack vbp$equipTheHeldStack(ItemLike item, Operation<ItemStack> original,
                                            @Local(argsOnly = true) Player player, @Local(argsOnly = true) InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        return held.is(item.asItem()) ? held.copyWithCount(1) : original.call(item);
    }

    /** GHAST-03: equipping emits a single EQUIP game event from onEquipItem, so drop VB's extra one. */
    @Redirect(method = "onInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;)V"))
    private void vbp$noExtraEquipEvent(Level level, Entity entity, Holder<GameEvent> event, Vec3 pos) {
    }
}
