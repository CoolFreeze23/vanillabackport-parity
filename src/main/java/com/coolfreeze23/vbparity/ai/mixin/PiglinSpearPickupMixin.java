package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.AiParity;
import java.util.function.Predicate;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-SPR-D2: native Piglin.canReplaceCurrentItem treats #piglin_preferred_weapons (crossbow, golden spear; adults only)
 * as wanted, then falls back to Mob.compareWeapons. 1.21.1 hard-codes the crossbow, so an adult never swapped between a
 * golden spear and a crossbow. Native rules whenever a spear is involved.
 */
@Mixin(Piglin.class)
public abstract class PiglinSpearPickupMixin {
    @Inject(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void vbp$nativePreferredWeapons(ItemStack candidate, ItemStack existing, CallbackInfoReturnable<Boolean> cir) {
        if (!AiParity.spearsOn() || !(AiParity.isSpear(candidate) || AiParity.isSpear(existing))) {
            return;
        }
        Piglin self = (Piglin) (Object) this;
        if (EnchantmentHelper.has(existing, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            cir.setReturnValue(false);
            return;
        }
        Predicate<ItemStack> preferred = AiParity.preferredWeapon(self);
        boolean newWanted = candidate.is(ItemTags.PIGLIN_LOVED) || preferred != null && preferred.test(candidate);
        boolean curWanted = existing.is(ItemTags.PIGLIN_LOVED) || preferred != null && preferred.test(existing);
        if (newWanted && !curWanted) {
            cir.setReturnValue(true);
        } else if (!newWanted && curWanted) {
            cir.setReturnValue(false);
        } else if (existing.isEmpty()) {
            cir.setReturnValue(true);
        } else if (self.getEquipmentSlotForItem(candidate) == EquipmentSlot.MAINHAND) {
            cir.setReturnValue(AiParity.compareWeapons(self, candidate, existing));
        } else {
            cir.setReturnValue(false);
        }
    }
}
