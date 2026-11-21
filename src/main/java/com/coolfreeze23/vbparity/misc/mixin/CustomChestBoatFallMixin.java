package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.level.entities.boat.CustomChestBoat;
import com.coolfreeze23.vbparity.misc.BoatFallLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** VB's pale oak / poplar boats copy the 1.21.1 fall-break code; native boats never break from falls. */
@Mixin(CustomChestBoat.class)
public abstract class CustomChestBoatFallMixin {
    @Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true)
    private void vbp$noFallBreak(double y, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        CustomChestBoat self = (CustomChestBoat) (Object) this;
        BoatFallLogic.checkFallDamage(self, y, onGround, self.level().getFluidState(self.blockPosition().below()).is(FluidTags.WATER));
        ci.cancel();
    }
}
