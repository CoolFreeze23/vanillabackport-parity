package com.coolfreeze23.vbparity.ghast.mixin.client;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GHAST-18: third-person distance while riding a happy ghast is the current game's
 * max(playerScale * playerDistance, ghastScale * 8), where the player distance (4) still goes through NeoForge's
 * detached-camera hook. Runs before VB's fixed-8 hook at the same point (lower priority applies first) and cancels.
 */
@Mixin(value = Camera.class, priority = 900)
public abstract class CameraMixin {
    @Shadow
    protected abstract void move(float zoom, float dy, float dx);

    @Shadow
    protected abstract float getMaxZoom(float maxZoom);

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), cancellable = true)
    private void vbp$happyGhastCameraDistance(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (entity.isPassenger() && entity.getVehicle() instanceof HappyGhast ghast) {
            float cameraScale = entity instanceof LivingEntity living ? living.getScale() : 1.0F;
            float camera = ClientHooks.getDetachedCameraDistance((Camera) (Object) this, thirdPersonReverse, cameraScale, 4.0F) * cameraScale;
            float mount = ghast.getScale() * 8.0F;
            this.move(-this.getMaxZoom(Math.max(camera, mount)), 0.0F, 0.0F);
            ci.cancel();
        }
    }
}
