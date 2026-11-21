package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** MAYHEM-22: native CamelHusk.playStepSound plays both step sounds at volume 0.4. */
@Mixin(value = CamelHusk.class, remap = false)
public abstract class CamelHuskMixin {
    @ModifyArg(
        method = "playStepSound",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/camel/CamelHusk;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"),
        index = 1
    )
    private float vbp$stepVolume(float volume) {
        return 0.4F;
    }
}
