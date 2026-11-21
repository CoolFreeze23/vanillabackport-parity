package com.coolfreeze23.vbparity.copper.mixin;

import com.coolfreeze23.vbparity.copper.CopperAntenna;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * COPPER-22: native OfferFlowerGoal is one goal with one nextInt(8000) roll that offers to the nearest
 * #candidate_for_iron_golem_gift (villagers and #accepts_iron_golem_gift), and stop() puts the poppy on the antenna
 * of a gift-accepting mob. This extends the vanilla goal to that native body, so every IronGolem that uses it (vanilla
 * and other mods') behaves natively. VB's separate goal is switched off (OfferCopperGolemFlowerGoalMixin).
 * The 1.21.1 field is typed Villager, so a non-villager target is kept in a separate field.
 */
@Mixin(OfferFlowerGoal.class)
public abstract class OfferFlowerGoalMixin {
    @Shadow @Final private IronGolem golem;
    @Shadow private Villager villager;
    @Shadow private int tick;

    @Unique
    @Nullable
    private LivingEntity vbparity$copper$target;

    @Inject(method = "canUse", at = @At("HEAD"))
    private void vbparity$copper$resetTarget(CallbackInfoReturnable<Boolean> cir) {
        this.vbparity$copper$target = null;
    }

    @WrapOperation(
        method = "canUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getNearestEntity(Ljava/lang/Class;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;DDDLnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/entity/LivingEntity;"
        )
    )
    private LivingEntity vbparity$copper$nearestCandidate(
        Level level, Class<?> type, TargetingConditions conditions, LivingEntity source, double x, double y, double z, AABB box, Operation<LivingEntity> original
    ) {
        LivingEntity nearest = CopperAntenna.nearestGiftCandidate(level, conditions, source, x, y, z, box);
        this.vbparity$copper$target = nearest;
        return nearest instanceof Villager ? nearest : null;
    }

    @ModifyReturnValue(method = "canUse", at = @At("RETURN"))
    private boolean vbparity$copper$anyCandidate(boolean found) {
        return found || this.vbparity$copper$target != null;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/control/LookControl;setLookAt(Lnet/minecraft/world/entity/Entity;FF)V"))
    private void vbparity$copper$lookAtTarget(LookControl control, Entity entity, float yMax, float xMax, Operation<Void> original) {
        Entity look = entity != null ? entity : this.vbparity$copper$target;
        if (look != null) {
            original.call(control, look, yMax, xMax);
        }
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void vbparity$copper$giveAntennaPoppy(CallbackInfo ci) {
        if (this.tick == 0
            && this.vbparity$copper$target instanceof Mob mob
            && mob.getType().is(CopperAntenna.ACCEPTS_IRON_GOLEM_GIFT)
            && mob.getItemBySlot(CopperAntenna.SLOT).isEmpty()
            && this.golem.getBoundingBox().inflate(6.0, 2.0, 6.0).intersects(mob.getBoundingBox())) {
            mob.setItemSlot(CopperAntenna.SLOT, Items.POPPY.getDefaultInstance());
            mob.setGuaranteedDrop(CopperAntenna.SLOT);
        }
        this.vbparity$copper$target = null;
    }
}
