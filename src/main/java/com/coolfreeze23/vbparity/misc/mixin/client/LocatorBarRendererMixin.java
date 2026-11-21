package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.waypoints.ClientWaypointManager;
import com.blackgear.vanillabackport.client.api.modules.waypoints.LocatorBarRenderer;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.misc.client.LocatorBarAccess;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Native Hud.nextContextualInfoState with waypoints: jump bar if riding and the jump has priority, else the XP bar
 * if it has priority (also while riding), else the locator bar.
 */
@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin implements LocatorBarAccess {
    @Shadow
    @Final
    private Minecraft minecraft;

    @org.spongepowered.asm.mixin.Unique
    private boolean vbp$xpPriority() {
        return ((LocatorBarRendererInvoker) (Object) this).vbp$willPrioritizeExperienceInfo();
    }

    @org.spongepowered.asm.mixin.Unique
    private boolean vbp$jumpPriority() {
        return ((LocatorBarRendererInvoker) (Object) this).vbp$willPrioritizeJumpInfo();
    }

    @Inject(method = "shouldSkipRendering", at = @At("HEAD"), cancellable = true)
    private void vbp$nativeContextualBar(CallbackInfoReturnable<Boolean> cir) {
        if (!VanillaBackport.CLIENT_CONFIG.enableLocatorBar.get() || !ClientWaypointManager.INSTANCE.hasWaypoints()) {
            cir.setReturnValue(true);
            return;
        }
        boolean jump = this.minecraft.player.jumpableVehicle() != null && this.vbp$jumpPriority();
        boolean experience = this.minecraft.gameMode.hasExperience() && this.vbp$xpPriority();
        cir.setReturnValue(jump || experience);
    }

    @Override
    public boolean vbp$showExperienceWhileRiding() {
        return this.minecraft.player != null
                && this.minecraft.gameMode != null
                && this.minecraft.player.jumpableVehicle() != null
                && VanillaBackport.CLIENT_CONFIG.enableLocatorBar.get()
                && ClientWaypointManager.INSTANCE.hasWaypoints()
                && !this.vbp$jumpPriority()
                && this.minecraft.gameMode.hasExperience()
                && this.vbp$xpPriority();
    }
}
