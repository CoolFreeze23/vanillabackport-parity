package com.coolfreeze23.vbparity.wild.mixin;

import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrunkPlacer.class)
public interface TrunkPlacerAccessor {
    @Accessor("baseHeight")
    int vbparity$baseHeight();
}
