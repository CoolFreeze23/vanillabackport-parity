package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.integrations.interactions.LeashInteraction;
import com.coolfreeze23.vbparity.misc.MiscContent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native Entity.interact lead handling: {@code item.lead.tied} / {@code item.lead.untied} sounds, {@code item.shears.snip}
 * when shearing leashes, and {@code entity_action} (not {@code entity_interact}) for mass-leashing onto a mob.
 */
@Mixin(LeashInteraction.class)
public abstract class LeashInteractionMixin {
    @ModifyExpressionValue(method = "onInteract", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;LEASH_KNOT_PLACE:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$leadTied(SoundEvent original) {
        return MiscContent.sound(MiscContent.LEAD_TIED);
    }

    @ModifyExpressionValue(method = "onInteract", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;LEASH_KNOT_BREAK:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$leadUntied(SoundEvent original) {
        return MiscContent.sound(MiscContent.LEAD_UNTIED);
    }

    @ModifyExpressionValue(method = "onInteract", at = @At(value = "FIELD", ordinal = 0,
            target = "Lnet/minecraft/world/level/gameevent/GameEvent;ENTITY_INTERACT:Lnet/minecraft/core/Holder$Reference;"))
    private Holder.Reference<GameEvent> vbp$entityAction(Holder.Reference<GameEvent> original) {
        return GameEvent.ENTITY_ACTION;
    }

    @ModifyExpressionValue(method = "shearAllConnections", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;SHEEP_SHEAR:Lnet/minecraft/sounds/SoundEvent;"))
    private static SoundEvent vbp$shearsSnip(SoundEvent original) {
        return MiscContent.sound(MiscContent.SHEARS_SNIP);
    }
}
