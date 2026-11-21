package com.coolfreeze23.vbparity.mayhem.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface MobAccessor {
    @Invoker("playAttackSound")
    void vbp$playAttackSound();
}
