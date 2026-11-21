package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.mayhem.MayhemParity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Player stab attacks (VB's merged Player.vb$stabAttack / vb$attackVisualEffects, so this applies after VB):
 * MAYHEM-02 the damage source comes from the weapon's damage_type (native createAttackSource(weaponItem)), also for
 * the ordinary Player.attack path; MAYHEM-03 stabs play no PLAYER_ATTACK_STRONG/WEAK sound (native skips it when
 * stabAttack is true).
 */
@Mixin(value = Player.class, priority = 1500)
public abstract class PlayerSpearMixin extends LivingEntity {
    protected PlayerSpearMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(
        method = "vb$stabAttack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;playerAttack(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/damagesource/DamageSource;"),
        remap = false
    )
    private DamageSource vbp$stabDamageSource(DamageSource original, @Local(argsOnly = true) EquipmentSlot weaponSlot) {
        return MayhemParity.weaponDamageSource(this.getItemBySlot(weaponSlot), this, original);
    }

    @ModifyExpressionValue(
        method = "attack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;playerAttack(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/damagesource/DamageSource;")
    )
    private DamageSource vbp$attackDamageSource(DamageSource original) {
        return MayhemParity.weaponDamageSource(this.getWeaponItem(), this, original);
    }

    @WrapWithCondition(
        method = "vb$attackVisualEffects",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"),
        remap = false
    )
    private boolean vbp$noStabAttackSound(Level level, Player except, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch) {
        return false;
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

    /** TPHYS-C08: native passes the hit entity (an ender dragon part stays the part) to the post-attack effects. */
    @ModifyArg(method = "vb$itemAttackInteraction",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostAttackEffectsWithItemSource(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;)V"),
        index = 1, remap = false)
    private Entity vbp$postAttackVictimIsHitEntity(Entity hurtTarget, @Local(argsOnly = true, ordinal = 0) Entity target) {
        return target;
    }
}
