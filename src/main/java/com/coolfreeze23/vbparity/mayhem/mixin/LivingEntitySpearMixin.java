package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.mayhem.MayhemParity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mob stab attacks (VB's merged LivingEntity.vb$stabAttack, so this applies after VB):
 * MAYHEM-02 the damage source comes from the weapon's damage_type (spears: minecraft:spear, in #no_knockback),
 * MAYHEM-04 a successful stab plays the mob's attack sound, as native LivingEntity.stabAttack does.
 */
@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class LivingEntitySpearMixin extends Entity {
    protected LivingEntitySpearMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(
        method = "vb$stabAttack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;mobAttack(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/damagesource/DamageSource;"),
        remap = false
    )
    private DamageSource vbp$weaponDamageSource(DamageSource original, @Local(argsOnly = true) EquipmentSlot weaponSlot) {
        LivingEntity self = (LivingEntity) (Object) this;
        return MayhemParity.weaponDamageSource(self.getItemBySlot(weaponSlot), self, original);
    }

    @Inject(
        method = "vb$stabAttack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setLastHurtMob(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER),
        remap = false
    )
    private void vbp$playAttackSound(EquipmentSlot weaponSlot, Entity target, float baseDamage, boolean dealsDamage, boolean dealsKnockback, boolean dismounts,
                                     CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Mob mob) {
            ((MobAccessor) mob).vbp$playAttackSound();
        }
    }

    /**
     * Natively every swing with a STAB item is a stab (LivingEntity.getCurrentSwingDuration and the arm animations
     * read the item's swing_animation for any swing). VB only did so for attack swings and fell back to the 6-tick
     * whack after any right-click; every swing now counts.
     */
    @Inject(method = "vb$isAttackSwing", at = @At("HEAD"), cancellable = true, remap = false)
    private void vbp$everySwingUsesItemAnimation(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    /**
     * TPHYS-C02: native getKnockback returns the attack knockback halved, and stabAttack uses it raw. The 1.21.1
     * getKnockback is unhalved (its own callers halve), so VB's second stab knockback was twice native.
     */
    @ModifyExpressionValue(method = "vb$stabAttack",
        at = @At(value = "INVOKE", target = "getKnockback(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F"), remap = false)
    private float vbp$halfKnockback(float knockback) {
        return knockback / 2.0F;
    }
}
