package com.coolfreeze23.vbparity.mayhem.mixin;

import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * MAYHEM-25: native baby zombie horse dimensions: passenger point at height - 0.25 and scale 0.7 (1.21.1: - 0.03125, 0.5).
 * MAYHEM-27: native speed divisor is the float literal 42.16F (VB's merged vb$generateZombieHorseSpeed used 42.16).
 */
@Mixin(value = ZombieHorse.class, priority = 1500)
public abstract class ZombieHorseMixin {
    @ModifyConstant(method = "<clinit>", constant = @Constant(floatValue = 0.03125F))
    private static float vbp$babyPassengerOffset(float offset) {
        return 0.25F;
    }

    @ModifyArg(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityDimensions;scale(F)Lnet/minecraft/world/entity/EntityDimensions;")
    )
    private static float vbp$babyScale(float scale) {
        return 0.7F;
    }

    @ModifyConstant(method = "vb$generateZombieHorseSpeed", constant = @Constant(doubleValue = 42.16), remap = false)
    private static double vbp$floatDivisor(double divisor) {
        return (double) 42.16F;
    }

    /** SPAWN-10: the rider is rolled before the horse's own spawn draws (AbstractHorseRiderMixin), not after. */
    @Inject(method = "vb$finalizeSpawn", at = @At("HEAD"), cancellable = true, remap = false)
    private void vbp$riderRolledEarlier(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        CallbackInfoReturnable<SpawnGroupData> cir) {
        cir.setReturnValue(data);
    }
}
