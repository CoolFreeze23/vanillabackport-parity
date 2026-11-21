package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.AiParity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Dynamic;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AI-GHS-D5 / AI-MNT-D4 / AI-CRK-D4 / AI-CPG-D1 / AI-VAR-D4 (happy ghast): native Brain draws each sensor's start
 * delay as body.getRandom().nextInt(scanRate), in sensor order, whenever a brain is built (construction and load).
 * 1.21.1 draws it from a static random. For the mobs VB adds the delays now come from the mob's own random.
 */
@Mixin(LivingEntity.class)
public abstract class SensorStartDelayMixin {
    @WrapOperation(method = {"<init>", "readAdditionalSaveData"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;makeBrain(Lcom/mojang/serialization/Dynamic;)Lnet/minecraft/world/entity/ai/Brain;"))
    private Brain<?> vbp$entityRandomDelay(LivingEntity self, Dynamic<?> dynamic, Operation<Brain<?>> original) {
        Brain<?> brain = original.call(self, dynamic);
        if (brain != null && AiParity.isVbAdded(self)) {
            for (Sensor<?> sensor : ((BrainAccessor) brain).vbp$sensors().values()) {
                SensorAccessor access = (SensorAccessor) sensor;
                access.vbp$setTimeToTick(self.getRandom().nextInt(access.vbp$scanRate()));
            }
        }
        return brain;
    }
}
