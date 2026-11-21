package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.coolfreeze23.vbparity.ghast.EyeEntityTracker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** GHAST-14: a tempted ghastling flies toward the player's eyes (the current game's lookInTheEyes = true). */
@Mixin(FollowTemptation.class)
public abstract class FollowTemptationMixin {
    @WrapOperation(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
        at = @At(value = "NEW", target = "(Lnet/minecraft/world/entity/Entity;Z)Lnet/minecraft/world/entity/ai/behavior/EntityTracker;", ordinal = 1))
    private EntityTracker vbp$ghastlingWalksToEyes(Entity player, boolean trackEyeHeight, Operation<EntityTracker> original,
                                                   @Local(argsOnly = true) PathfinderMob body) {
        return body instanceof HappyGhast ? new EyeEntityTracker(player, true) : original.call(player, trackEyeHeight);
    }
}
