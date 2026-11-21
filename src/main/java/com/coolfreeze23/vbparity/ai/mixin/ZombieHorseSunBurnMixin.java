package com.coolfreeze23.vbparity.ai.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AI-MNT-06 (VB side): VB burns zombie horses after the whole horse aiStep. The burn now runs inside Mob.aiStep
 * ({@link MobSunBurnMixin}), where native Mob.burnUndead runs, so VB's late roll is switched off without drawing.
 * Applied after VB's mixin (priority) so its merged aiStep exists.
 */
@Mixin(value = ZombieHorse.class, priority = 1500)
public abstract class ZombieHorseSunBurnMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "isSunBurnTick()Z"))
    private boolean vbp$burnRolledInMobAiStep(ZombieHorse self, Operation<Boolean> original) {
        return false;
    }
}
