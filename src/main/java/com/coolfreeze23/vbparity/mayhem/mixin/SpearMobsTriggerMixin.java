package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.triggers.SpearMobsTrigger;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** MAYHEM-41: native spear_mobs trigger reads "player" with EntityPredicate.ADVANCEMENT_CODEC (plain predicate or condition list). */
@Mixin(value = SpearMobsTrigger.TriggerInstance.class, remap = false)
public abstract class SpearMobsTriggerMixin {
    @ModifyExpressionValue(
        method = "lambda$static$0",
        at = @At(value = "FIELD", target = "Lnet/minecraft/advancements/critereon/ContextAwarePredicate;CODEC:Lcom/mojang/serialization/Codec;")
    )
    private static Codec<ContextAwarePredicate> vbp$advancementCodec(Codec<ContextAwarePredicate> original) {
        return EntityPredicate.ADVANCEMENT_CODEC;
    }
}
