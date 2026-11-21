package com.coolfreeze23.vbparity.copper.mixin.client;

import com.blackgear.vanillabackport.client.level.renderer.entity.mob.CopperGolemRenderer;
import com.coolfreeze23.vbparity.copper.client.CopperGolemHeadLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** COPPER-25: native adds a CustomHeadLayer after the block-decoration layer. */
@Mixin(CopperGolemRenderer.class)
public abstract class CopperGolemRendererMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void vbparity$copper$headLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        CopperGolemRenderer self = (CopperGolemRenderer) (Object) this;
        ((LivingEntityRenderer) self).addLayer(new CopperGolemHeadLayer(self, context.getModelSet(), context.getItemInHandRenderer()));
    }
}
