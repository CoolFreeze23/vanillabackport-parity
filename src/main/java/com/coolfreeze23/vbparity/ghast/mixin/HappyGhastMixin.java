package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.client.registries.ModSoundEvents;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HappyGhast.class)
public abstract class HappyGhastMixin extends Animal {
    protected HappyGhastMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    /** GHAST-01: a worn harness always drops when the ghast dies, as with the current game's setGuaranteedDrop on equip. */
    @Override
    protected float getEquipmentDropChance(EquipmentSlot slot) {
        if (slot == EquipmentSlot.CHEST && ((HappyGhast) (Object) this).isHarnessed()) {
            return 2.0F;
        }

        return super.getEquipmentDropChance(slot);
    }

    /** GHAST-10: ghastling dimensions scale the adult box by 0.2375 with a fixed 0.46875 eye height. */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? this.getType().getDimensions().scale(0.2375F).withEyeHeight(0.46875F) : super.getDefaultDimensions(pose);
    }

    /** GHAST-03: the equip sound comes from onEquipItem (full volume, respects isSilent), not from here. */
    @Inject(method = "equipHarness", at = @At("HEAD"), cancellable = true)
    private void vbp$noHalfVolumeEquipSound(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * GHAST-09: with no "other" entity known, the current game only treats the ghast as solid while it holds still.
     * Being solid to other happy ghasts while ridden, and to players standing on top client-side, is handled per
     * entity in EntityCollisionMixin.
     */
    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void vbp$canBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!this.isBaby() && this.isAlive() && ((HappyGhast) (Object) this).isOnStillTimeout());
    }

    /** GHAST-17: rain heals fast in any dimension with weather; the cloud check keeps its own dimension rule. */
    @Redirect(method = "continuousHeal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;natural()Z"))
    private boolean vbp$anyDimensionCanHealFast(DimensionType type) {
        return true;
    }

    /**
     * GHAST-05 and GHAST-06: the adult interaction in the current game's order. Shears remove the harness only when
     * not sneaking and when Curse of Binding does not hold it (creative ignores binding); the harness drops at the
     * average seat position with a SHEAR game event. Sneaking never mounts and falls through to Animal.
     */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void vbp$mobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (this.isBaby()) {
            return;
        }

        HappyGhast self = (HappyGhast) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            InteractionResult result = stack.interactLivingEntity(player, this, hand);
            if (result.consumesAction()) {
                cir.setReturnValue(result);
                return;
            }
        }

        ItemStack harness = this.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.is(Items.SHEARS)
            && !this.isVehicle()
            && !player.isSecondaryUseActive()
            && self.isHarnessed()
            && (!EnchantmentHelper.has(harness, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || player.isCreative())) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            this.gameEvent(GameEvent.SHEAR, player);
            if (!this.level().isClientSide) {
                // The average of the four seat attachments: (0, 4, 0).
                this.spawnAtLocation(harness, 4.0F);
                com.coolfreeze23.vbparity.mayhem.MayhemParity.shearedEquipment(player, harness, this);
            }

            this.playSound(ModSoundEvents.HARNESS_UNEQUIP.get());
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (self.isHarnessed() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }

            cir.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide()));
            return;
        }

        cir.setReturnValue(super.mobInteract(player, hand));
    }
}
