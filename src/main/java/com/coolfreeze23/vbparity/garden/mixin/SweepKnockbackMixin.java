package com.coolfreeze23.vbparity.garden.mixin;

import com.coolfreeze23.vbparity.garden.GardenKnockback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** TPHYS-C06: the sweep attack's 0.4 knockback (second knockback call in Player.attack) is blocked natively. */
@Mixin(Player.class)
public abstract class SweepKnockbackMixin {
    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V", ordinal = 1))
    private void vbparity$garden$markSweep(LivingEntity target, double power, double x, double z, Operation<Void> original) {
        GardenKnockback.enter();
        try {
            original.call(target, power, x, z);
        } finally {
            GardenKnockback.exit();
        }
    }
}
