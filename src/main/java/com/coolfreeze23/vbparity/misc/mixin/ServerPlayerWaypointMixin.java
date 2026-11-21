package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.api.modules.waypoints.Waypoint;
import com.blackgear.vanillabackport.common.api.modules.waypoints.WaypointTransmitter;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native ServerPlayer.restoreFrom: {@code this.waypointIcon().copyFrom(oldPlayer.waypointIcon())} on every respawn. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerWaypointMixin {
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void vbp$copyWaypointIcon(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        Waypoint.Icon icon = ((WaypointTransmitter) this).waypointIcon();
        Waypoint.Icon old = ((WaypointTransmitter) oldPlayer).waypointIcon();
        icon.style = old.style;
        icon.color = old.color;
    }
}
