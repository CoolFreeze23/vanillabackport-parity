package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.container.ContainerUser;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COPPER-03: native recheckOpeners counts every container user (players and copper golems, never spectators), not only
 * players. A golem that still holds a chest open keeps it open, and one that stopped lets it close on the next recheck.
 * The 1.21.1 body runs; the player list loses spectators, the count gains the golems and the range takes their reach.
 * Not gated on has_copper_golems: golems exist with it off (eggs, /summon, old worlds) and VB still counts them in and
 * out of chests, so a players-only recheck would push the count below zero.
 */
@Mixin(ContainerOpenersCounter.class)
public abstract class ContainerOpenersCounterMixin {
    @Shadow
    private double maxInteractionRange;

    @Unique
    private List<LivingEntity> vbparity$golemUsers = List.of();

    @WrapOperation(
        method = "recheckOpeners",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/ContainerOpenersCounter;getPlayersWithContainerOpen(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Ljava/util/List;")
    )
    private List<Player> vbparity$allUsers(ContainerOpenersCounter self, Level level, BlockPos pos, Operation<List<Player>> original) {
        List<Player> players = new ArrayList<>(original.call(self, level, pos));
        players.removeIf(Player::isSpectator);
        AABB box = new AABB(pos).inflate(this.maxInteractionRange + 4.0);
        this.vbparity$golemUsers = level.getEntitiesOfClass(
            LivingEntity.class, box, entity -> !(entity instanceof Player) && !entity.isSpectator() && entity instanceof ContainerUser user && user.hasContainerOpen(self, pos)
        );
        return players;
    }

    @Inject(method = "recheckOpeners", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private void vbparity$golemRange(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        for (LivingEntity golem : this.vbparity$golemUsers) {
            this.maxInteractionRange = Math.max(((ContainerUser) golem).getContainerInteractionRange(), this.maxInteractionRange);
        }
    }

    @ModifyExpressionValue(method = "recheckOpeners", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int vbparity$countGolems(int players) {
        int count = players + this.vbparity$golemUsers.size();
        this.vbparity$golemUsers = List.of();
        return count;
    }
}
