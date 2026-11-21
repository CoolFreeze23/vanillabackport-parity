package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GARDEN-06: native boats skip #cannot_be_pushed_onto_boats (which lists minecraft:creaking) and push them instead.
 * Covers vanilla boats, chest boats and VB's CustomBoat / CustomChestBoat (all extend Boat).
 */
@Mixin(Boat.class)
public abstract class BoatMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean vbparity$garden$noCreakingPassengers(Entity entity, Entity vehicle, Operation<Boolean> original) {
        if (entity instanceof Creaking) {
            ((Boat) (Object) this).push(entity);
            return false;
        }
        return original.call(entity, vehicle);
    }
}
