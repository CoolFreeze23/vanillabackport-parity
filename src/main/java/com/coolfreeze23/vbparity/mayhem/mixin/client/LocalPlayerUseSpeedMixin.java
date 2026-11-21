package com.coolfreeze23.vbparity.mayhem.mixin.client;

import com.blackgear.vanillabackport.common.level.components.UseEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** MAYHEM-40: native item-use slowdown is the used item's use_effects.speed_multiplier (default 0.2). */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerUseSpeedMixin {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "CONSTANT", args = "floatValue=0.2F"))
    private float vbp$useSpeedMultiplier(float multiplier) {
        return UseEffects.get(((LocalPlayer) (Object) this).getUseItem()).speedMultiplier();
    }
}
