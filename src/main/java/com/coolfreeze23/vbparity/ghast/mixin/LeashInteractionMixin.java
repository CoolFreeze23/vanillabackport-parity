package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.integrations.interactions.LeashInteraction;
import com.coolfreeze23.vbparity.ghast.GhastSounds;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** GHAST-32: the current game's lead sounds for tying, untying and shearing leash connections. */
@Mixin(LeashInteraction.class)
public abstract class LeashInteractionMixin {
    @Redirect(method = "onInteract", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;LEASH_KNOT_PLACE:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$leadTied() {
        return GhastSounds.LEAD_TIED;
    }

    @Redirect(method = "onInteract", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;LEASH_KNOT_BREAK:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$leadUntied() {
        return GhastSounds.LEAD_UNTIED;
    }

    @Redirect(method = "shearAllConnections", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;SHEEP_SHEAR:Lnet/minecraft/sounds/SoundEvent;"))
    private static SoundEvent vbp$shearsSnip() {
        return GhastSounds.SHEARS_SNIP;
    }
}
