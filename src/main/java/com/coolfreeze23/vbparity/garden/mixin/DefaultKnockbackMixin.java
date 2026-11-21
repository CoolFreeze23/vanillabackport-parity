package com.coolfreeze23.vbparity.garden.mixin;

import com.coolfreeze23.vbparity.garden.GardenKnockback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** TPHYS-C06: LivingEntity.hurt's default 0.4 knockback is one native blocks on a frozen creaking. */
@Mixin(LivingEntity.class)
public abstract class DefaultKnockbackMixin {
    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void vbparity$garden$markDefault(LivingEntity target, double power, double x, double z, Operation<Void> original) {
        GardenKnockback.enter();
        try {
            original.call(target, power, x, z);
        } finally {
            GardenKnockback.exit();
        }
    }
}
