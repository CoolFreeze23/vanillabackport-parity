package com.coolfreeze23.vbparity.ai.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-GHS-01: native Ghast.RandomFloatAroundGoal keeps a wander target when mob.isWithinHome(target) holds, i.e. the
 * exact position against the home block centre: homePosition.distToCenterSqr(target) < radius * radius. VB floored the
 * target to a block and compared block corners (Mob.isWithinRestriction(BlockPos)).
 */
@Mixin(targets = "com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast$RandomFloatAroundGoal", remap = false)
public abstract class HappyGhastWanderHomeMixin {
    @Shadow
    private static Vec3 chooseRandomPosition(Vec3 origin, RandomSource random) {
        throw new AssertionError();
    }

    @Inject(method = "chooseRandomPositionWithRestriction", at = @At("HEAD"), cancellable = true)
    private static void vbp$exactHomeCheck(Mob mob, Vec3 origin, RandomSource random, CallbackInfoReturnable<Vec3> cir) {
        Vec3 target = chooseRandomPosition(origin, random);
        if (mob.hasRestriction()) {
            float radius = mob.getRestrictRadius();
            boolean within = radius == -1.0F || mob.getRestrictCenter().distToCenterSqr(target) < (double) (radius * radius);
            cir.setReturnValue(within ? target : null);
        } else {
            cir.setReturnValue(target);
        }
    }
}
