package com.coolfreeze23.vbparity.garden;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

/** Native 26.2 creaking sensing and target-validity logic, rebuilt on the 1.21.1 brain. */
public final class GardenCreaking {
    private static final long TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200L;

    private GardenCreaking() {
    }

    /**
     * Native PlayerSensor.doTick for a creaking: every range is the FOLLOW_RANGE attribute (32), as native
     * Sensor.updateTargetingConditionRanges / PlayerSensor.getFollowDistance do. 1.21.1 hard-codes 16.
     */
    public static void tickPlayerSensor(ServerLevel level, Creaking creaking) {
        double range = creaking.getAttributeValue(Attributes.FOLLOW_RANGE);
        List<Player> players = level.players()
            .stream()
            .filter(EntitySelector.NO_SPECTATORS)
            .filter(player -> creaking.closerThan(player, range))
            .sorted(Comparator.comparingDouble(creaking::distanceToSqr))
            .collect(Collectors.toList());
        Brain<?> brain = creaking.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_PLAYERS, players);
        List<Player> visible = players.stream().filter(player -> isTargetable(creaking, player, range)).collect(Collectors.toList());
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, visible.isEmpty() ? null : visible.get(0));
        List<Player> attackable = visible.stream().filter(player -> isAttackable(creaking, player, range)).toList();
        ((CreakingSensorData) creaking).vbparity$garden$setVisibleAttackablePlayers(attackable);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, attackable.isEmpty() ? null : attackable.get(0));
    }

    private static boolean isTargetable(Creaking creaking, LivingEntity target, double range) {
        TargetingConditions conditions = TargetingConditions.forNonCombat().range(range);
        if (creaking.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target)) {
            conditions.ignoreInvisibilityTesting();
        }
        return conditions.test(creaking, target);
    }

    private static boolean isAttackable(Creaking creaking, LivingEntity target, double range) {
        TargetingConditions conditions = TargetingConditions.forCombat().range(range);
        if (creaking.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target)) {
            conditions.ignoreInvisibilityTesting();
        }
        return conditions.test(creaking, target);
    }

    /** Native CreakingAi.isAttackTargetStillReachable. */
    public static boolean isAttackTargetStillReachable(Creaking creaking, LivingEntity target) {
        List<Player> players = ((CreakingSensorData) creaking).vbparity$garden$getVisibleAttackablePlayers();
        return players != null && target instanceof Player player && players.contains(player);
    }

    /**
     * Native StopAttackingIfTargetInvalid.create((level, target) -> !isAttackTargetStillReachable(body, target)).
     * 1.21.1's predicate overload only receives the target, so the mob-aware copy lives here.
     */
    public static BehaviorControl<Creaking> stopAttackingIfTargetUnreachable() {
        return BehaviorBuilder.create(
            instance -> instance.group(instance.present(MemoryModuleType.ATTACK_TARGET), instance.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
                .apply(instance, (attackTarget, cantReachSince) -> (level, creaking, time) -> {
                    LivingEntity target = instance.get(attackTarget);
                    if (creaking.canAttack(target)
                        && !isTiredOfTryingToReachTarget(creaking, instance.tryGet(cantReachSince))
                        && target.isAlive()
                        && target.level() == creaking.level()
                        && isAttackTargetStillReachable(creaking, target)) {
                        return true;
                    }
                    attackTarget.erase();
                    return true;
                })
        );
    }

    private static boolean isTiredOfTryingToReachTarget(LivingEntity entity, Optional<Long> cantReachSince) {
        return cantReachSince.isPresent() && entity.level().getGameTime() - cantReachSince.get() > TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE;
    }
}
