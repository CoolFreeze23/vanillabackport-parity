package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.platform.core.BuiltInCoreRegistry;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.SoundVariantHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** AI-VAR-01 / AI-VAR-03: VB's sound pick at Mob.finalizeSpawn RETURN keeps the one already drawn, without a draw. */
@Mixin(value = Wolf.class, priority = 1500)
public abstract class WolfLateSoundMixin {
    @WrapOperation(method = "vb$finalizeSpawn", remap = false,
        at = @At(value = "INVOKE", target = "Lcom/blackgear/platform/core/BuiltInCoreRegistry;getRandomElement(Lnet/minecraft/util/RandomSource;)Ljava/lang/Object;"))
    private Object vbp$keepEarlyPick(BuiltInCoreRegistry<?> registry, RandomSource random, Operation<Object> original) {
        return ((SoundVariantHolder<?>) (Object) this).vb$getSoundVariant();
    }
}
