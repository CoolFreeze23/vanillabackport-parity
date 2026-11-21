package com.coolfreeze23.vbparity.variants.mixin;

import com.coolfreeze23.vbparity.variants.BabyDimensions;
import com.coolfreeze23.vbparity.variants.SoundVariants;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VARIANTS-05: chickens take their sounds from their sound variant, chicks from the native baby chicken set.
 * VARIANTS-13: native chick dimensions.
 */
@Mixin(Chicken.class)
public abstract class ChickenMixin extends Animal {
    protected ChickenMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void vbp$babyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (this.isBaby()) {
            cir.setReturnValue(BabyDimensions.CHICKEN);
        }
    }

    @ModifyExpressionValue(method = "getAmbientSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CHICKEN_AMBIENT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$ambient(SoundEvent original) {
        return SoundVariants.CHICKEN.sounds(this).ambient();
    }

    @ModifyExpressionValue(method = "getHurtSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CHICKEN_HURT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$hurt(SoundEvent original) {
        return SoundVariants.CHICKEN.sounds(this).hurt();
    }

    @ModifyExpressionValue(method = "getDeathSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CHICKEN_DEATH:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$death(SoundEvent original) {
        return SoundVariants.CHICKEN.sounds(this).death();
    }

    @ModifyExpressionValue(method = "playStepSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CHICKEN_STEP:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$step(SoundEvent original) {
        return SoundVariants.CHICKEN.sounds(this).step();
    }
}
