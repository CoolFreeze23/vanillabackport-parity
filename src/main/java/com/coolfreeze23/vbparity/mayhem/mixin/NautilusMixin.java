package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.Nautilus;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * MAYHEM-16: the eating sound goes through makeSound (voice pitch, baby pitch).
 * MAYHEM-21: native Nautilus.baseTick only handles the air supply on the server and when the AI is on.
 */
@Mixin(value = Nautilus.class, priority = 1500)
public abstract class NautilusMixin extends TamableAnimal {
    protected NautilusMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Redirect(
        method = "playEatingSound",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/nautilus/Nautilus;playSound(Lnet/minecraft/sounds/SoundEvent;)V")
    )
    private void vbp$makeEatSound(Nautilus self, @Nullable SoundEvent sound) {
        self.makeSound(sound);
    }

    @WrapWithCondition(
        method = "baseTick",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/nautilus/Nautilus;handleAirSupply(I)V")
    )
    private boolean vbp$airOnServerWithAi(Nautilus self, int airSupply) {
        return !this.isNoAi() && !this.level().isClientSide();
    }

    /** TPHYS-A03: native BABY_DIMENSIONS = type dimensions x 0.5 with the passenger point at 0.5. */
    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void vbp$babyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (this.isBaby()) {
            cir.setReturnValue(this.getType().getDimensions().scale(0.5F)
                .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.5F, 0.0F)));
        }
    }
}
