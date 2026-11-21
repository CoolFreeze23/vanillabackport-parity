package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.waypoints.LocatorBarRenderer;
import com.coolfreeze23.vbparity.misc.client.LocatorBarAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.PlayerRideableJumping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While riding with waypoints shown, native shows the XP bar (not the jump bar) for 100 ticks after an XP change
 * when the jump has no priority. 1.21.1 always draws the jump meter while riding; treat the vehicle as absent for
 * the jump meter / XP bar choice in that state.
 */
@Mixin(Gui.class)
public abstract class GuiContextualBarMixin {
    @ModifyExpressionValue(method = {"maybeRenderJumpMeter", "maybeRenderExperienceBar", "isExperienceBarVisible"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;jumpableVehicle()Lnet/minecraft/world/entity/PlayerRideableJumping;"))
    private PlayerRideableJumping vbp$experienceWhileRiding(PlayerRideableJumping original) {
        if (original != null && ((LocatorBarAccess) LocatorBarRenderer.INSTANCE).vbp$showExperienceWhileRiding()) {
            return null;
        }
        return original;
    }
}
