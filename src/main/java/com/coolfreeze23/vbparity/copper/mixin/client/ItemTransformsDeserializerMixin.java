package com.coolfreeze23.vbparity.copper.mixin.client;

import com.coolfreeze23.vbparity.copper.CopperShelf;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** COPPER-27: the extended ON_SHELF context reads native's "on_shelf" display key. */
@Mixin(targets = "net.minecraft.client.renderer.block.model.ItemTransforms$Deserializer")
public abstract class ItemTransformsDeserializerMixin {
    @ModifyExpressionValue(
        method = "getTransform(Lcom/google/gson/JsonDeserializationContext;Lcom/google/gson/JsonObject;Lnet/minecraft/world/item/ItemDisplayContext;)Lnet/minecraft/client/renderer/block/model/ItemTransform;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemDisplayContext;getSerializedName()Ljava/lang/String;")
    )
    private String vbparity$nativeOnShelfKey(String name, @Local(argsOnly = true) ItemDisplayContext context) {
        return context == CopperShelf.onShelf() ? CopperShelf.NATIVE_KEY : name;
    }
}
