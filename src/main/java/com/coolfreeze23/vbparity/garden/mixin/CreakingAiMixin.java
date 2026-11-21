package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.CreakingAi;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.CreakingMeleeAttack;
import com.coolfreeze23.vbparity.garden.GardenCreaking;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GARDEN-03/04: native FIGHT activity. It only requires ATTACK_TARGET (it does not erase it when the activity stops,
 * so a freeze keeps the target), and it drops a target that is no longer in the visible + attackable player list.
 */
@Mixin(CreakingAi.class)
public abstract class CreakingAiMixin {
    @Inject(method = "initFightActivity", at = @At("HEAD"), cancellable = true)
    private static void vbparity$garden$nativeFightActivity(Brain<Creaking> brain, CallbackInfo ci) {
        brain.addActivityWithConditions(
            Activity.FIGHT,
            ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Creaking>>>of(
                Pair.of(10, SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F)),
                Pair.of(11, CreakingMeleeAttack.<Creaking>create(Creaking::canMove, 40)),
                Pair.of(12, GardenCreaking.stopAttackingIfTargetUnreachable())
            ),
            ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT))
        );
        ci.cancel();
    }
}
