package com.coolfreeze23.vbparity.flora.mixin;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Predicate;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FLORA-14: native sheep eat everything in #edible_for_sheep: short grass, short and tall dry grass, fern. */
@Mixin(EatBlockGoal.class)
public abstract class EatBlockGoalMixin {
    @WrapOperation(method = {"canUse", "tick"}, at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    private boolean vbparity$nativeEdible(Predicate<Object> predicate, Object state, Operation<Boolean> original) {
        if (original.call(predicate, state)) {
            return true;
        }
        BlockState s = (BlockState) state;
        return s.is(Blocks.FERN) || s.is(ModBlocks.SHORT_DRY_GRASS.get()) || s.is(ModBlocks.TALL_DRY_GRASS.get());
    }
}
