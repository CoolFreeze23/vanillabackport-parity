package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.skeleton.Parched;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MAYHEM-30: native parched are immune to Weakness (VB's condition was inverted). */
@Mixin(Parched.class)
public abstract class ParchedMixin {
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void vbp$weaknessImmune(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect.getEffect() == MobEffects.WEAKNESS) {
            cir.setReturnValue(false);
        }
    }
}
