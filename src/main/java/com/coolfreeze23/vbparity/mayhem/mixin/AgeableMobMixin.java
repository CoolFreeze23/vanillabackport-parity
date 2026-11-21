package com.coolfreeze23.vbparity.mayhem.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** MAYHEM-25: native ZombieHorse.canAgeUp() is false, so baby zombie horses never grow up on their own. */
@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin {
    @WrapWithCondition(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;setAge(I)V", ordinal = 0)
    )
    private boolean vbp$zombieHorseStaysBaby(AgeableMob self, int age) {
        return !(self instanceof ZombieHorse);
    }
}
