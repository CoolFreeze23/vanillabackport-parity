package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.coolfreeze23.vbparity.ai.AiParity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** AI-GHS-D1 for the camel husk (AbstractHorse.getRiddenInput): both rider axes are square-fitted as natively. */
@Mixin(AbstractHorse.class)
public abstract class CamelHuskRiddenInputMixin {
    @ModifyExpressionValue(method = "getRiddenInput",
        at = {
            @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;xxa:F"),
            @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;zza:F")
        })
    private float vbp$squareInput(float value, @Local(argsOnly = true) Player player) {
        return (Object) this instanceof CamelHusk ? AiParity.squareInput(value, player) : value;
    }
}
