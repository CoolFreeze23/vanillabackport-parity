package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GHAST-15: ghastlings panic with the current game's AnimalPanic(2.0F, 0): an air or water position ahead of them
 * (within 90 degrees of where they look) instead of a land position.
 */
@Mixin(AnimalPanic.class)
public abstract class AnimalPanicMixin {
    @WrapOperation(method = "getPanicPos", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/util/LandRandomPos;getPos(Lnet/minecraft/world/entity/PathfinderMob;II)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 vbp$ghastlingPanicsInTheAir(PathfinderMob mob, int maxDistance, int yRange, Operation<Vec3> original) {
        if (mob instanceof HappyGhast) {
            Vec3 view = mob.getViewVector(0.0F);
            return AirAndWaterRandomPos.getPos(mob, 5, 4, 0, view.x, view.z, (float) (Math.PI / 2));
        }

        return original.call(mob, maxDistance, yRange);
    }
}
