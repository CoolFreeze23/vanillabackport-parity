package com.coolfreeze23.vbparity.sulfur.mixin;

import com.coolfreeze23.vbparity.sulfur.SulfurBounce;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TPHYS-A02. Native LivingEntity.aiStep applies block effects (stepOn) after travel, on the velocity left after the
 * bounce. For the entities whose restitution is native (EntityBounceMixin skips stepOn inside move for them) the
 * stepOn call happens here, right after travel, with move's arguments (on-ground, getOnPosLegacy()).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityStepOnMixin extends net.minecraft.world.entity.Entity {
    private LivingEntityStepOnMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "aiStep", at = @At(value = "CONSTANT", args = "stringValue=freezing"))
    private void vbp$stepOnAfterTravel(CallbackInfo ci) {
        if (this.onGround() && SulfurBounce.nativeRestitution(this) && (!this.level().isClientSide() || this.isControlledByLocalInstance())) {
            BlockPos pos = this.getOnPosLegacy();
            BlockState state = this.level().getBlockState(pos);
            state.getBlock().stepOn(this.level(), pos, state, this);
        }
    }
}
