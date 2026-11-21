package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.waypoints.ExperienceDisplay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native LocalPlayer.setExperienceValues starts the 100-tick XP display only when the progress changes, and the
 * first change after joining/respawning only arms it (-2147483647). VB sets it on every XP packet.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerXpMixin {
    @WrapOperation(method = "handleSetExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setExperienceValues(FII)V"))
    private void vbp$nativeExperienceDisplayTick(LocalPlayer player, float progress, int total, int level, Operation<Void> original) {
        ExperienceDisplay display = ExperienceDisplay.of(player);
        int previousTick = display.getExperienceDisplayStartTick();
        boolean changed = progress != player.experienceProgress;
        original.call(player, progress, total, level);
        if (!changed) {
            display.setExperienceDisplayStartTick(previousTick);
        } else {
            display.setExperienceDisplayStartTick(previousTick == Integer.MIN_VALUE ? -2147483647 : player.tickCount);
        }
    }
}
