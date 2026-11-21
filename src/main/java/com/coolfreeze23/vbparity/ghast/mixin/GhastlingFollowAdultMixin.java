package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.GhastlingFollowAdult;
import com.coolfreeze23.vbparity.ghast.EyeEntityTracker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GHAST-14: like the current game's BabyFollowAdult with targetEye = true, both the walk target and the look target
 * track the leader's eyes, and the walk target block is the block at the leader's eyes.
 */
@Mixin(GhastlingFollowAdult.class)
public abstract class GhastlingFollowAdultMixin {
    @WrapOperation(method = "lambda$create$1", at = @At(value = "NEW",
        target = "(Lnet/minecraft/world/entity/Entity;Z)Lnet/minecraft/world/entity/ai/behavior/EntityTracker;"))
    private static EntityTracker vbp$trackEyes(Entity leader, boolean trackEyeHeight, Operation<EntityTracker> original) {
        return new EyeEntityTracker(leader, true);
    }
}
