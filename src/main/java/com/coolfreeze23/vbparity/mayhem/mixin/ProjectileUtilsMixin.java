package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.core.util.ProjectileUtils;
import com.coolfreeze23.vbparity.mayhem.KnownMovement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** MAYHEM-32: the jab reach bonus reads the native (tick-end reset) known movement, as ProjectileUtil does natively. */
@Mixin(value = ProjectileUtils.class, remap = false)
public abstract class ProjectileUtilsMixin {
    @WrapOperation(method = "getHitEntitiesAlong", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getKnownMovement()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 vbp$nativeKnownMovement(Entity attacker, Operation<Vec3> original) {
        return KnownMovement.involvesServerPlayer(attacker) ? KnownMovement.of(attacker) : original.call(attacker);
    }
}
