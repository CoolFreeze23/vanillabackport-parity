package com.coolfreeze23.vbparity.misc.mixin;

import com.coolfreeze23.vbparity.misc.BoatFallLogic;
import com.coolfreeze23.vbparity.misc.MiscContent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Native AbstractBoat: boats never break from falls, and entities in {@code #cannot_be_pushed_onto_boats}
 * (elder guardian, creaking, ...) are pushed instead of being picked up.
 */
@Mixin(Boat.class)
public abstract class BoatMixin {
    @Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true)
    private void vbp$noFallBreak(double y, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        Boat self = (Boat) (Object) this;
        BoatFallLogic.checkFallDamage(self, y, onGround, self.canBoatInFluid(self.level().getFluidState(self.blockPosition().below())));
        ci.cancel();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean vbp$cannotBePushedOntoBoats(Entity entity, Entity vehicle, Operation<Boolean> original) {
        if (entity.getType().is(MiscContent.CANNOT_BE_PUSHED_ONTO_BOATS)) {
            vehicle.push(entity);
            return false;
        }
        return original.call(entity, vehicle);
    }
}
