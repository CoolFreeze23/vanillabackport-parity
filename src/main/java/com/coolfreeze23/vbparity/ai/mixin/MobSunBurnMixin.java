package com.coolfreeze23.vbparity.ai.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AI-MNT-06: native Mob.aiStep burns #burn_in_daylight mobs right after LivingEntity.aiStep, before the horse's own
 * heal and eat-grass rolls; the zombie horse protects itself with its BODY slot (sunProtectionSlot). Same body as VB's.
 */
@Mixin(Mob.class)
public abstract class MobSunBurnMixin extends LivingEntity {
    protected MobSunBurnMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Shadow
    protected abstract boolean isSunBurnTick();

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;aiStep()V", shift = At.Shift.AFTER))
    private void vbp$zombieHorseBurn(CallbackInfo ci) {
        if (!((Object) this instanceof ZombieHorse)) {
            return;
        }
        if (this.isAlive() && this.isSunBurnTick()) {
            EquipmentSlot slot = EquipmentSlot.BODY;
            ItemStack armor = this.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                if (armor.isDamageableItem()) {
                    armor.setDamageValue(armor.getDamageValue() + this.random.nextInt(2));
                    if (armor.getDamageValue() >= armor.getMaxDamage()) {
                        this.onEquippedItemBroken(armor.getItem(), slot);
                        this.setItemSlot(slot, ItemStack.EMPTY);
                    }
                }
            } else {
                this.igniteForSeconds(8.0F);
            }
        }
    }
}
