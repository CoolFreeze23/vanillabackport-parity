package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.waypoints.LocatorBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocatorBarRenderer.class)
public interface LocatorBarRendererInvoker {
    @Invoker("willPrioritizeExperienceInfo")
    boolean vbp$willPrioritizeExperienceInfo();

    @Invoker("willPrioritizeJumpInfo")
    boolean vbp$willPrioritizeJumpInfo();
}
