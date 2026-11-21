package com.coolfreeze23.vbparity.data.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Native Animal XP rolls {@code 1 + this.random.nextInt(3)} (the mob's own RNG); 1.21.1 rolls the level RNG.
 * Only VanillaBackport's animals take the native source; vanilla 1.21.1 animals keep theirs.
 */
@Mixin(Animal.class)
public abstract class AnimalXpMixin {
    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void vbparity$data$nativeXpRandom(CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (self instanceof HappyGhast || self instanceof AbstractNautilus || self instanceof CamelHusk) {
            cir.setReturnValue(1 + ((Entity) self).getRandom().nextInt(3));
        }
    }
}
