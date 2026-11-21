package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.coolfreeze23.vbparity.ai.AiParity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Engine order in LivingEntity.aiStep for the mobs VanillaBackport adds (vanilla mobs keep 1.21.1):
 * <ul>
 * <li>AI-GHS-D1b: native applyInput (xxa/zza *= 0.98) runs before serverAiStep, so the move control's fresh input
 * reaches travel undecayed; 1.21.1 decays it after the AI, 2% slower.</li>
 * <li>AI-GHS-D2: natively no AI runs while a player controls the happy ghast (isEffectiveAi is false for a
 * client-authoritative vehicle); 1.21.1 kept ticking goals, move and look control on the server.</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAiOrderMixin extends Entity {
    protected LivingEntityAiOrderMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Unique
    private boolean vbp$nativeInputOrder() {
        return !this.level().isClientSide() && AiParity.isVbAdded(this);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isImmobile()Z"))
    private void vbp$applyInputFirst(CallbackInfo ci) {
        if (this.vbp$nativeInputOrder()) {
            LivingEntity self = (LivingEntity) (Object) this;
            self.xxa *= 0.98F;
            self.zza *= 0.98F;
        }
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(floatValue = 0.98F))
    private float vbp$noLateInputDecay(float decay) {
        return this.vbp$nativeInputOrder() ? 1.0F : decay;
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;serverAiStep()V"))
    private void vbp$noAiWhileSteered(LivingEntity self, Operation<Void> original) {
        if (self instanceof HappyGhast && !self.level().isClientSide() && self.getControllingPassenger() instanceof Player) {
            return;
        }
        original.call(self);
    }
}
