package com.coolfreeze23.vbparity.mayhem.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MeleeAttack.class)
public interface MeleeAttackAccessor {
    @Invoker("isHoldingUsableProjectileWeapon")
    static boolean vbp$isHoldingUsableProjectileWeapon(Mob mob) {
        throw new AssertionError();
    }
}
