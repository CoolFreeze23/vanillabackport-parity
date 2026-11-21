package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import com.coolfreeze23.vbparity.ai.AiParity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AI-GHS-D1: the happy ghast and the (zombie) nautilus read the rider's strafe from Player.xxa; natively that is
 * 0.70710677 when a forward/back key is held too (square-fitted keyboard input), 0.98 in 1.21.1. Their forward term
 * only uses the sign of zza, so only the strafe changes.
 */
@Mixin(value = {HappyGhast.class, AbstractNautilus.class}, remap = false)
public abstract class RiddenInputSquareMixin {
    @ModifyExpressionValue(method = "getRiddenInput", remap = true,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;xxa:F", ordinal = 0))
    private float vbp$squareStrafe(float strafe, @Local(argsOnly = true) Player player) {
        return AiParity.squareInput(strafe, player);
    }
}
