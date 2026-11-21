package com.coolfreeze23.vbparity.flora.mixin;

import com.blackgear.vanillabackport.common.level.worldgen.features.FallenTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the GameTests run the fallen-log ground check directly. */
@Mixin(FallenTreeFeature.class)
public interface FallenTreeFeatureInvoker {
    @Invoker("canPlaceEntireFallenLog")
    boolean vbparity$canPlaceEntireFallenLog(WorldGenLevel level, int length, BlockPos.MutableBlockPos pos, Direction direction);
}
