package com.coolfreeze23.vbparity.garden.mixin;

import com.coolfreeze23.vbparity.garden.GardenKnockback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.RamTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** TPHYS-C06: a goat ram's knockback uses the 5-argument overload natively, so it is blocked on a frozen creaking. */
@Mixin(RamTarget.class)
public abstract class RamTargetKnockbackMixin {
    @WrapOperation(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/goat/Goat;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void vbparity$garden$markRam(LivingEntity target, double power, double x, double z, Operation<Void> original) {
        GardenKnockback.enter();
        try {
            original.call(target, power, x, z);
        } finally {
            GardenKnockback.exit();
        }
    }
}
