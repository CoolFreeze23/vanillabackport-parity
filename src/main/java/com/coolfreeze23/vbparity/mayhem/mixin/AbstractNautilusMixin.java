package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.client.registries.ModSoundEvents;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import com.blackgear.vanillabackport.common.level.items.NautilusArmorItem;
import com.coolfreeze23.vbparity.mayhem.MayhemParity;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nautilus and zombie nautilus (shared base class):
 * MAYHEM-16 dash and dash-ready sounds go through makeSound (voice pitch); MAYHEM-17 the dash emits ENTITY_ACTION;
 * MAYHEM-18 the saddle sound plays at volume 1, the armour sound only when the armour slot changes (and is the
 * nautilus armour sound at volume 1, like the native equippable); MAYHEM-19 body armour only on tame adults;
 * MAYHEM-20 any player can shear the armour, then the saddle, off an unridden nautilus.
 */
@Mixin(value = AbstractNautilus.class, priority = 1500)
public abstract class AbstractNautilusMixin extends TamableAnimal {
    @Shadow
    protected SimpleContainer inventory;

    @Shadow(remap = false)
    public abstract boolean isSaddled();

    protected AbstractNautilusMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Redirect(
        method = {"tick", "handleStartJump"},
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/nautilus/AbstractNautilus;playSound(Lnet/minecraft/sounds/SoundEvent;)V")
    )
    private void vbp$makeDashSound(AbstractNautilus self, @Nullable SoundEvent sound) {
        self.makeSound(sound);
    }

    @ModifyArg(
        method = "handleStartJump",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/nautilus/AbstractNautilus;gameEvent(Lnet/minecraft/core/Holder;)V")
    )
    private Holder<GameEvent> vbp$dashGameEvent(Holder<GameEvent> event) {
        return GameEvent.ENTITY_ACTION;
    }

    @ModifyArg(
        method = "containerChanged",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/nautilus/AbstractNautilus;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", ordinal = 0),
        index = 1
    )
    private float vbp$saddleVolume(float volume) {
        return 1.0F;
    }

    @WrapWithCondition(
        method = "containerChanged",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/nautilus/AbstractNautilus;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", ordinal = 1)
    )
    private boolean vbp$noArmourSoundOnInventoryChange(AbstractNautilus self, SoundEvent sound, float volume, float pitch) {
        return false;
    }

    /** Native equippable sound for nautilus armour: item.armor.equip_nautilus at (1, 1) when the armour slot changes. */
    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem) {
        if (slot == EquipmentSlot.BODY && newItem.getItem() instanceof NautilusArmorItem) {
            if (!ItemStack.isSameItemSameComponents(oldItem, newItem) && !this.firstTick && !this.level().isClientSide() && !this.isSpectator()) {
                if (!this.isSilent()) {
                    this.level().playSeededSound(null, this.getX(), this.getY(), this.getZ(), ModSoundEvents.ARMOR_EQUIP_NAUTILUS.get(),
                        this.getSoundSource(), 1.0F, 1.0F, this.random.nextLong());
                }
                if (this.doesEmitEquipEvent(slot)) {
                    this.gameEvent(GameEvent.EQUIP);
                }
            }
            return;
        }
        super.onEquipItem(slot, oldItem, newItem);
    }

    @Inject(method = "canUseSlot", at = @At("HEAD"), cancellable = true)
    private void vbp$armourOnlyWhenTame(EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        if (slot == EquipmentSlot.BODY) {
            cir.setReturnValue(this.isAlive() && !this.isBaby() && this.isTame());
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void vbp$shearEquipment(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.SHEARS) || player.isSecondaryUseActive() || this.isVehicle()) {
            return;
        }

        ItemStack armor = this.getBodyArmorItem();
        boolean shearArmor = !armor.isEmpty() && vbp$canShear(armor, player);
        ItemStack saddle = this.inventory.getItem(0);
        boolean shearSaddle = !shearArmor && (this.level().isClientSide() ? this.isSaddled() : !saddle.isEmpty() && vbp$canShear(saddle, player));
        if (!shearArmor && !shearSaddle) {
            return;
        }

        if (!this.level().isClientSide()) {
            held.hurtAndBreak(1, player, getSlotForHand(hand));
            float dropHeight = (float) this.getDimensions(this.getPose()).attachments().get(EntityAttachment.PASSENGER, 0, this.getYRot()).y;
            ItemStack sheared;
            SoundEvent sound;
            if (shearArmor) {
                sheared = armor.copy();
                this.setBodyArmorItem(ItemStack.EMPTY);
                sound = ModSoundEvents.ARMOR_UNEQUIP_NAUTILUS.get();
            } else {
                sheared = saddle.copy();
                this.inventory.setItem(0, ItemStack.EMPTY);
                sound = MayhemParity.SADDLE_UNEQUIP;
            }
            this.gameEvent(GameEvent.SHEAR, player);
            this.spawnAtLocation(sheared, dropHeight);
            MayhemParity.shearedEquipment(player, sheared, this);
            this.playSound(sound);
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide()));
    }

    @Unique
    private static boolean vbp$canShear(ItemStack stack, Player player) {
        return !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || player.isCreative();
    }

    /** TPHYS-C09: native knockback resistance is the float literal 0.3F. */
    @ModifyArg(method = "createAttributes",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;add(Lnet/minecraft/core/Holder;D)Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;", ordinal = 3),
        index = 1)
    private static double vbp$floatKnockbackResistance(double value) {
        return value == 0.3 ? (double) 0.3F : value;
    }
}
