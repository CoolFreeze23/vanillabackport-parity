package com.coolfreeze23.vbparity.sulfur.mixin.client;

import com.blackgear.vanillabackport.client.level.particle.SulfurCubeParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** SULFUR-35. Native goo (BreakingItemParticle.SulfurCubeProvider) keeps the full random particle velocity, no x0.1 damping. */
@Mixin(SulfurCubeParticle.Provider.class)
public abstract class SulfurCubeParticleProviderMixin {
    @Redirect(
        method = "createParticle(Lnet/minecraft/core/particles/SimpleParticleType;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lcom/blackgear/vanillabackport/client/level/particle/SulfurCubeParticle;"
        )
    )
    private SulfurCubeParticle vbp$undampedGoo(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        return new SulfurCubeParticle(level, x, y, z);
    }
}
