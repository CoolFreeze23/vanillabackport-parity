package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilusAi;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** MAYHEM-14: zombie nautilus charge speed 0.5 (SPEED_WHEN_ATTACKING); MAYHEM-15: tempted speed 0.9. */
@Mixin(value = ZombieNautilusAi.class, remap = false)
public abstract class ZombieNautilusAiMixin {
    @ModifyArg(
        method = "initFightActivity",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/ai/behavior/ChargeAttack;<init>(ILnet/minecraft/world/entity/ai/targeting/TargetingConditions;FFDDLnet/minecraft/sounds/SoundEvent;)V"),
        index = 2
    )
    private static float vbp$chargeSpeed(float speed) {
        return 0.5F;
    }

    @ModifyArg(
        method = "initIdleActivity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/FollowTemptation;<init>(Ljava/util/function/Function;Ljava/util/function/Function;)V"),
        index = 0
    )
    private static Function<LivingEntity, Float> vbp$temptSpeed(Function<LivingEntity, Float> speed) {
        return mob -> 0.9F;
    }
}
