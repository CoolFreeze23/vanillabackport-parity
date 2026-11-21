package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.level.blocks.StrawBedBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StrawBedBlock.class)
public abstract class StrawBedBlockMixin extends HorizontalDirectionalBlock {
    private StrawBedBlockMixin(Properties properties) {
        super(properties);
    }

    /**
     * WILD-15. VB awards Stats.SLEEP_IN_BED a second time on top of ServerPlayer.startSleepInBed's own award. Native
     * awards only the bed's stat once, inside startSleepInBed (see ServerPlayerMixin), so VB's extra award is dropped.
     */
    @WrapOperation(
        method = "useWithoutItem",
        at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Either;ifRight(Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;")
    )
    private Either<?, ?> vbparity$wild$noExtraBedStat(Either<?, ?> result, Consumer<?> awardStat, Operation<Either<?, ?>> original) {
        return result;
    }

    /** WILD-18. Native AbstractBedBlock.isPathfindable returns false (1.21.1 default: true for non-full collision shapes). */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    /**
     * WILD-22. Native LivingEntity.stopSleeping removes the straw bed (onStopSleeping) before it searches for the
     * stand-up spot, so the spot above the bed is free. VB's stopSleeping handler searches first and removes the bed
     * after. Its only call of findStandUpPosition is made with the bed still there and just set unoccupied, so the bed
     * is removed here, and VB's later onStopSleeping call (bed already gone) does nothing.
     */
    @Inject(method = "findStandUpPosition", at = @At("HEAD"))
    private static void vbparity$wild$removeBedBeforeStandUp(
        EntityType<?> type, CollisionGetter level, BlockPos pos, Direction forward, float yaw, CallbackInfoReturnable<Optional<Vec3>> cir
    ) {
        if (level instanceof Level realLevel && realLevel.getBlockState(pos).getBlock() instanceof StrawBedBlock bed) {
            bed.onStopSleeping(realLevel, pos);
        }
    }

    @Inject(method = "onStopSleeping", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$bedAlreadyRemoved(Level level, BlockPos pos, CallbackInfo ci) {
        if (!(level.getBlockState(pos).getBlock() instanceof StrawBedBlock)) {
            ci.cancel();
        }
    }
}
