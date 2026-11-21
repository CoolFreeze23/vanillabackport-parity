package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.platform.core.BuiltInCoreRegistry;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.SoundVariantHolder;
import com.coolfreeze23.vbparity.ai.NativeSoundOrder;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** AI-VAR-06: a bred wolf's sound variant is a native pick (parent random, id order). */
@Mixin(value = SoundVariantHolder.class, remap = false)
public interface WolfSoundInheritanceMixin {
    @Inject(method = "trySetOffspringVariant", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void vbp$nativeOrder(LivingEntity child, LivingEntity father, BuiltInCoreRegistry registry, CallbackInfo ci) {
        SoundVariantHolder holder = SoundVariantHolder.of(child);
        if (holder != null) {
            holder.vb$setSoundVariant(NativeSoundOrder.pick(registry, father.getRandom()));
        }
        ci.cancel();
    }
}
