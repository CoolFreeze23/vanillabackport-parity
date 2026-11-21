package com.coolfreeze23.vbparity.mayhem.mixin.client;

import com.blackgear.vanillabackport.common.level.items.spear.SpearAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MAYHEM-34: native third-person spear curves use getTicksUsingItem(partialTicks) (ticks + partial while using), so
 * the raise, sway and lower run smoothly; VB fed the whole-tick count. Render thread only.
 */
@Mixin(value = SpearAnimations.class, remap = false)
public abstract class SpearAnimationsMixin {
    @Unique
    private static LivingEntity vbp$posedEntity;

    @Unique
    private static float vbp$partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    @Inject(method = "thirdPersonHandUse", at = @At("HEAD"))
    private static void vbp$rememberEntity(HumanoidModel<?> model, ModelPart arm, ModelPart head, HumanoidArm holdingArm, ItemStack item, LivingEntity entity,
                                           CallbackInfo ci) {
        vbp$posedEntity = entity;
    }

    @ModifyArg(
        method = "thirdPersonHandUse",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/items/spear/SpearAnimations$UseParams;fromKineticWeapon(Lcom/blackgear/vanillabackport/common/level/components/KineticWeapon;F)Lcom/blackgear/vanillabackport/common/level/items/spear/SpearAnimations$UseParams;"),
        index = 1
    )
    private static float vbp$smoothArmTime(float time) {
        LivingEntity entity = vbp$posedEntity;
        vbp$posedEntity = null;
        return entity != null && entity.isUsingItem() ? time + vbp$partialTick() : time;
    }

    @ModifyVariable(method = "thirdPersonUseItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static float vbp$smoothItemTime(float timeHeld) {
        return timeHeld != 0.0F ? timeHeld + vbp$partialTick() : timeHeld;
    }
}
