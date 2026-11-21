package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.components.UseEffects;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TPHYS-C04: natively the item-use vibrations go through ItemStack.causeUseVibration, which skips them when the
 * item's use_effects has interact_vibrations = false (spears). Items without VB's component keep the 1.21.1 events.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityUseVibrationMixin {
    @Shadow
    protected ItemStack useItem;

    @WrapWithCondition(method = {"startUsingItem", "stopUsingItem"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;gameEvent(Lnet/minecraft/core/Holder;)V"))
    private boolean vbp$useVibration(LivingEntity self, Holder<GameEvent> event) {
        UseEffects effects = UseEffects.get(this.useItem);
        return effects == null || effects.interactVibrations();
    }
}
