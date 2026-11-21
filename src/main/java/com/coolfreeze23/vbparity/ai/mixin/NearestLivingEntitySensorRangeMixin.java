package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.AiParity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** AI-MNT-D3: native NearestLivingEntitySensor scans FOLLOW_RANGE in every direction (1.21.1: 16). Scoped. */
@Mixin(NearestLivingEntitySensor.class)
public abstract class NearestLivingEntitySensorRangeMixin {
    @WrapOperation(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB vbp$followRange(AABB box, double x, double y, double z, Operation<AABB> original,
                                 @Local(argsOnly = true) LivingEntity body) {
        if (AiParity.followRangeSensors(body)) {
            double range = AiParity.followRange(body);
            return original.call(box, range, range, range);
        }
        return original.call(box, x, y, z);
    }
}
