package com.coolfreeze23.vbparity.flora.mixin;

import com.blackgear.vanillabackport.common.level.blocks.FireflyBushBlock;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FLORA-01: native fireflies spawn only at raw brightness 13 or lower; the night test belongs to the idle sound only. */
@Mixin(FireflyBushBlock.class)
public abstract class FireflyBushBlockMixin {
    @ModifyExpressionValue(
        method = "animateTick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/core/util/WorldUtilities$EnvironmentUtils;isNaturalNight(Lnet/minecraft/world/level/Level;)Z",
            ordinal = 1
        )
    )
    private boolean vbparity$particlesIgnoreNight(boolean night) {
        return false;
    }
}
