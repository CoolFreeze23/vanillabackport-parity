package com.coolfreeze23.vbparity.sulfur.mixin;

import com.coolfreeze23.vbparity.sulfur.KnockbackEffect;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AI-SLF-01, stabs (Player): native stabAttack pushes with causeExtraKnockback(target, 0.4, ..., false) and then
 * causeExtraKnockback(target, getKnockback, ..., true). Applied after VB's spear mixin (priority) so its merged
 * vb$stabAttack exists.
 */
@Mixin(value = Player.class, priority = 1100)
public abstract class StabKnockbackEffectPlayerMixin {
    @WrapOperation(method = "vb$stabAttack", remap = false,
        at = @At(value = "INVOKE", target = "vb$causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;)V", ordinal = 0))
    private void vbp$jabIsNotExtra(Player self, Entity target, float knockback, Vec3 oldMovement, Operation<Void> original) {
        KnockbackEffect.run(false, () -> original.call(self, target, knockback, oldMovement));
    }

    @WrapOperation(method = "vb$stabAttack", remap = false,
        at = @At(value = "INVOKE", target = "vb$causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;)V", ordinal = 1))
    private void vbp$secondPushIsExtra(Player self, Entity target, float knockback, Vec3 oldMovement, Operation<Void> original) {
        KnockbackEffect.run(true, () -> original.call(self, target, knockback, oldMovement));
    }
}
