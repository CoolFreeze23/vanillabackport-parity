package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.registries.blocks.ModPoiTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * COPPER-08: native findLightningRod searches every lightning rod. VB's cancelling HEAD inject only searches its own
 * seven rods, so the plain rod stopped attracting lightning. The call site is wrapped instead of racing VB's inject,
 * so the result does not depend on mixin order.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelLightningRodMixin {
    @WrapOperation(
        method = "findLightningTargetAround",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;findLightningRod(Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;")
    )
    private Optional<BlockPos> vbparity$findAnyLightningRod(ServerLevel level, BlockPos center, Operation<Optional<BlockPos>> original) {
        return level.getPoiManager()
            .findClosest(
                type -> type.is(PoiTypes.LIGHTNING_ROD) || type.is(ModPoiTypes.LIGHTNING_RODS),
                rodPos -> rodPos.getY() == level.getHeight(Heightmap.Types.WORLD_SURFACE, rodPos.getX(), rodPos.getZ()) - 1,
                center,
                128,
                PoiManager.Occupancy.ANY
            )
            .map(rodPos -> rodPos.above(1));
    }
}
