package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native pale oak and poplar boats (and their chest boats) are {@code sized(1.375F, 0.5625F).eyeHeight(0.5625F)},
 * like every other boat. VB's custom boat types leave out the eye height (default 0.85 x 0.5625 = 0.478125),
 * which moves the lead attachment point and every eye-anchored check.
 */
@Mixin(ModEntityTypes.class)
public abstract class CustomBoatEyeHeightMixin {
    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType$Builder;sized(FF)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static EntityType.Builder<?> vbp$boatEyeHeight(EntityType.Builder<?> builder, float width, float height,
                                                         Operation<EntityType.Builder<?>> original) {
        EntityType.Builder<?> sized = original.call(builder, width, height);
        return width == 1.375F && height == 0.5625F ? sized.eyeHeight(0.5625F) : sized;
    }
}
