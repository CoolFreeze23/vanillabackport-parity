package com.coolfreeze23.vbparity.variants.mixin;

import com.coolfreeze23.vbparity.variants.BabyDimensions;
import com.coolfreeze23.vbparity.variants.SoundVariants;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * VARIANTS-06: every cat sound comes from the cat's sound variant, and kittens use the native baby cat set.
 * VARIANTS-13: native kitten dimensions.
 */
@Mixin(Cat.class)
public abstract class CatMixin extends TamableAnimal {
    protected CatMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BabyDimensions.CAT : super.getDefaultDimensions(pose);
    }

    @ModifyExpressionValue(method = {"getAmbientSound", "handleLieDown"}, at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_PURR:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$purr(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).purr();
    }

    @ModifyExpressionValue(method = "getAmbientSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_PURREOW:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$purreow(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).purreow();
    }

    @ModifyExpressionValue(method = "getAmbientSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_AMBIENT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$ambient(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).ambient();
    }

    @ModifyExpressionValue(method = "getAmbientSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_STRAY_AMBIENT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$strayAmbient(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).strayAmbient();
    }

    @ModifyExpressionValue(method = "hiss", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_HISS:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$hiss(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).hiss();
    }

    @ModifyExpressionValue(method = "getHurtSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_HURT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$hurt(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).hurt();
    }

    @ModifyExpressionValue(method = "getDeathSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_DEATH:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$death(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).death();
    }

    @ModifyExpressionValue(method = "usePlayerItem", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_EAT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$eat(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).eat();
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;CAT_BEG_FOR_FOOD:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$begForFood(SoundEvent original) {
        return SoundVariants.CAT.sounds(this).begForFood();
    }
}
