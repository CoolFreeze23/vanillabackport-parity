package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AI-MNT-08: native nautilus water movement (travelInWater) only runs where the nautilus is simulated, and every tick
 * still updates the walk animation. VB's travel override ran water physics on every client copy and never animated.
 * Non-local copies now fall through to LivingEntity.travel, which only animates; the water branch animates too.
 */
@Mixin(AbstractNautilus.class)
public abstract class NautilusTravelMixin extends TamableAnimal {
    protected NautilusTravelMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "isInWater()Z"))
    private boolean vbp$onlyWhereSimulated(boolean inWater) {
        return inWater && this.isControlledByLocalInstance();
    }

    @Inject(method = "travel", at = @At(value = "INVOKE", target = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void vbp$animateInWater(Vec3 input, CallbackInfo ci) {
        this.calculateEntityAnimation(false);
    }
}
