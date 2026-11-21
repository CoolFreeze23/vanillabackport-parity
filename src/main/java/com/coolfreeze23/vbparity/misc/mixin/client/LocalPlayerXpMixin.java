package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.waypoints.ExperienceDisplay;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native LocalPlayer: {@code experienceDisplayStartTick = Integer.MIN_VALUE} ("not ready to set") for a new player. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerXpMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void vbp$experienceDisplayUnready(CallbackInfo ci) {
        ExperienceDisplay.of((LocalPlayer) (Object) this).setExperienceDisplayStartTick(Integer.MIN_VALUE);
    }
}
