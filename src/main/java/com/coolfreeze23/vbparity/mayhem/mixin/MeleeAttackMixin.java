package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.components.KineticWeapon;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TPHYS-CE01: natively MeleeAttack skips mobs holding a usable non-melee weapon, and Piglin.canUseNonMeleeWeapon
 * counts kinetic weapons, so a spear piglin only stabs. Scoped to piglins with a kinetic weapon.
 */
@Mixin(MeleeAttack.class)
public abstract class MeleeAttackMixin {
    @ModifyReturnValue(method = "isHoldingUsableProjectileWeapon", at = @At("RETURN"))
    private static boolean vbp$piglinSpearIsNotMelee(boolean original, Mob mob) {
        return original || mob instanceof Piglin && mob.isHolding(stack -> KineticWeapon.get(stack) != null);
    }
}
