package com.coolfreeze23.vbparity.ai.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * AI-VAR-06: native cat and wolf offspring get the mixed collar colour with no draw of the parent's random; 1.21.1's
 * 50/50 collar pick (which VB then overwrites with the mixed colour) drew one nextBoolean. That draw is removed.
 */
@Mixin({Cat.class, Wolf.class})
public abstract class BreedCollarDrawMixin {
    @Redirect(method = "getBreedOffspring",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextBoolean()Z", ordinal = 1))
    private boolean vbp$noCollarDraw(RandomSource random) {
        return true;
    }
}
