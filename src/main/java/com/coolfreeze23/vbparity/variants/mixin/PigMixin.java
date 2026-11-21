package com.coolfreeze23.vbparity.variants.mixin;

import com.coolfreeze23.vbparity.variants.BabyDimensions;
import com.coolfreeze23.vbparity.variants.SoundVariants;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * VARIANTS-04: pigs take their sounds from their sound variant, piglets from the native baby pig set (the eat sound is
 * played from {@link AnimalMixin}). VARIANTS-13: native piglet dimensions.
 */
@Mixin(Pig.class)
public abstract class PigMixin extends Animal {
    protected PigMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BabyDimensions.PIG : super.getDefaultDimensions(pose);
    }

    @ModifyExpressionValue(method = "getAmbientSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;PIG_AMBIENT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$ambient(SoundEvent original) {
        return SoundVariants.PIG.sounds(this).ambient();
    }

    @ModifyExpressionValue(method = "getHurtSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;PIG_HURT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$hurt(SoundEvent original) {
        return SoundVariants.PIG.sounds(this).hurt();
    }

    @ModifyExpressionValue(method = "getDeathSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;PIG_DEATH:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$death(SoundEvent original) {
        return SoundVariants.PIG.sounds(this).death();
    }

    @ModifyExpressionValue(method = "playStepSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;PIG_STEP:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$step(SoundEvent original) {
        return SoundVariants.PIG.sounds(this).step();
    }
}
