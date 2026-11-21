package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.AiParity;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.PlayerSensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AI-MNT-D3: native PlayerSensor keeps players within getFollowDistance(body) = FOLLOW_RANGE (1.21.1: 16). Same body
 * as 1.21.1 otherwise; scoped to the happy ghast, (zombie) nautilus and camel husk.
 */
@Mixin(PlayerSensor.class)
public abstract class PlayerSensorRangeMixin {
    @Inject(method = "doTick", at = @At("HEAD"), cancellable = true)
    private void vbp$followRange(ServerLevel level, LivingEntity body, CallbackInfo ci) {
        if (!AiParity.followRangeSensors(body)) {
            return;
        }
        ci.cancel();
        double range = AiParity.followRange(body);
        List<Player> players = level.players()
            .stream()
            .filter(EntitySelector.NO_SPECTATORS)
            .filter(player -> body.closerThan(player, range))
            .sorted(Comparator.comparingDouble(body::distanceToSqr))
            .map(Player.class::cast)
            .collect(Collectors.toList());
        Brain<?> brain = body.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_PLAYERS, players);
        List<Player> visible = players.stream().filter(p -> Sensor.isEntityTargetable(body, p)).collect(Collectors.toList());
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, visible.isEmpty() ? null : visible.get(0));
        Optional<Player> attackable = visible.stream().filter(p -> Sensor.isEntityAttackable(body, p)).findFirst();
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, attackable);
    }
}
