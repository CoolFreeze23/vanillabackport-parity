package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.mayhem.MinecartSpeedFlag;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MAYHEM-33: skips VB's second, end-of-tick computeSpeed on a minecart that already computed it this tick. */
@Mixin(value = Entity.class, priority = 1500)
public abstract class EntityComputeSpeedMixin {
    @Inject(method = "computeSpeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void vbp$skipLateMinecartSpeed(CallbackInfo ci) {
        if (this instanceof MinecartSpeedFlag flag && flag.vbp$consumeSpeedComputed()) {
            ci.cancel();
        }
    }
}
