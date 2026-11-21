package com.coolfreeze23.vbparity.garden.mixin;

import com.coolfreeze23.vbparity.garden.GardenBees;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Predicate;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GARDEN-16: a closed eyeblossom is not bee food, does not tempt bees and is not a pollination flower. */
@Mixin(Bee.class)
public abstract class BeeMixin {
    @Inject(method = "isFood", at = @At("HEAD"), cancellable = true)
    private void vbparity$garden$closedEyeblossomNotFood(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (GardenBees.isClosedEyeblossom(stack)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyArg(
        method = "registerGoals",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/TemptGoal;<init>(Lnet/minecraft/world/entity/PathfinderMob;DLjava/util/function/Predicate;Z)V"),
        index = 2
    )
    private Predicate<ItemStack> vbparity$garden$temptNotClosedEyeblossom(Predicate<ItemStack> items) {
        return stack -> items.test(stack) && !GardenBees.isClosedEyeblossom(stack);
    }

    @WrapOperation(method = "isFlowerValid", at = @At(value = "INVOKE", target = "is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean vbparity$garden$flowerNotClosedEyeblossom(BlockState state, TagKey<Block> tag, Operation<Boolean> original) {
        return original.call(state, tag) && !GardenBees.isClosedEyeblossom(state);
    }
}
