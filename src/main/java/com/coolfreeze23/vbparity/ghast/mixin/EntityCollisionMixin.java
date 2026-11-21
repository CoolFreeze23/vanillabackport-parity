package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-09: the current game's per-entity happy ghast collision. A ridden ghast is solid only to other happy
 * ghasts; a still ghast is solid to everything; client-side, a player at or above the top face lands on it.
 */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void vbp$happyGhastCollision(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof HappyGhast ghast) {
            Entity self = (Entity) (Object) this;
            boolean solid = false;
            if (!ghast.isBaby() && ghast.isAlive()) {
                if (ghast.level().isClientSide() && self instanceof Player && self.position().y >= ghast.getBoundingBox().maxY) {
                    solid = true;
                } else {
                    solid = ghast.isVehicle() && self instanceof HappyGhast || ghast.isOnStillTimeout();
                }
            }

            cir.setReturnValue(solid && !self.isPassengerOfSameVehicle(other));
        }
    }
}
