package com.coolfreeze23.vbparity.misc.mixin;

import com.coolfreeze23.vbparity.misc.client.MiscClientHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native TrackedWaypoint.Vec3iWaypoint.position uses {@code entity.getEyePosition(partialTick)} so nearby
 * locator dots move smoothly. The waypoint math only runs on the client (locator bar), but the class is shared.
 */
@Mixin(targets = "com.blackgear.vanillabackport.common.api.modules.waypoints.TrackedWaypoint$Vec3iWaypoint")
public abstract class TrackedWaypointVec3iMixin {
    @WrapOperation(method = "lambda$position$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 vbp$interpolatedEyePosition(Entity entity, Operation<Vec3> original) {
        if (entity.level().isClientSide()) {
            return entity.getEyePosition(MiscClientHooks.entityPartialTick(entity));
        }
        return original.call(entity);
    }
}
