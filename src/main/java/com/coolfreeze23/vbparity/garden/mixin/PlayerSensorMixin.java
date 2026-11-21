package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.coolfreeze23.vbparity.garden.GardenCreaking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.PlayerSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** GARDEN-02: the creaking's player sensor uses its 32-block follow range, as native. Other mobs are untouched. */
@Mixin(PlayerSensor.class)
public abstract class PlayerSensorMixin {
    @Inject(method = "doTick", at = @At("HEAD"), cancellable = true)
    private void vbparity$garden$creakingFollowRange(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof Creaking creaking) {
            GardenCreaking.tickPlayerSensor(level, creaking);
            ci.cancel();
        }
    }
}
