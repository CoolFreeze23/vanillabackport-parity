package com.coolfreeze23.vbparity.flora.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrassBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * FLORA-13: native bone meal on grass picks a random bone-meal flower feature of the biome ({@code Util.getRandom});
 * 1.21.1 always takes the first one, so VB's wildflowers never come from bone meal.
 */
@Mixin(GrassBlock.class)
public abstract class GrassBlockMixin {
    @WrapOperation(method = "performBonemeal", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
    private Object vbparity$randomFlower(List<?> list, int index, Operation<Object> original, @Local(argsOnly = true) RandomSource random) {
        return original.call(list, random.nextInt(list.size()));
    }
}
