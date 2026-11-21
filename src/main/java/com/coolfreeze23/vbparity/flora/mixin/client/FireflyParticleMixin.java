package com.coolfreeze23.vbparity.flora.mixin.client;

import com.blackgear.vanillabackport.client.level.particle.FireflyParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** FLORA-20: native firefly wander rolls come from the particle's own random ({@code nextFloat}), not Math.random. */
@Mixin(FireflyParticle.class)
public abstract class FireflyParticleMixin extends TextureSheetParticle {
    protected FireflyParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
    private double vbparity$particleRandom() {
        return this.random.nextFloat();
    }
}
