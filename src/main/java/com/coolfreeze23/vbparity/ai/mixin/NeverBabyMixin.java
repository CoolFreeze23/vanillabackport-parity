package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilus;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-MNT-09: native camel husks and zombie nautiluses have canBeABaby() false: isBaby() is always false and Age /
 * ForcedAge are neither saved nor loaded. 1.21.1 kept reading Age, so {Age:-24000} made a baby.
 */
@Mixin(AgeableMob.class)
public abstract class NeverBabyMixin {
    @Unique
    private boolean vbp$cannotBeBaby() {
        return (Object) this instanceof CamelHusk || (Object) this instanceof ZombieNautilus;
    }

    @Inject(method = "isBaby", at = @At("HEAD"), cancellable = true)
    private void vbp$neverBaby(CallbackInfoReturnable<Boolean> cir) {
        if (this.vbp$cannotBeBaby()) {
            cir.setReturnValue(false);
        }
    }

    @WrapWithCondition(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;setAge(I)V"))
    private boolean vbp$noAgeLoad(AgeableMob self, int age) {
        return !this.vbp$cannotBeBaby();
    }

    @WrapOperation(method = "readAdditionalSaveData", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/AgeableMob;forcedAge:I", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void vbp$noForcedAgeLoad(AgeableMob self, int value, Operation<Void> original) {
        if (!this.vbp$cannotBeBaby()) {
            original.call(self, value);
        }
    }

    @WrapWithCondition(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putInt(Ljava/lang/String;I)V"))
    private boolean vbp$noAgeSave(CompoundTag tag, String key, int value) {
        return !this.vbp$cannotBeBaby();
    }
}
