package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** AI-MNT-D5: native WaterBoundPathNavigation advances to the next node within 0.5 vertically (1.21.1: 1.0). Scoped. */
@Mixin(PathNavigation.class)
public abstract class NautilusWaypointMixin {
    @Shadow
    @Final
    protected Mob mob;

    @ModifyConstant(method = "followThePath", constant = @Constant(doubleValue = 1.0))
    private double vbp$waterWaypointHeight(double tolerance) {
        return (Object) this instanceof WaterBoundPathNavigation && this.mob instanceof AbstractNautilus ? 0.5 : tolerance;
    }
}
