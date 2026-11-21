package com.coolfreeze23.vbparity.variants.mixin.access;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets GameTests read the hurt and death sounds an entity would play. */
@Mixin(LivingEntity.class)
public interface LivingEntitySoundInvoker {
    @Invoker("getHurtSound")
    SoundEvent vbp$getHurtSound(DamageSource source);

    @Invoker("getDeathSound")
    SoundEvent vbp$getDeathSound();
}
