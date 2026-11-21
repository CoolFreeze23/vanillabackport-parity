package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.client.registries.ModSoundEvents;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.core.data.tags.ModItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GHAST-03: putting a harness on a happy ghast behaves like equipping an equippable item in the current game:
 * the equip sound at full volume (unless the ghast is silent) for every equip path, and one EQUIP game event.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipMixin extends Entity {
    protected LivingEntityEquipMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow
    protected abstract boolean doesEmitEquipEvent(EquipmentSlot slot);

    @Inject(method = "onEquipItem", at = @At("HEAD"), cancellable = true)
    private void vbp$harnessEquip(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, CallbackInfo ci) {
        if (!((Object) this instanceof HappyGhast) || slot != EquipmentSlot.CHEST || !newItem.is(ModItemTags.HARNESSES)) {
            return;
        }

        ci.cancel();
        if (!ItemStack.isSameItemSameComponents(oldItem, newItem) && !this.firstTick && !this.level().isClientSide() && !this.isSpectator()) {
            if (!this.isSilent()) {
                this.level().playSeededSound(
                    null, this.getX(), this.getY(), this.getZ(), ModSoundEvents.HARNESS_EQUIP.get(), this.getSoundSource(), 1.0F, 1.0F, this.random.nextLong()
                );
            }

            if (this.doesEmitEquipEvent(slot)) {
                this.gameEvent(GameEvent.EQUIP);
            }
        }
    }
}
