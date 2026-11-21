package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhastAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** GHAST-13: ghastlings stop 3 blocks from a tempting player and follow adults from 3 to 16 blocks. */
@Mixin(HappyGhastAi.class)
public abstract class HappyGhastAiMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/valueproviders/UniformInt;of(II)Lnet/minecraft/util/valueproviders/UniformInt;"), index = 0)
    private static int vbp$adultFollowMin(int min) {
        return 3;
    }

    @ModifyConstant(method = "lambda$initIdleActivity$1", constant = @Constant(doubleValue = 5.0))
    private static double vbp$closeEnoughDistance(double distance) {
        return 3.0;
    }
}
