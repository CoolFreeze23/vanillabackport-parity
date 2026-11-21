package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GHAST-12: the current game reads the tempt range from the TEMPT_RANGE attribute, which is 16 for happy ghasts.
 * 1.21.1 has no such attribute and hard-codes 10, so ghastlings get their own 16-block pass here.
 */
@Mixin(TemptingSensor.class)
public abstract class TemptingSensorMixin {
    @Unique
    private static final double VBP$HAPPY_GHAST_TEMPT_RANGE = 16.0;
    @Unique
    private static final TargetingConditions VBP$HAPPY_GHAST_TARGETING = TargetingConditions.forNonCombat().range(VBP$HAPPY_GHAST_TEMPT_RANGE).ignoreLineOfSight();

    @Shadow
    private boolean playerHoldingTemptation(Player player) {
        throw new AssertionError();
    }

    @Inject(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;)V", at = @At("HEAD"), cancellable = true)
    private void vbp$happyGhastTemptRange(ServerLevel level, PathfinderMob body, CallbackInfo ci) {
        if (!(body instanceof HappyGhast)) {
            return;
        }

        ci.cancel();
        Brain<?> brain = body.getBrain();
        List<Player> players = level.players()
            .stream()
            .filter(EntitySelector.NO_SPECTATORS)
            .filter(player -> VBP$HAPPY_GHAST_TARGETING.test(body, player))
            .filter(this::playerHoldingTemptation)
            .filter(player -> !body.hasPassenger(player))
            .sorted(Comparator.comparingDouble(body::distanceToSqr))
            .map(Player.class::cast)
            .toList();
        if (!players.isEmpty()) {
            brain.setMemory(MemoryModuleType.TEMPTING_PLAYER, players.get(0));
        } else {
            brain.eraseMemory(MemoryModuleType.TEMPTING_PLAYER);
        }
    }
}
