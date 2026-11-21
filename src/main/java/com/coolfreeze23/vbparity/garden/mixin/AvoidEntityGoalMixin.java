package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AI-CRK-01: native illagers flee creakings with AvoidEntityGoal(8, 1.0, 1.2) (vindicator, pillager, illusioner) and
 * (8, 0.6, 1.0) (evoker); VB registers all four with (8, 0.6, 1.2). Only VB builds this goal with the creaking class,
 * so no other goal changes.
 */
@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin {
    @Shadow @Final @Mutable private double walkSpeedModifier;
    @Shadow @Final @Mutable private double sprintSpeedModifier;

    @Inject(
        method = "<init>(Lnet/minecraft/world/entity/PathfinderMob;Ljava/lang/Class;Ljava/util/function/Predicate;FDDLjava/util/function/Predicate;)V",
        at = @At("TAIL")
    )
    private void vbparity$garden$nativeCreakingFleeSpeeds(
        PathfinderMob mob, Class<?> avoidClass, Predicate<LivingEntity> avoidPredicate, float maxDistance, double walk, double sprint,
        Predicate<LivingEntity> onAvoid, CallbackInfo ci
    ) {
        if (avoidClass != Creaking.class) {
            return;
        }
        EntityType<?> type = mob.getType();
        if (type == EntityType.VINDICATOR || type == EntityType.PILLAGER || type == EntityType.ILLUSIONER) {
            this.walkSpeedModifier = 1.0;
        } else if (type == EntityType.EVOKER) {
            this.sprintSpeedModifier = 1.0;
        }
    }
}
