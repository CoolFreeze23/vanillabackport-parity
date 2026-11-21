package com.coolfreeze23.vbparity.sulfur.mixin.client;

import com.blackgear.vanillabackport.client.level.particle.GeyserBaseParticle;
import com.blackgear.vanillabackport.client.level.particle.particleoptions.GeyserBaseParticleOptions;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** SULFUR-33. Native burst impulse is {@code base + 0.25 * waterBlocks} (VB: base + 0.25 + waterBlocks). */
@Mixin(GeyserBaseParticle.Provider.class)
public abstract class GeyserBaseParticleProviderMixin {
    @ModifyVariable(
        method = "createParticle(Lcom/blackgear/vanillabackport/client/level/particle/particleoptions/GeyserBaseParticleOptions;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("STORE"),
        ordinal = 0
    )
    private float vbp$nativeBurstImpulse(float burstImpulse, @Local(argsOnly = true) GeyserBaseParticleOptions type) {
        return type.burstImpulseBase() + 0.25F * type.waterBlocks();
    }
}
