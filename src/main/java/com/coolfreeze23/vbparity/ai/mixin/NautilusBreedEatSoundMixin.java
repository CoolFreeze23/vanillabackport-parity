package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AI-MNT-D1: native Animal.mobInteract plays the eating sound after feeding to breed and after feeding a baby. */
@Mixin(Animal.class)
public abstract class NautilusBreedEatSoundMixin {
    @Inject(
        method = "mobInteract",
        at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Animal;setInLove(Lnet/minecraft/world/entity/player/Player;)V", shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Animal;ageUp(IZ)V", shift = At.Shift.AFTER)
        }
    )
    private void vbp$nautilusEatSound(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof AbstractNautilus nautilus) {
            ((AbstractNautilusInvoker) nautilus).vbp$playEatingSound();
        }
    }
}
