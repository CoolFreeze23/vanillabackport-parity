package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.bundle_ui.BundleTooltipHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native ClientBundleTooltip: empty height = description lines * 9 + 13 + 8 (not a fixed 39, which assumes 2 lines). */
@Mixin(BundleTooltipHandler.class)
public abstract class BundleTooltipHandlerMixin {
    @Unique
    private static final Component VBP_EMPTY_DESCRIPTION = Component.translatable("item.minecraft.bundle.empty.description");

    @Shadow
    @Final
    private BundleContents contents;

    @Shadow
    public abstract int getWidth();

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void vbp$emptyHeight(CallbackInfoReturnable<Integer> cir) {
        if (this.contents.isEmpty()) {
            cir.setReturnValue(Minecraft.getInstance().font.split(VBP_EMPTY_DESCRIPTION, this.getWidth()).size() * 9 + 13 + 8);
        }
    }
}
