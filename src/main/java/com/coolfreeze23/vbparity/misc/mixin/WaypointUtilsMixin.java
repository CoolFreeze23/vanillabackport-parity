package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.api.modules.waypoints.WaypointUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native hides a head-slot wearer through the item's own attribute modifier
 * ({@code minecraft:waypoint_transmit_range_hide}, added to the pumpkin and heads in MiscModEvents), not through a
 * player-only tag check. Native invisibility uses its own id {@code minecraft:effect.waypoint_transmit_range_hide};
 * VB reused the item id for it, which would strip the item's modifier every tick.
 */
@Mixin(WaypointUtils.class)
public abstract class WaypointUtilsMixin {
    @Unique
    private static final AttributeModifier VBP_INVISIBILITY_HIDE = new AttributeModifier(
            ResourceLocation.withDefaultNamespace("effect.waypoint_transmit_range_hide"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    @ModifyExpressionValue(method = "onPlayerAttributeUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static boolean vbp$noHeadTagCheck(boolean original) {
        return false;
    }

    @ModifyExpressionValue(method = "onEntityTick", at = @At(value = "FIELD",
            target = "Lcom/blackgear/vanillabackport/common/api/modules/waypoints/WaypointUtils;WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER:Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;"))
    private static AttributeModifier vbp$invisibilityModifier(AttributeModifier original) {
        return VBP_INVISIBILITY_HIDE;
    }
}
