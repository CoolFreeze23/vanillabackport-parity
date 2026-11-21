package com.coolfreeze23.vbparity.sulfur.mixin;

import com.coolfreeze23.vbparity.sulfur.StabKnockbackTarget;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TPHYS-C05, mob side. Native LivingEntity stab passes postEnchantmentDamage to causeExtraKnockback whatever
 * dealsDamage is; VB's merged vb$stabAttack only reaches the cube through hurt. Captured from VB's own modifyDamage
 * call, so no extra enchantment evaluation.
 */
@Mixin(value = LivingEntity.class, priority = 1100)
public abstract class StabKnockbackLivingEntityMixin {
    @ModifyExpressionValue(
        method = "vb$stabAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"
        ),
        remap = false
    )
    private float vbp$knockbackOnlyStab(
        float postEnchantmentDamage,
        @Local(argsOnly = true) Entity target,
        @Local(argsOnly = true, ordinal = 0) boolean dealsDamage,
        @Local(argsOnly = true, ordinal = 1) boolean dealsKnockback
    ) {
        if (dealsKnockback && !dealsDamage && target instanceof StabKnockbackTarget cube) {
            LivingEntity self = (LivingEntity) (Object) this;
            cube.vbparity$prepareKnockback(self.damageSources().mobAttack(self), postEnchantmentDamage);
        }
        return postEnchantmentDamage;
    }
}
