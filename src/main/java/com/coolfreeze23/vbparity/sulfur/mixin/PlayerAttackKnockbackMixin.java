package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.coolfreeze23.vbparity.sulfur.KnockbackEffect;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AI-SLF-01, player melee. Native Player.attack: the attack's extra knockback is causeExtraKnockback(..., true); a sweep
 * target is hurt first and then pushed with knockback(0.4, ..., source, damage) (not from an effect). 1.21.1 pushes
 * sweep targets before hurting them, so a block-holding sulfur cube had no pending source yet (plain vanilla push).
 * The sweep order is changed for sulfur cubes only.
 */
@Mixin(Player.class)
public abstract class PlayerAttackKnockbackMixin {
    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V", ordinal = 0))
    private void vbp$attackKnockbackIsExtra(LivingEntity target, double power, double x, double z, Operation<Void> original) {
        KnockbackEffect.run(true, () -> original.call(target, power, x, z));
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V", ordinal = 1))
    private void vbp$sweepPushAfterHurt(LivingEntity target, double power, double x, double z, Operation<Void> original) {
        if (!(target instanceof SulfurCube) || target.level().isClientSide()) {
            original.call(target, power, x, z);
        }
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", ordinal = 0))
    private boolean vbp$sweepHurtThenPush(LivingEntity target, DamageSource source, float damage, Operation<Boolean> original) {
        boolean hurt = original.call(target, source, damage);
        if (hurt && target instanceof SulfurCube && !target.level().isClientSide()) {
            Player self = (Player) (Object) this;
            float yaw = self.getYRot() * (float) (Math.PI / 180.0);
            KnockbackEffect.run(false, () -> target.knockback(0.4F, Mth.sin(yaw), -Mth.cos(yaw)));
        }
        return hurt;
    }
}
