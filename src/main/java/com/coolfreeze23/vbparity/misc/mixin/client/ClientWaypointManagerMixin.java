package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.waypoints.ClientWaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native has no XP-display restart while an enchanting table or anvil screen is open. */
@Mixin(ClientWaypointManager.class)
public abstract class ClientWaypointManagerMixin {
    @Inject(method = "lambda$bootstrap$0", at = @At("HEAD"), cancellable = true)
    private static void vbp$noContainerXpDisplay(Minecraft minecraft, AbstractContainerScreen<?> screen, CallbackInfo ci) {
        ci.cancel();
    }
}
