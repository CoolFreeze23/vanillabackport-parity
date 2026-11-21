package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilus;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** MAYHEM-16: the zombie nautilus eating sound goes through makeSound (voice pitch). */
@Mixin(value = ZombieNautilus.class, priority = 1500)
public abstract class ZombieNautilusMixin {
    @Redirect(
        method = "playEatingSound",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/nautilus/ZombieNautilus;playSound(Lnet/minecraft/sounds/SoundEvent;)V")
    )
    private void vbp$makeEatSound(ZombieNautilus self, @Nullable SoundEvent sound) {
        self.makeSound(sound);
    }
}
