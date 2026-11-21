package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.integrations.worldgen.BiomeGeneration;
import com.coolfreeze23.vbparity.sulfur.SulfurRegistration;
import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Native 26.2 OverworldBiomeBuilder.addUndergroundBiomes adds sulfur caves after lush caves and before deep dark.
 * Platform appends VB's point at the end of addBiomes instead, which changes the parameter-list order (R-tree tie order)
 * and the possible-biome order (and so the global feature order). The point itself is the same object VB adds;
 * {@link SulfurBiomeGenerationMixin} drops VB's late copy. Gated by VB's has_sulfur_caves, read at common setup (when VB registers its own listener).
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class SulfurOverworldBiomeBuilderMixin {
    @Inject(
        method = "addUndergroundBiomes",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/OverworldBiomeBuilder;addBottomBiome(Ljava/util/function/Consumer;Lnet/minecraft/world/level/biome/Climate$Parameter;Lnet/minecraft/world/level/biome/Climate$Parameter;Lnet/minecraft/world/level/biome/Climate$Parameter;Lnet/minecraft/world/level/biome/Climate$Parameter;Lnet/minecraft/world/level/biome/Climate$Parameter;FLnet/minecraft/resources/ResourceKey;)V")
    )
    private void vbparity$sulfur$nativeSulfurCavesPlace(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, CallbackInfo ci) {
        if (SulfurRegistration.sulfurCavesPlacement()) {
            biomes.accept(BiomeGeneration.SULFUR_CAVES);
        }
    }
}
