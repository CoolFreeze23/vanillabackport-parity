package com.coolfreeze23.vbparity.variants.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.mob_rendering.AbstractVariantRenderer;
import com.blackgear.vanillabackport.client.api.modules.mob_rendering.ChickenVariantRenderer;
import com.blackgear.vanillabackport.client.api.modules.mob_rendering.CowVariantRenderer;
import com.blackgear.vanillabackport.client.api.modules.mob_rendering.PigVariantRenderer;
import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VARIANTS-10: for calves, piglets and chicks that get the native baby model and texture, VanillaBackport's adult variant
 * model and adult variant texture stand aside (native babies never use them).
 */
@Mixin(AbstractVariantRenderer.class)
public abstract class AbstractVariantRendererMixin {
    @org.spongepowered.asm.mixin.Unique
    private boolean vbp$isFarmRenderer() {
        Object self = this;
        return self instanceof CowVariantRenderer || self instanceof PigVariantRenderer || self instanceof ChickenVariantRenderer;
    }

    @Inject(method = "getTexture(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void vbp$noAdultTextureForBabies(LivingEntity entity, CallbackInfoReturnable<Optional<?>> cir) {
        if (this.vbp$isFarmRenderer() && BabyRendering.hasFarmBaby(entity)) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "getModel(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void vbp$noAdultModelForBabies(LivingEntity entity, CallbackInfoReturnable<Optional<?>> cir) {
        if (this.vbp$isFarmRenderer() && BabyRendering.hasFarmBaby(entity)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
