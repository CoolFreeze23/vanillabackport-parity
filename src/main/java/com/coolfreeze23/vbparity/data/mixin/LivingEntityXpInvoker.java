package com.coolfreeze23.vbparity.data.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityXpInvoker {
    @Invoker("getBaseExperienceReward")
    int vbparity$data$baseExperienceReward();
}
