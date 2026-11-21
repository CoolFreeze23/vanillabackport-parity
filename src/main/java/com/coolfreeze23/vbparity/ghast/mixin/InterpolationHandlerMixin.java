package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.InterpolationHandler;
import java.lang.reflect.Field;
import java.util.Objects;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GHAST-41: like the current game's InterpolationHandler, a target that matches the interpolation already running
 * (same position and rotation) does not restart it. VB's InterpolationData is a package-private nested class, so its
 * fields are read reflectively (once resolved, cached).
 */
@Mixin(InterpolationHandler.class)
public abstract class InterpolationHandlerMixin {
    @Shadow
    @Final
    private int interpolationSteps;

    @Shadow
    public abstract boolean hasActiveInterpolation();

    @Unique
    private static Field vbp$data;
    @Unique
    private static Field vbp$position;
    @Unique
    private static Field vbp$yRot;
    @Unique
    private static Field vbp$xRot;
    @Unique
    private static boolean vbp$broken;

    @Inject(method = "interpolateTo", at = @At("HEAD"), cancellable = true)
    private void vbp$ignoreSameTarget(Vec3 position, float yRot, float xRot, CallbackInfo ci) {
        if (this.interpolationSteps == 0 || !this.hasActiveInterpolation() || vbp$broken) {
            return;
        }

        try {
            if (vbp$data == null) {
                vbp$data = InterpolationHandler.class.getDeclaredField("data");
                vbp$data.setAccessible(true);
                Class<?> type = vbp$data.getType();
                vbp$position = type.getDeclaredField("position");
                vbp$yRot = type.getDeclaredField("yRot");
                vbp$xRot = type.getDeclaredField("xRot");
                vbp$position.setAccessible(true);
                vbp$yRot.setAccessible(true);
                vbp$xRot.setAccessible(true);
            }

            Object data = vbp$data.get(this);
            if (Objects.equals(vbp$yRot.getFloat(data), yRot) && Objects.equals(vbp$xRot.getFloat(data), xRot) && Objects.equals(vbp$position.get(data), position)) {
                ci.cancel();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            vbp$broken = true;
        }
    }
}
