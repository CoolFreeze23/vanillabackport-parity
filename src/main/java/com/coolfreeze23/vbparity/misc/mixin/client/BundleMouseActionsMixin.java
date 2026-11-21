package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.bundle_ui.BundleMouseActions;
import com.blackgear.vanillabackport.client.api.modules.bundle_ui.ScrollWheelHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native BundleMouseActions/ScrollWheelHandler accumulate horizontal and vertical scroll separately and use
 * {@code wheel = y == 0 ? -x : y}, so a tilt wheel or horizontal trackpad scroll also moves the selection.
 * VB's scroll callback only forwards deltaY; this keeps deltaX for the same call.
 */
@Mixin(BundleMouseActions.class)
public abstract class BundleMouseActionsMixin {
    @Unique
    private static double vbp$pendingScrollX;
    @Unique
    private double vbp$accumulatedScrollX;
    @Unique
    private double vbp$accumulatedScrollY;

    @WrapOperation(method = "lambda$bootstrap$0", at = @At(value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/client/api/modules/bundle_ui/BundleMouseActions;onMouseScrolled(DILnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean vbp$keepScrollX(BundleMouseActions instance, double deltaY, int slot, ItemStack stack, Operation<Boolean> original,
                                           @Local(argsOnly = true, ordinal = 2) double deltaX) {
        vbp$pendingScrollX = deltaX;
        try {
            return original.call(instance, deltaY, slot, stack);
        } finally {
            vbp$pendingScrollX = 0.0;
        }
    }

    @WrapOperation(method = "onMouseScrolled", at = @At(value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/client/api/modules/bundle_ui/ScrollWheelHandler;onMouseScroll(D)Lorg/joml/Vector2i;"))
    private Vector2i vbp$twoAxisScroll(ScrollWheelHandler handler, double deltaY, Operation<Vector2i> original) {
        double deltaX = vbp$pendingScrollX;
        if (this.vbp$accumulatedScrollX != 0.0 && Math.signum(deltaX) != Math.signum(this.vbp$accumulatedScrollX)) {
            this.vbp$accumulatedScrollX = 0.0;
        }
        if (this.vbp$accumulatedScrollY != 0.0 && Math.signum(deltaY) != Math.signum(this.vbp$accumulatedScrollY)) {
            this.vbp$accumulatedScrollY = 0.0;
        }
        this.vbp$accumulatedScrollX += deltaX;
        this.vbp$accumulatedScrollY += deltaY;
        int wheelX = (int) this.vbp$accumulatedScrollX;
        int wheelY = (int) this.vbp$accumulatedScrollY;
        if (wheelX == 0 && wheelY == 0) {
            return new Vector2i(0, 0);
        }
        this.vbp$accumulatedScrollX -= wheelX;
        this.vbp$accumulatedScrollY -= wheelY;
        return new Vector2i(wheelX, wheelY);
    }
}
