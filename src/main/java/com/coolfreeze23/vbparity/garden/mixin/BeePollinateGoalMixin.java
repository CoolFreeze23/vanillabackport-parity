package com.coolfreeze23.vbparity.garden.mixin;

import com.coolfreeze23.vbparity.garden.GardenBees;
import java.util.function.Predicate;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** GARDEN-16: bees do not pick a closed eyeblossom as a pollination target. */
@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeePollinateGoal")
public abstract class BeePollinateGoalMixin {
    @ModifyArg(
        method = "findNearbyFlower",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bee$BeePollinateGoal;findNearestBlock(Ljava/util/function/Predicate;D)Ljava/util/Optional;"),
        index = 0
    )
    private Predicate<BlockState> vbparity$garden$skipClosedEyeblossom(Predicate<BlockState> predicate) {
        return state -> predicate.test(state) && !GardenBees.isClosedEyeblossom(state);
    }
}
