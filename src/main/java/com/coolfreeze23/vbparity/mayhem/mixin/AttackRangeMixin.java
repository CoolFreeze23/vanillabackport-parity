package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.components.AttackRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** MAYHEM-39: native AttackRange codec defaults: max_reach 3.0, max_creative_reach 5.0, hitbox_margin 0.3. */
@Mixin(value = AttackRange.class, remap = false)
public abstract class AttackRangeMixin {
    @ModifyArg(
        method = "lambda$static$0",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;optionalFieldOf(Ljava/lang/String;Ljava/lang/Object;)Lcom/mojang/serialization/MapCodec;"),
        index = 1
    )
    private static Object vbp$nativeDefaults(String name, Object defaultValue) {
        return switch (name) {
            case "max_reach" -> 3.0F;
            case "max_creative_reach" -> 5.0F;
            case "hitbox_margin" -> 0.3F;
            default -> defaultValue;
        };
    }
}
