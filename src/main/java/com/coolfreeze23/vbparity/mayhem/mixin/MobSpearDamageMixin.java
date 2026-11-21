package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.mayhem.MayhemParity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** MAYHEM-02: mob melee (Mob.doHurtTarget) uses the weapon's damage type, as native weaponItem.getDamageSource(this). */
@Mixin(Mob.class)
public abstract class MobSpearDamageMixin {
    @ModifyExpressionValue(
        method = "doHurtTarget",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;mobAttack(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/damagesource/DamageSource;")
    )
    private DamageSource vbp$weaponDamageSource(DamageSource original) {
        Mob self = (Mob) (Object) this;
        return MayhemParity.weaponDamageSource(self.getWeaponItem(), self, original);
    }
}
