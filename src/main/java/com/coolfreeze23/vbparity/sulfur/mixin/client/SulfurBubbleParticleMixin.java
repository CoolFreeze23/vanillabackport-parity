package com.coolfreeze23.vbparity.sulfur.mixin.client;

import com.blackgear.vanillabackport.client.level.particle.SulfurBubbleParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SULFUR-34. Native bubbles use the 4-argument particle constructor, so they start with no vertical speed. */
@Mixin(SulfurBubbleParticle.class)
public abstract class SulfurBubbleParticleMixin extends TextureSheetParticle {
    protected SulfurBubbleParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void vbp$noInitialRise(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        this.yd = 0.0;
    }
}
