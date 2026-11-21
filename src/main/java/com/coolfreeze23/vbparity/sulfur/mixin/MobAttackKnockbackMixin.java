package com.coolfreeze23.vbparity.sulfur.mixin;

import com.coolfreeze23.vbparity.sulfur.KnockbackEffect;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** AI-SLF-01, mob melee: native Mob.doHurtTarget pushes with causeExtraKnockback(..., true). */
@Mixin(Mob.class)
public abstract class MobAttackKnockbackMixin {
    @WrapOperation(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void vbp$attackKnockbackIsExtra(LivingEntity target, double power, double x, double z, Operation<Void> original) {
        KnockbackEffect.run(true, () -> original.call(target, power, x, z));
    }
}
