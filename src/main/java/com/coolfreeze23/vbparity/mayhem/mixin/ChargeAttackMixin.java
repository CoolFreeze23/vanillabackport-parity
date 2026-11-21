package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.entities.ai.behavior.ChargeAttack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MAYHEM-12: native ChargeAttack takes (maxChargeDistance, maxTargetDetectionDistance) and both nautilus AIs pass
 * (12.0, 11.0); VB's constructor has the two the other way round. The fields are swapped back after construction.
 * MAYHEM-13: native knockback goes through causeExtraKnockback, which keeps the charger's vertical speed (0.6, 1.0, 0.6).
 */
@Mixin(value = ChargeAttack.class, remap = false)
public abstract class ChargeAttackMixin {
    @Shadow @Final @Mutable private double maxTargetDetectionDistance;
    @Shadow @Final @Mutable private double maxChargeDistance;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void vbp$swapDistances(CallbackInfo ci) {
        double detection = this.maxTargetDetectionDistance;
        this.maxTargetDetectionDistance = this.maxChargeDistance;
        this.maxChargeDistance = detection;
    }

    @ModifyArg(
        method = "dealKnockback",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"),
        index = 1
    )
    private double vbp$keepVerticalSpeed(double y) {
        return 1.0;
    }
}
