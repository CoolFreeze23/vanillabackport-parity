package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashPhysics;
import com.coolfreeze23.vbparity.mayhem.KnownMovement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The elastic leash pull compares the holder's and the leashed entity's known movement. Natively a player's known
 * movement is zero once a client tick ends without a movement packet (handleClientTickEnd), so a player who stops
 * stops pulling at once; 1.21.1 kept the last movement for up to 20 ticks.
 */
@Mixin(value = LeashPhysics.class, remap = false)
public abstract class LeashPhysicsMixin {
    @WrapOperation(method = {"getHolderMovement", "checkElasticInteractions"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getKnownMovement()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 vbp$nativeKnownMovement(Entity entity, Operation<Vec3> original) {
        return KnownMovement.involvesServerPlayer(entity) ? KnownMovement.of(entity) : original.call(entity);
    }
}
