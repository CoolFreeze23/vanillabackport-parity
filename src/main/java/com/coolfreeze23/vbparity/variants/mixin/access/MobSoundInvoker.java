package com.coolfreeze23.vbparity.variants.mixin.access;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets GameTests read the ambient sound a mob would play. */
@Mixin(Mob.class)
public interface MobSoundInvoker {
    @Invoker("getAmbientSound")
    SoundEvent vbp$getAmbientSound();
}
