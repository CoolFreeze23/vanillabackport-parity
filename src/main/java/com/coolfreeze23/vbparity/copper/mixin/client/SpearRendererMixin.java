package com.coolfreeze23.vbparity.copper.mixin.client;

import com.blackgear.vanillabackport.client.level.renderer.item.SpearRenderer;
import com.coolfreeze23.vbparity.copper.CopperShelf;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** COPPER-27: native spear item definitions use the flat model on_shelf, like gui, ground and fixed. */
@Mixin(SpearRenderer.class)
public abstract class SpearRendererMixin {
    @ModifyExpressionValue(
        method = "renderFirstPerson",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/ItemDisplayContext;FIXED:Lnet/minecraft/world/item/ItemDisplayContext;")
    )
    private ItemDisplayContext vbparity$onShelfIsFlat(ItemDisplayContext fixed, @Local(argsOnly = true) ItemDisplayContext context) {
        return context == CopperShelf.onShelf() ? context : fixed;
    }
}
