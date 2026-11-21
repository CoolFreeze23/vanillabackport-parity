package com.coolfreeze23.vbparity.variants.mixin;

import com.blackgear.vanillabackport.common.api.modules.sound_variant.WolfSoundVariant;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.WolfSoundVariants;
import com.coolfreeze23.vbparity.variants.VariantSounds;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * VARIANTS-01: armor-absorbed damage plays {@code item.wolf_armor.damage}, as in native and vanilla 1.21.1, even when
 * VanillaBackport's wolf sound variants are on. VARIANTS-02: puppies use the native shared baby sound set (including
 * the baby step sound) whatever their sound variant is.
 * <p>{@code @WrapMethod} wraps the whole method, including VanillaBackport's cancellable HEAD callbacks, so these
 * decisions are taken first and VanillaBackport's sound variant only applies to adults without absorbing armor.
 */
@Mixin(value = Wolf.class, priority = 1100)
public abstract class WolfSoundMixin extends TamableAnimal {
    @Unique
    private static Map<SoundEvent, SoundEvent> vbp$babyByAdult;

    protected WolfSoundMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Shadow
    private boolean canArmorAbsorb(DamageSource damageSource) {
        throw new AssertionError();
    }

    @WrapMethod(method = "getHurtSound")
    private SoundEvent vbp$hurtSound(DamageSource source, Operation<SoundEvent> original) {
        if (this.canArmorAbsorb(source)) {
            return SoundEvents.WOLF_ARMOR_DAMAGE;
        }
        return this.isBaby() ? VariantSounds.BABY_WOLF_HURT : original.call(source);
    }

    @WrapMethod(method = "getDeathSound")
    private SoundEvent vbp$deathSound(Operation<SoundEvent> original) {
        return this.isBaby() ? VariantSounds.BABY_WOLF_DEATH : original.call();
    }

    /**
     * The adult logic (VanillaBackport's or vanilla's) has already made its {@code nextInt(3)} draw and picked a role
     * (growl / whine / pant / ambient); a puppy keeps that role and takes the baby event for it, so no extra draw is made.
     */
    @WrapMethod(method = "getAmbientSound")
    private SoundEvent vbp$ambientSound(Operation<SoundEvent> operation) {
        SoundEvent original = operation.call();
        if (!this.isBaby() || original == null) {
            return original;
        }
        SoundEvent baby = vbp$babyRoles().get(original);
        return baby != null ? baby : VariantSounds.BABY_WOLF_AMBIENT;
    }

    @ModifyExpressionValue(
        method = "playStepSound",
        at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;WOLF_STEP:Lnet/minecraft/sounds/SoundEvent;")
    )
    private SoundEvent vbp$stepSound(SoundEvent original) {
        return this.isBaby() ? VariantSounds.BABY_WOLF_STEP : original;
    }

    @Unique
    private static Map<SoundEvent, SoundEvent> vbp$babyRoles() {
        if (vbp$babyByAdult == null) {
            Map<SoundEvent, SoundEvent> map = new IdentityHashMap<>();
            map.put(SoundEvents.WOLF_AMBIENT, VariantSounds.BABY_WOLF_AMBIENT);
            map.put(SoundEvents.WOLF_GROWL, VariantSounds.BABY_WOLF_GROWL);
            map.put(SoundEvents.WOLF_PANT, VariantSounds.BABY_WOLF_PANT);
            map.put(SoundEvents.WOLF_WHINE, VariantSounds.BABY_WOLF_WHINE);
            for (WolfSoundVariant variant : WolfSoundVariants.REGISTRIES.values()) {
                map.put(variant.ambientSound().value(), VariantSounds.BABY_WOLF_AMBIENT);
                map.put(variant.growlSound().value(), VariantSounds.BABY_WOLF_GROWL);
                map.put(variant.pantSound().value(), VariantSounds.BABY_WOLF_PANT);
                map.put(variant.whineSound().value(), VariantSounds.BABY_WOLF_WHINE);
            }
            vbp$babyByAdult = map;
        }
        return vbp$babyByAdult;
    }
}
