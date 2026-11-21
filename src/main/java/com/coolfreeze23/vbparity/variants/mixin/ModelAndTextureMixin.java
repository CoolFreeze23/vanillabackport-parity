package com.coolfreeze23.vbparity.variants.mixin;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.ClientAsset;
import com.blackgear.vanillabackport.common.api.modules.mob_variant.ModelAndTexture;
import com.coolfreeze23.vbparity.variants.BabyAssets;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.MapCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** VARIANTS-10: the optional native {@code baby_asset_id} field on every VanillaBackport mob variant. */
@Mixin(ModelAndTexture.class)
public abstract class ModelAndTextureMixin implements BabyAssets.Holder {
    @Unique
    private ClientAsset vbp$babyAsset;

    @Override
    public ClientAsset vbp$getBabyAsset() {
        return this.vbp$babyAsset;
    }

    @Override
    public void vbp$setBabyAsset(ClientAsset asset) {
        this.vbp$babyAsset = asset;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @ModifyReturnValue(method = "codec", at = @At("RETURN"))
    private static MapCodec vbp$withBabyAsset(MapCodec original) {
        return BabyAssets.withBabyAsset(original);
    }
}
