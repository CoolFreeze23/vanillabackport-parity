package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.coolfreeze23.vbparity.garden.GardenKnockback;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Creaking.class)
public abstract class CreakingPhysicsMixin {
    /** TPHYS-C06: only the default, sweep and ram knockback is ignored while the creaking cannot move. */
    @ModifyExpressionValue(method = "knockback", at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/creaking/Creaking;canMove()Z"))
    private boolean vbparity$garden$extraKnockbackMoves(boolean canMove) {
        return canMove || !GardenKnockback.isDefaultKnockback();
    }

    /** TPHYS-C09: native .add(MOVEMENT_SPEED, 0.4F), a float widened to 0.4000000059604645. */
    @ModifyConstant(method = "createAttributes", constant = @Constant(doubleValue = 0.4))
    private static double vbparity$garden$floatSpeed(double original) {
        return (double) 0.4F;
    }
}
