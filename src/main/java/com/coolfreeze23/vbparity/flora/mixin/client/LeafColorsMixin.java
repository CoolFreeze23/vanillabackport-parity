package com.coolfreeze23.vbparity.flora.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.falling_leaves.LeafColors;
import com.coolfreeze23.vbparity.flora.client.FloraClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FLORA-19: native leaf-litter tint is the biome-blended dry-foliage colour, each biome giving its fixed colour (dark
 * forest, swamp, pale garden) or its colormap colour. VB paints fixed-colour biomes flat and blends only the colormap.
 */
@Mixin(LeafColors.class)
public abstract class LeafColorsMixin {
    @Inject(method = "getAverageDryFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void vbparity$blendedDryFoliage(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            cir.setReturnValue(level.getBlockTint(pos, FloraClient.DRY_FOLIAGE_COLOR_RESOLVER));
        }
    }
}
