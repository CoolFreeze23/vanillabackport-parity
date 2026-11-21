package com.coolfreeze23.vbparity.variants.mixin;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantUtils;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * VARIANTS-20: native {@code VariantUtils.selectVariantToSpawn} has no spawn-chance roll. With VanillaBackport's
 * {@code mob_variants_spawn_chance} at its default 1.0 the roll can never fail, so skip the draw and keep the level
 * random in step with native. Below 1.0 (a VanillaBackport-only knob) the roll is kept.
 */
@Mixin(VariantUtils.class)
public abstract class VariantUtilsMixin {
    @WrapOperation(
        method = "selectVariantToSpawn(Lcom/blackgear/vanillabackport/common/api/modules/mob_variant/spawn/SpawnContext;Lcom/blackgear/platform/core/BuiltInCoreRegistry;Lcom/blackgear/vanillabackport/common/api/modules/mob_variant/VariantSpawner;)Ljava/util/Optional;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F")
    )
    private static float vbp$noChanceRoll(RandomSource random, Operation<Float> original) {
        return VanillaBackport.COMMON_CONFIG.mobVariantSpawnChance.get() >= 1.0 ? 0.0F : original.call(random);
    }
}
