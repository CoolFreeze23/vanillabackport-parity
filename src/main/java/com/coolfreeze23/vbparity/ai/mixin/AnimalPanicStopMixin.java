package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.AiParity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** AI-GHS-D4 / AI-MNT-D2: native AnimalPanic.start also stops the current path. Native for the mobs VB adds only. */
@Mixin(AnimalPanic.class)
public abstract class AnimalPanicStopMixin {
    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V", at = @At("TAIL"))
    private void vbp$stopPath(ServerLevel level, PathfinderMob entity, long gameTime, CallbackInfo ci) {
        if (AiParity.isVbAdded(entity)) {
            entity.getNavigation().stop();
        }
    }
}
