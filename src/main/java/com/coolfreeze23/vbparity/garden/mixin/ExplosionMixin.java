package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AI-CRK-D1: native ServerExplosion applies knockback through Entity.push, which a frozen creaking ignores
 * (Creaking.push only moves while canMove). 1.21.1 sets the motion directly. Scoped to VB's creaking.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @WrapOperation(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void vbparity$garden$creakingPush(Entity entity, Vec3 motion, Operation<Void> original) {
        if (entity instanceof Creaking creaking) {
            Vec3 impulse = motion.subtract(creaking.getDeltaMovement());
            creaking.push(impulse.x, impulse.y, impulse.z);
        } else {
            original.call(entity, motion);
        }
    }
}
