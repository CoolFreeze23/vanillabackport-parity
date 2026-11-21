package com.coolfreeze23.vbparity.variants.mixin;

import com.blackgear.vanillabackport.core.util.AdditionalCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Native {@code ExtraCodecs.floatRange(min, max)} accepts {@code min <= value <= max}. VanillaBackport's copy builds it
 * on its min-exclusive helper, so {@code 0.0} is rejected by every range that starts at zero: the {@code attack_range}
 * reach and hitbox-margin fields and the {@code minimum_attack_range} (native {@code minimum_attack_charge}) component.
 * A datapack or command that writes {@code 0}, and an item stack whose component holds {@code 0}, fail to decode (and
 * the codec's validation also refuses to encode such a value).
 */
@Mixin(AdditionalCodecs.class)
public abstract class AdditionalCodecsMixin {
    @Inject(method = "floatRange(FF)Lcom/mojang/serialization/Codec;", at = @At("HEAD"), cancellable = true)
    private static void vbp$inclusiveRange(float minInclusive, float maxInclusive, CallbackInfoReturnable<Codec<Float>> cir) {
        cir.setReturnValue(Codec.FLOAT.validate(
            value -> value.compareTo(minInclusive) >= 0 && value.compareTo(maxInclusive) <= 0
                ? DataResult.success(value)
                : DataResult.error(() -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: " + value)
        ));
    }
}
