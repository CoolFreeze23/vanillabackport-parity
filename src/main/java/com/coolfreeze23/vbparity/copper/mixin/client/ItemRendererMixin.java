package com.coolfreeze23.vbparity.copper.mixin.client;

import com.coolfreeze23.vbparity.copper.CopperShelf;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * COPPER-27: native items/trident.json and spyglass.json pick the flat inventory model for gui, ground, fixed and
 * on_shelf. In 1.21.1 that choice is the "GUI || GROUND || FIXED" test in ItemRenderer.render; ON_SHELF joins FIXED.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyExpressionValue(
        method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/ItemDisplayContext;FIXED:Lnet/minecraft/world/item/ItemDisplayContext;", ordinal = 0)
    )
    private ItemDisplayContext vbparity$onShelfIsFlat(ItemDisplayContext fixed, @Local(argsOnly = true) ItemDisplayContext context) {
        return context == CopperShelf.onShelf() ? context : fixed;
    }
}
