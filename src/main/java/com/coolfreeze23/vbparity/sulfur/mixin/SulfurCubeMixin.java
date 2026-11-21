package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.AbstractCubeMob;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SulfurCube.class)
public abstract class SulfurCubeMixin extends AbstractCubeMob implements com.coolfreeze23.vbparity.sulfur.StabKnockbackTarget {
    @Shadow
    @Nullable
    private DamageSource pendingKnockbackSource;

    @Shadow
    private float pendingKnockbackDamage;

    /** TPHYS-C05: a knockback-only stab never calls hurt; native still passes the source and damage to knockback. */
    @Override
    public void vbparity$prepareKnockback(DamageSource source, float damage) {
        this.pendingKnockbackSource = source;
        this.pendingKnockbackDamage = damage;
    }

    protected SulfurCubeMixin(EntityType<? extends AgeableMob> type, Level level) {
        super(type, level);
    }

    // SULFUR-04 / AI-SLF-01: native scales the archetype push by power * 0.25 only when the caller marks the knockback
    // as coming from an effect (extra knockback of an attack, second stab push); see KnockbackEffect.
    @ModifyExpressionValue(method = "knockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;sqrt(F)F"))
    private float vbp$extraKnockbackScale(float powerMultiplier, @Local(argsOnly = true, ordinal = 0) double power) {
        return com.coolfreeze23.vbparity.sulfur.KnockbackEffect.fromEffect() ? powerMultiplier * (float) power * 0.25F : powerMultiplier;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vbp$dropStaleKnockbackSource(CallbackInfo ci) {
        this.pendingKnockbackSource = null;
    }

    // SULFUR-05: fuse priming and baby eating use the mob voice (LivingEntity#makeSound: 0.4 * size, voice pitch).
    @Redirect(
        method = {"primeTime", "mobInteract"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;playSound(Lnet/minecraft/sounds/SoundEvent;)V"
        )
    )
    private void vbp$voice(SulfurCube self, SoundEvent sound) {
        self.makeSound(sound);
    }

    // SULFUR-06: a block restored from a bucket is set silently (native applies the content component).
    @Redirect(
        method = "loadFromBucketTag",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;equipItem(Lnet/minecraft/world/item/ItemStack;)Z"
        )
    )
    private boolean vbp$silentBucketContent(SulfurCube self, ItemStack stack) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, stack.copyWithCount(1)); // SULFUR-18: native BODY
        return true;
    }

    // SULFUR-07: ejected/sheared blocks drop at the passenger attachment, (0, bbHeight, 0).
    @ModifyArg(
        method = {"equipItem", "shear"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"
        ),
        index = 1
    )
    private float vbp$dropAtAttachment(float offsetY) {
        return this.getBbHeight();
    }

    // SULFUR-08: a custom name alone does not make the cube persistent.
    @Inject(method = "isPersistenceRequired", at = @At("HEAD"), cancellable = true)
    private void vbp$nameIsNotPersistence(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((MobAccessor) this).vbp$sulfurPersistenceRequired());
    }

    /** SULFUR-14: native mobInteract fires player_sheared_equipment with the sheared-off block after shearing. */
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
        method = "mobInteract",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;shear(Lnet/minecraft/sounds/SoundSource;)V")
    )
    private void vbp$shearedEquipmentTrigger(
        SulfurCube self, net.minecraft.sounds.SoundSource source, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
        @Local(argsOnly = true) net.minecraft.world.entity.player.Player player
    ) {
        ItemStack sheared = self.getItemBySlot(EquipmentSlot.BODY).copy();
        original.call(self, source);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.coolfreeze23.vbparity.mayhem.MayhemParity.PLAYER_SHEARED_EQUIPMENT.trigger(serverPlayer, sheared, self);
        }
    }

    /**
     * Native mobInteract uses ItemStack.consume(1, player) (baby food through usePlayerItem, fire charge when priming), which
     * keeps the stack for players with infinite materials. VB shrinks the fire charge even in creative.
     */
    @Redirect(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void vbp$consumeLikeNative(ItemStack stack, int amount, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        stack.consume(amount, player);
    }

    /** AI-SLF-03: native Bucketable.bucketMobPickup never sets persistence; a released cube is kept by fromBucket only. */
    @Redirect(
        method = "bucketMobPickup",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;setPersistenceRequired()V")
    )
    private static void vbp$noBucketPersistence(SulfurCube cube) {
    }

    /**
     * AI-SLF-04: native dispensers equip a sulfur cube only through canDispenserEquipIntoSlot(BODY), which no armour,
     * elytra or pumpkin uses, so none can be dispensed onto a cube. 1.21.1's armour dispenser asks canTakeItem.
     */
    @Override
    public boolean canTakeItem(ItemStack stack) {
        return false;
    }

    /** AI-SLF-05: dispenser shears drop the block like player shears (SulfurCube.shear), not NeoForge's sheep drop. */
    public void spawnShearedDrop(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, ItemStack drop) {
        this.spawnAtLocation(drop, this.getBbHeight());
    }
}
