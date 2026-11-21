package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.AiParity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AI-MNT-D3: native sensors set their targeting conditions' range to the body's FOLLOW_RANGE (16 plus the random
 * spawn bonus, up to 17.84) before each scan; 1.21.1 fixes them at 16. Scoped to the happy ghast, (zombie) nautilus
 * and camel husk.
 */
@Mixin(Sensor.class)
public abstract class SensorFollowRangeMixin {
    @WrapOperation(method = {"isEntityTargetable", "isEntityAttackable", "isEntityAttackableIgnoringLineOfSight"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;test(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private static boolean vbp$followRange(TargetingConditions conditions, LivingEntity body, LivingEntity target, Operation<Boolean> original) {
        if (AiParity.followRangeSensors(body)) {
            double range = AiParity.followRange(body);
            if (range != 16.0) {
                return original.call(conditions.copy().range(range), body, target);
            }
        }
        return original.call(conditions, body, target);
    }
}
