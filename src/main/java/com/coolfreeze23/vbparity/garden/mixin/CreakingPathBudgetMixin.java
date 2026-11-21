package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AI-CRK-D3: native Mob.onAttributeUpdated re-budgets the pathfinder when FOLLOW_RANGE changes:
 * maxVisitedNodes = floor(max(follow range, 16) * 16). 1.21.1 keeps the constructor's 512, so the heart-spawned
 * creaking's random follow-range bonus never reached the node budget. Scoped to VB's creaking. Saved modifiers mark
 * the attribute dirty on load, so this also runs for loaded creakings.
 */
@Mixin(LivingEntity.class)
public abstract class CreakingPathBudgetMixin {
    @Inject(method = "onAttributeUpdated", at = @At("TAIL"))
    private void vbparity$garden$creakingNodeBudget(Holder<Attribute> attribute, CallbackInfo ci) {
        if ((Object) this instanceof Creaking creaking && attribute.is(Attributes.FOLLOW_RANGE)) {
            int nodes = Mth.floor(Math.max((float) creaking.getAttributeValue(Attributes.FOLLOW_RANGE), 16.0F) * 16.0F);
            ((PathFinderAccessor) ((PathNavigationAccessor) creaking.getNavigation()).vbparity$garden$pathFinder()).vbparity$garden$setMaxVisitedNodes(nodes);
        }
    }
}
