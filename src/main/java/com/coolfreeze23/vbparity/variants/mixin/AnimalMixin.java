package com.coolfreeze23.vbparity.variants.mixin;

import com.coolfreeze23.vbparity.variants.SoundVariants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VARIANTS-03..06: cows, pigs, chickens and cats save and load their sound variant as {@code sound_variant}, like native.
 * VARIANTS-04: a pig that is fed (to breed, or to grow up) plays its set's eat sound, as native {@code Pig.playEatingSound}.
 */
@Mixin(Animal.class)
public abstract class AnimalMixin extends AgeableMob {
    protected AnimalMixin(EntityType<? extends AgeableMob> type, Level level) {
        super(type, level);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void vbp$saveSoundVariant(CompoundTag tag, CallbackInfo ci) {
        SoundVariants.Family<?> family = SoundVariants.familyOf(this);
        if (family != null) {
            family.save(this, tag);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void vbp$loadSoundVariant(CompoundTag tag, CallbackInfo ci) {
        SoundVariants.Family<?> family = SoundVariants.familyOf(this);
        if (family != null) {
            family.load(this, tag);
        }
    }

    @Inject(
        method = "mobInteract",
        at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Animal;setInLove(Lnet/minecraft/world/entity/player/Player;)V", shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Animal;ageUp(IZ)V", shift = At.Shift.AFTER)
        }
    )
    private void vbp$pigEatSound(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof Pig pig) {
            pig.makeSound(SoundVariants.PIG.sounds(pig).eat());
        }
    }
}
