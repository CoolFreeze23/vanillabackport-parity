package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.entities.ai.goal.OfferCopperGolemFlowerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * COPPER-22: native has a single flower goal. The vanilla OfferFlowerGoal now does the native work for villagers and
 * copper golems (OfferFlowerGoalMixin), so VB's second goal, which VB adds to every iron golem, never starts.
 */
@Mixin(OfferCopperGolemFlowerGoal.class)
public abstract class OfferCopperGolemFlowerGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void vbparity$copper$singleGoal(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
