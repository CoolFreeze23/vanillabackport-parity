package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.platform.core.util.event.ResultHolder;
import com.blackgear.vanillabackport.client.level.renderer.item.BannerPatternRenderer;
import com.coolfreeze23.vbparity.misc.MiscContent;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VB registers its banner-pattern renderer for every BannerPatternItem and shows a blank pattern for items outside
 * its own list. The native Field Masoned and Bordure Indented pattern items have their own 26.2 models, so they keep
 * their normal model.
 */
@Mixin(BannerPatternRenderer.class)
public abstract class BannerPatternRendererMixin {
    @Inject(method = "renderFirstPerson", at = @At("HEAD"), cancellable = true)
    private void vbp$ownModelFirstPerson(ItemStack stack, ItemDisplayContext context, ItemModelShaper shaper,
                                         CallbackInfoReturnable<ResultHolder<BakedModel>> cir) {
        if (vbp$isNativeOnly(stack)) {
            cir.setReturnValue(ResultHolder.pass());
        }
    }

    @Inject(method = "renderThirdPerson", at = @At("HEAD"), cancellable = true)
    private void vbp$ownModelThirdPerson(ItemStack stack, ItemModelShaper shaper, CallbackInfoReturnable<ResultHolder<BakedModel>> cir) {
        if (vbp$isNativeOnly(stack)) {
            cir.setReturnValue(ResultHolder.pass());
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean vbp$isNativeOnly(ItemStack stack) {
        return stack.is(MiscContent.fieldMasonedBannerPattern) || stack.is(MiscContent.bordureIndentedBannerPattern);
    }
}
