package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.level.blocks.StrawBedBlock;
import com.coolfreeze23.vbparity.wild.WildRegistry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Either;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * WILD-15. Native ServerPlayer.startSleepInBed: on success award bedBlock.getSleptInBedStatType() (straw bed:
 * Stats.SLEEP_IN_STRAW_BED) and trigger SLEPT_IN_BED ("Sweet Dreams") only when the bed rule can set spawn, which the
 * straw bed rule (DESTROY_ON_LEAVE, can_set_spawn NEVER) cannot. For a straw bed the success consumer is swapped.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(
        method = "startSleepInBed",
        at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Either;ifRight(Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;")
    )
    private Either vbparity$wild$strawBedStat(Either result, Consumer onSlept, Operation<Either> original, BlockPos at) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.level().getBlockState(at).getBlock() instanceof StrawBedBlock) {
            Consumer strawBed = unit -> self.awardStat(WildRegistry.SLEEP_IN_STRAW_BED);
            return original.call(result, strawBed);
        }
        return original.call(result, onSlept);
    }
}
