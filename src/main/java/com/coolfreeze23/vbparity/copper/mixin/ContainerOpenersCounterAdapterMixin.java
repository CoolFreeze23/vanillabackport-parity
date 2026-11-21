package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.container.ContainerOpenersCounterAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import com.coolfreeze23.vbparity.copper.CopperChests;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** COPPER-03: a container opened by a golem schedules the 5-tick opener recheck, as native incrementOpeners does. */
@Mixin(ContainerOpenersCounterAdapter.class)
public abstract class ContainerOpenersCounterAdapterMixin {
    @Inject(method = "incrementOpeners", at = @At("TAIL"))
    private static void vbparity$scheduleRecheck(
        ContainerOpenersCounter counter, LivingEntity entity, Level level, BlockPos pos, BlockState state, double maxInteractionRange, CallbackInfo ci
    ) {
        // Native schedules when previous == 0, i.e. this call took the count from 0 to 1.
        if (counter.getOpenerCount() == 1) {
            level.scheduleTick(pos, state.getBlock(), 5);
        }
    }

    /**
     * AI-CPG-01: VB opened and closed the block at ChestBlock.getConnectedDirection even for a single chest (its
     * counter-clockwise neighbour). Natively the golem opens a CompoundContainer only for a real double chest.
     */
    @WrapOperation(
        method = "handleDoubleChest",
        at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/level/block/entity/ChestBlockEntity", ordinal = 0)
    )
    private boolean vbparity$onlyRealDoubleChests(Object target, Operation<Boolean> original, @Local(argsOnly = true) BlockEntity blockEntity, @Local(argsOnly = true) Level level) {
        return original.call(target) && CopperChests.isCombinedDouble(blockEntity, level);
    }
}
