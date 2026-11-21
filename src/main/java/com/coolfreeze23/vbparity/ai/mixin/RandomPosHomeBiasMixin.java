package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.AiParity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.RandomPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-GHS-D3 / AI-CPG-D2: native RandomPos.generateRandomPosTowardDirection pulls a restricted mob toward home by
 * nextDouble() * range / 2 per axis (continuous); 1.21.1 uses nextInt(range / 2). Native for the mobs VB adds only.
 */
@Mixin(RandomPos.class)
public abstract class RandomPosHomeBiasMixin {
    @Inject(method = "generateRandomPosTowardDirection", at = @At("HEAD"), cancellable = true)
    private static void vbp$continuousHomeBias(PathfinderMob mob, int range, RandomSource random, BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
        if (!AiParity.isVbAdded(mob)) {
            return;
        }
        double xt = pos.getX();
        double zt = pos.getZ();
        if (mob.hasRestriction() && range > 1.0) {
            BlockPos center = mob.getRestrictCenter();
            if (mob.getX() > center.getX()) {
                xt -= random.nextDouble() * range / 2.0;
            } else {
                xt += random.nextDouble() * range / 2.0;
            }
            if (mob.getZ() > center.getZ()) {
                zt -= random.nextDouble() * range / 2.0;
            } else {
                zt += random.nextDouble() * range / 2.0;
            }
        }
        cir.setReturnValue(BlockPos.containing(xt + mob.getX(), pos.getY() + mob.getY(), zt + mob.getZ()));
    }
}
