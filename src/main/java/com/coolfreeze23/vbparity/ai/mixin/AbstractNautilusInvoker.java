package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractNautilus.class, remap = false)
public interface AbstractNautilusInvoker {
    @Invoker("playEatingSound")
    void vbp$playEatingSound();
}
