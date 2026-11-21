package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * MAYHEM-23: the camel husk saddle sound plays at volume 1.0 natively (the 0.5 the horse family uses lives in their
 * sounds.json entries, not in camel_husk's).
 * MAYHEM-24: zombie horses never panic natively (the panic goal moved to addBehaviourGoals, which ZombieHorse overrides).
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {
    @ModifyArg(
        method = "containerChanged",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", ordinal = 0),
        index = 1
    )
    private float vbp$camelHuskSaddleVolume(float volume) {
        return (Object) this instanceof CamelHusk ? 1.0F : volume;
    }

    @WrapWithCondition(
        method = "registerGoals",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V", ordinal = 0)
    )
    private boolean vbp$noZombieHorsePanic(GoalSelector selector, int priority, Goal goal) {
        return !((Object) this instanceof ZombieHorse);
    }
}
