package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.AiParity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-SPR-D1: native Mob.canReplaceCurrentItem compares main-hand weapons by preferred weapon type, then attack damage,
 * then canReplaceEqualItem. 1.21.1 decides by item class (SwordItem / DiggerItem), where a spear is neither, so a spear
 * never replaced anything and any sword replaced a spear. Native rules for every main-hand swap that involves a spear.
 */
@Mixin(Mob.class)
public abstract class SpearPickupMixin {
    @Inject(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void vbp$nativeWeaponCompare(ItemStack candidate, ItemStack existing, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (existing.isEmpty() || !AiParity.spearsOn() || !(AiParity.isSpear(candidate) || AiParity.isSpear(existing))) {
            return;
        }
        if (self.getEquipmentSlotForItem(candidate) != EquipmentSlot.MAINHAND) {
            return;
        }
        cir.setReturnValue(AiParity.compareWeapons(self, candidate, existing));
    }
}
