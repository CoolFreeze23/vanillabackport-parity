package com.coolfreeze23.vbparity.mayhem.mixin.client;

import com.blackgear.vanillabackport.common.api.extensions.entity.arms.ArmPoses;
import com.blackgear.vanillabackport.common.level.components.SwingAnimation;
import com.blackgear.vanillabackport.common.level.components.SwingAnimationType;
import com.blackgear.vanillabackport.common.level.items.spear.SpearAnimations;
import com.blackgear.vanillabackport.core.ModChecker;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third-person spear details of the current game's HumanoidModel.
 *
 * <p>setupAttackAnimation: natively the torso twist and arm pivot shift run for every swing before the per-type part
 * ({@code case STAB -> SpearAnimations.thirdPersonAttackHand}). VB cancelled the whole method for a stab, so the body
 * never twisted. This runs first (priority 900, before VB's hook at the same point), does the 1.21.1 preamble and the
 * stab, and cancels.
 *
 * <p>setupAnim: natively the second arm is not posed when the first-posed arm's pose affects the off hand
 * ({@code ArmPose.SPEAR(false, true)}), so with a spear held the other arm keeps its walking swing.
 */
@Mixin(value = HumanoidModel.class, priority = 900)
public abstract class HumanoidModelStabMixin<T extends LivingEntity> extends AgeableListModel<T> {
    @Shadow
    public HumanoidModel.ArmPose rightArmPose;
    @Shadow
    public HumanoidModel.ArmPose leftArmPose;
    @Shadow
    @Final
    public ModelPart rightArm;
    @Shadow
    @Final
    public ModelPart leftArm;
    @Shadow
    @Final
    public ModelPart body;

    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void vbp$stabKeepsBodyTwist(T entity, float ageInTicks, CallbackInfo ci) {
        if (ModChecker.BETTER_COMBAT || this.attackTime <= 0.0F) {
            return;
        }

        InteractionHand hand = entity.swingingArm != null ? entity.swingingArm : InteractionHand.MAIN_HAND;
        SwingAnimation animation = SwingAnimation.get(entity.getItemInHand(hand));
        if (animation == null || animation.type() != SwingAnimationType.STAB) {
            return;
        }

        HumanoidArm attackArm = hand == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
        float attackTime = this.attackTime;
        this.body.yRot = Mth.sin(Mth.sqrt(attackTime) * (float) (Math.PI * 2)) * 0.2F;
        if (attackArm == HumanoidArm.LEFT) {
            this.body.yRot *= -1.0F;
        }

        this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F;
        this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F;
        this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F;
        this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F;
        this.rightArm.yRot = this.rightArm.yRot + this.body.yRot;
        this.leftArm.yRot = this.leftArm.yRot + this.body.yRot;
        this.leftArm.xRot = this.leftArm.xRot + this.body.yRot;
        SpearAnimations.thirdPersonAttackHand((HumanoidModel<T>) (Object) this, attackTime, attackArm);
        ci.cancel();
    }

    // setupAnim, not using an item: left posed first, then right (poseRightArm ordinal 1).
    @WrapWithCondition(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;poseRightArm(Lnet/minecraft/world/entity/LivingEntity;)V", ordinal = 1))
    private boolean vbp$spearLeftLeavesRight(HumanoidModel<T> self, LivingEntity entity) {
        return this.leftArmPose != ArmPoses.SPEAR.get();
    }

    // setupAnim, not using an item: right posed first, then left (poseLeftArm ordinal 2).
    @WrapWithCondition(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;poseLeftArm(Lnet/minecraft/world/entity/LivingEntity;)V", ordinal = 2))
    private boolean vbp$spearRightLeavesLeft(HumanoidModel<T> self, LivingEntity entity) {
        return this.rightArmPose != ArmPoses.SPEAR.get();
    }
}
