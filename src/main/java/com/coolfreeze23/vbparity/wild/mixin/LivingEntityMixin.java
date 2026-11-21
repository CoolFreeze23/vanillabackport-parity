package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.level.blocks.StrawBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WILD-16. Native LivingEntity.setPosToBed puts the sleeper at bed.y + sleepHeight + 0.125, where sleepHeight is the max Y
 * of the foot part's shape (straw bed: 4/16, for both parts), so 0.375. 1.21.1 uses 0.6875 (the regular bed's 9/16 +
 * 0.125) for every bed.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "setPosToBed", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$strawBedSleepHeight(BlockPos pos, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().getBlockState(pos).getBlock() instanceof StrawBedBlock) {
            self.setPos(pos.getX() + 0.5, pos.getY() + 0.25 + 0.125, pos.getZ() + 0.5);
            ci.cancel();
        }
    }
}
