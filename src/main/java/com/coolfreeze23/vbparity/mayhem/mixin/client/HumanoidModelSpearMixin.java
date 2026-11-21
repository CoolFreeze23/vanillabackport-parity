package com.coolfreeze23.vbparity.mayhem.mixin.client;

import com.blackgear.vanillabackport.common.api.extensions.entity.arms.ArmPoses;
import com.blackgear.vanillabackport.common.level.components.SwingAnimation;
import com.blackgear.vanillabackport.common.level.components.SwingAnimationType;
import com.blackgear.vanillabackport.core.data.tags.ModItemTags;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MAYHEM-35: native HumanoidMobRenderer.getArmPose gives mobs the SPEAR pose for an arm holding a spear (or swinging a
 * STAB item), and AbstractZombieRenderer also for the arm opposite a STAB item. The models are shared between mobs,
 * so a SPEAR pose left over from another mob is cleared; other poses set by subclass models are left alone.
 * MAYHEM-36: native swimming keeps a SPEAR arm out of the swim stroke.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelSpearMixin<T extends LivingEntity> {
    @Shadow
    public HumanoidModel.ArmPose rightArmPose;
    @Shadow
    public HumanoidModel.ArmPose leftArmPose;

    @Inject(method = "prepareMobModel(Lnet/minecraft/world/entity/LivingEntity;FFF)V", at = @At("TAIL"))
    private void vbp$mobSpearPose(T entity, float limbSwing, float limbSwingAmount, float partialTick, CallbackInfo ci) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        boolean zombie = (Object) this instanceof AbstractZombieModel<?>;
        this.rightArmPose = vbp$pose(mob, HumanoidArm.RIGHT, this.rightArmPose, zombie);
        this.leftArmPose = vbp$pose(mob, HumanoidArm.LEFT, this.leftArmPose, zombie);
    }

    @Unique
    private static HumanoidModel.ArmPose vbp$pose(Mob mob, HumanoidArm arm, HumanoidModel.ArmPose current, boolean zombie) {
        HumanoidModel.ArmPose spear = ArmPoses.SPEAR.get();
        ItemStack held = vbp$heldByArm(mob, arm);
        boolean spearPose = (vbp$isStab(held) && mob.swinging) || held.is(ModItemTags.SPEARS)
            || (zombie && vbp$isStab(vbp$heldByArm(mob, arm.getOpposite())));
        if (spearPose) {
            return spear;
        }
        return current == spear ? HumanoidModel.ArmPose.EMPTY : current;
    }

    @Unique
    private static ItemStack vbp$heldByArm(Mob mob, HumanoidArm arm) {
        return mob.getMainArm() == arm ? mob.getMainHandItem() : mob.getOffhandItem();
    }

    @Unique
    private static boolean vbp$isStab(ItemStack stack) {
        SwingAnimation animation = SwingAnimation.get(stack);
        return animation != null && animation.type() == SwingAnimationType.STAB;
    }

    // The 5th and 6th reads of swimAmount in setupAnim are the right and left arm swim amounts (1.21.1 HumanoidModel 246-247).
    @ModifyExpressionValue(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/HumanoidModel;swimAmount:F", ordinal = 4)
    )
    private float vbp$rightArmSwim(float swimAmount) {
        return this.rightArmPose == ArmPoses.SPEAR.get() ? 0.0F : swimAmount;
    }

    @ModifyExpressionValue(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/HumanoidModel;swimAmount:F", ordinal = 5)
    )
    private float vbp$leftArmSwim(float swimAmount) {
        return this.leftArmPose == ArmPoses.SPEAR.get() ? 0.0F : swimAmount;
    }
}
