package com.coolfreeze23.vbparity.variants.mixin;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.spawn.check.MoonBrightnessCheck;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VanillaBackport's {@code moon_brightness} spawn condition reads the native moon brightness (see {@link CatNativeSpawnMixin}). */
@Mixin(MoonBrightnessCheck.class)
public abstract class MoonBrightnessCheckMixin {
    @WrapOperation(
        method = "test(Lcom/blackgear/vanillabackport/common/api/modules/mob_variant/spawn/SpawnContext;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMoonBrightness()F")
    )
    private float vbp$nativeMoon(ServerLevel level, Operation<Float> original) {
        return level.dimensionType().hasFixedTime() ? 1.0F : original.call(level);
    }
}
