package com.coolfreeze23.vbparity.variants.mixin;

import com.coolfreeze23.vbparity.variants.BabyDimensions;
import com.coolfreeze23.vbparity.variants.SoundVariants;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VARIANTS-03: cows take their ambient, hurt, death and step sounds from their sound variant (mooshrooms stay classic).
 * VARIANTS-13: native calf dimensions (also used by native mooshroom calves, which extend Cow here).
 */
@Mixin(Cow.class)
public abstract class CowMixin extends Animal {
    protected CowMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void vbp$babyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (this.isBaby()) {
            cir.setReturnValue(BabyDimensions.COW);
        }
    }

    @Unique
    private SoundVariants.CowSet vbp$sounds() {
        return SoundVariants.isPlainCow(this) ? SoundVariants.COW.sounds(this) : null;
    }

    @ModifyExpressionValue(method = "getAmbientSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;COW_AMBIENT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$ambient(SoundEvent original) {
        SoundVariants.CowSet set = this.vbp$sounds();
        return set != null ? set.ambient() : original;
    }

    @ModifyExpressionValue(method = "getHurtSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;COW_HURT:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$hurt(SoundEvent original) {
        SoundVariants.CowSet set = this.vbp$sounds();
        return set != null ? set.hurt() : original;
    }

    @ModifyExpressionValue(method = "getDeathSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;COW_DEATH:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$death(SoundEvent original) {
        SoundVariants.CowSet set = this.vbp$sounds();
        return set != null ? set.death() : original;
    }

    @ModifyExpressionValue(method = "playStepSound", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;COW_STEP:Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent vbp$step(SoundEvent original) {
        SoundVariants.CowSet set = this.vbp$sounds();
        return set != null ? set.step() : original;
    }
}
