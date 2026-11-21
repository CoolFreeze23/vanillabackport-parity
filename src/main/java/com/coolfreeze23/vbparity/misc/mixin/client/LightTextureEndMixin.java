package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.end_flashes.EndFlashRenderer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Native End lighting (the_end.json {@code sky_light_factor 0.0}, {@code sky_light_color #ac60cd};
 * LightmapRenderStateExtractor adds the End flash intensity to the sky factor). VB gives the End sky light, and with
 * the 1.21.1 lightmap (End fixed time = noon, sky factor 1, sky colour white) every sky-exposed End block was lit
 * like overworld noon. Here the End sky factor is the flash intensity (0 without a flash, /3 under boss fog) and the
 * sky colour is #ac60cd. VB's separate post-gamma tint is turned off in EndFlashRendererMixin.
 */
@Mixin(value = LightTexture.class, priority = 2000)
public abstract class LightTextureEndMixin {
    @Unique
    private static final float VBP_SKY_R = 0xAC / 255.0F;
    @Unique
    private static final float VBP_SKY_G = 0x60 / 255.0F;
    @Unique
    private static final float VBP_SKY_B = 0xCD / 255.0F;

    @Shadow
    @Final
    private Minecraft minecraft;

    // Local slot 4 is the sky factor (f1 = skyDarken * 0.95 + 0.05, or 1.0 during a lightning flash).
    @ModifyVariable(method = "updateLightTexture", index = 4,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;darknessEffectScale()Lnet/minecraft/client/OptionInstance;"))
    private float vbp$endSkyFactor(float skyFactor, float partialTicks) {
        return this.vbp$isEnd() ? EndFlashRenderer.calculateLightIntensity(this.minecraft, partialTicks) : skyFactor;
    }

    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lorg/joml/Vector3f;lerp(Lorg/joml/Vector3fc;F)Lorg/joml/Vector3f;"))
    private Vector3f vbp$endSkyColour(Vector3f skyColour) {
        return this.vbp$isEnd() ? skyColour.set(VBP_SKY_R, VBP_SKY_G, VBP_SKY_B) : skyColour;
    }

    @Unique
    private boolean vbp$isEnd() {
        ClientLevel level = this.minecraft.level;
        return level != null && level.effects().skyType() == DimensionSpecialEffects.SkyType.END;
    }
}
