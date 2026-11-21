package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AI-VAR-06: native getBreedOffspring picks the parent variant with the breeding parent's own random (this.random).
 * VB drew from the newborn's random, and for cats and wolves (no VB data variants) re-picked the variant 1.21.1 had
 * already chosen from this.random. The parent's random is used and the re-pick is skipped.
 */
@Mixin(value = VariantDataHolder.class, remap = false)
public interface VariantInheritanceMixin {
    @Inject(method = "trySetOffspringVariant", at = @At("HEAD"), cancellable = true)
    private static void vbp$noRepick(LivingEntity child, LivingEntity father, LivingEntity mother, CallbackInfo ci) {
        boolean fatherData = VariantDataHolder.getHolder(father).flatMap(VariantDataHolder::getVariantData).isPresent();
        boolean motherData = VariantDataHolder.getHolder(mother).flatMap(VariantDataHolder::getVariantData).isPresent();
        if (!fatherData && !motherData) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "trySetOffspringVariant", at = @At("STORE"), ordinal = 0)
    private static RandomSource vbp$parentRandom(RandomSource childRandom, LivingEntity child, LivingEntity father, LivingEntity mother) {
        return father.getRandom();
    }
}
