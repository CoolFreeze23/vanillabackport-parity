package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.core.util.WorldUtilities;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-17: the current overworld cloud layer starts at 192.33 (a float) and is 4 blocks thick. Dimensions without
 * clouds (VB's non-natural check, matching cloud alpha 0 in the vanilla dimensions) never count.
 */
@Mixin(WorldUtilities.EnvironmentUtils.class)
public abstract class EnvironmentUtilsMixin {
    @Inject(method = "isInClouds", at = @At("HEAD"), cancellable = true)
    private static void vbp$cloudBand(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!entity.level().dimensionType().natural()) {
            cir.setReturnValue(false);
            return;
        }

        float cloudBottom = 192.33F;
        if (entity.getY() + entity.getBbHeight() < cloudBottom) {
            cir.setReturnValue(false);
        } else {
            float cloudTop = cloudBottom + 4.0F;
            cir.setReturnValue(entity.getY() <= cloudTop);
        }
    }
}
