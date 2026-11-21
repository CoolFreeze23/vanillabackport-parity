package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.MushroomCow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native 26.2 {@code MushroomCowRenderer}: mooshroom calves use {@code mooshroom_<red|brown>_baby}. Adults use the
 * Spring to Life cow model (swapped in by {@code LivingEntityRendererMixin}) with the 64x64 native textures, which
 * replace {@code red_mooshroom.png} and {@code brown_mooshroom.png}.
 */
@Mixin(MushroomCowRenderer.class)
public abstract class MushroomCowRendererMixin {
    @ModifyReturnValue(
        method = "getTextureLocation(Lnet/minecraft/world/entity/animal/MushroomCow;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("RETURN")
    )
    private ResourceLocation vbp$babyTexture(ResourceLocation original, MushroomCow entity) {
        ResourceLocation baby = BabyRendering.babyTexture(entity);
        return baby != null ? baby : original;
    }
}
