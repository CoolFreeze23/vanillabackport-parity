package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.api.extensions.SoundExtensions;
import com.blackgear.vanillabackport.common.level.components.PiercingWeapon;
import net.minecraft.world.entity.Entity;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MAYHEM-05: native PiercingWeapon.makeSound plays the jab sound with the causer as the excluded entity, so the
 * attacking player hears it at once from the client call and the server copy skips them. VB passed null.
 */
@Mixin(value = PiercingWeapon.class, remap = false)
public abstract class PiercingWeaponMixin {
    @Inject(method = "makeSound", at = @At("HEAD"), cancellable = true)
    private void vbp$predictedSound(Entity causer, CallbackInfo ci) {
        ((PiercingWeapon) (Object) this).sound().ifPresent(sound -> ((SoundExtensions) causer.level())
            .playSound(causer, causer.getX(), causer.getY(), causer.getZ(), sound, causer.getSoundSource(), 1.0F, 1.0F));
        ci.cancel();
    }

    /** TPHYS-C07: native isInvulnerableToPiercingWeapon; a primed sulfur cube can be hit even when invulnerable. */
    @WrapOperation(method = "canHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInvulnerable()Z"))
    private static boolean vbp$invulnerableToPiercing(Entity target, Operation<Boolean> original) {
        return original.call(target) && !(target instanceof SulfurCube cube && cube.isPrimed());
    }
}
