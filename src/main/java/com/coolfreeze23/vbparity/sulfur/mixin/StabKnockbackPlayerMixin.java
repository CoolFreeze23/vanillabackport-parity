package com.coolfreeze23.vbparity.sulfur.mixin;

import com.coolfreeze23.vbparity.sulfur.StabKnockbackTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TPHYS-C05, player side. Native Player stab: causeExtraKnockback(target, 0.4, …, damageSource, totalDamage) with
 * totalDamage 0 when the stab deals no damage, so a block-holding sulfur cube gets the archetype push with sqrt(0)
 * (none). VB's merged vb$stabAttack skips hurt then, which leaves the cube without a pending source (plain 0.4 push).
 * Applied after VB's spear mixin (priority) so its merged method exists.
 */
@Mixin(value = Player.class, priority = 1100)
public abstract class StabKnockbackPlayerMixin {
    @Inject(method = "vb$stabAttack", at = @At("HEAD"), remap = false)
    private void vbp$knockbackOnlyStab(
        EquipmentSlot weaponSlot, Entity target, float baseDamage, boolean dealsDamage, boolean dealsKnockback, boolean dismounts,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (dealsKnockback && !dealsDamage && target instanceof StabKnockbackTarget cube) {
            Player self = (Player) (Object) this;
            cube.vbparity$prepareKnockback(self.damageSources().playerAttack(self), 0.0F);
        }
    }
}
