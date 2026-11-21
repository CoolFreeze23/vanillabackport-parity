package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.mayhem.MinecartSpeedFlag;

import com.blackgear.vanillabackport.common.api.extensions.entity.movement.MotionAwareEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MAYHEM-33: native AbstractMinecart.tick computes the known speed before the portal step and the track movement
 * (so it lags one tick). VB computed it at the end of tick; that call is skipped by {@link EntityComputeSpeedMixin}.
 */
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartSpeedMixin implements MinecartSpeedFlag {
    @Unique
    private boolean vbp$speedComputed;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;handlePortal()V"))
    private void vbp$computeSpeedBeforeMoving(CallbackInfo ci) {
        this.vbp$speedComputed = false;
        ((MotionAwareEntity) this).computeSpeed();
        this.vbp$speedComputed = true;
    }

    @Override
    public boolean vbp$consumeSpeedComputed() {
        boolean computed = this.vbp$speedComputed;
        this.vbp$speedComputed = false;
        return computed;
    }
}
