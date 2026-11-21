package com.coolfreeze23.vbparity.misc.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Native ThrownEnderpearl differences:
 * - no endermite roll result on Peaceful ({@code level.isSpawningMonsters() && difficulty != PEACEFUL});
 * - the pearl's portal cooldown is passed to the player it teleports;
 * - a dead owner's pearls are kept while the owner is in the End credits ({@code !serverPlayer.wonGame});
 * - a pearl whose owner is not an online player (offline, or a mob) keeps flying instead of being deleted.
 * The landing point needs no change: 1.21.1 calls onHit before the pearl moves, so {@code getX()} already is the
 * native {@code oldPosition()} (the position the pearl had at the start of the impact tick).
 */
@Mixin(value = ThrownEnderpearl.class, priority = 2000)
public abstract class ThrownEnderpearlMixin extends ThrowableItemProjectile {
    public ThrownEnderpearlMixin(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean vbp$noEndermiteOnPeaceful(boolean doMobSpawning) {
        return doMobSpawning && this.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;"))
    private void vbp$passPortalCooldown(HitResult result, CallbackInfo ci) {
        Entity owner = this.getOwner();
        if (owner != null && this.isOnPortalCooldown()) {
            owner.setPortalCooldown();
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean vbp$keepPearlsDuringCredits(boolean vanishOnDeath) {
        return vanishOnDeath && !(this.getOwner() instanceof ServerPlayer player && player.wonGame);
    }

    /** Owner UUID hidden from VB's offline-owner check for the current tick (see vbp$keepPearlsOfOfflineOwners). */
    @Unique
    @Nullable
    private UUID vbp$hiddenOwner;

    /**
     * VB deletes (without saving) every pearl whose owner UUID is not an online player, from a HEAD injection in
     * {@code tick}. Native just lets such a pearl fly. The owner UUID is hidden while VB's check runs and put back at
     * the first statement of the vanilla tick body ({@code getOwner()}), so the original tick, VB's other code and other
     * mods' injections all still run.
     */
    @WrapMethod(method = "tick")
    private void vbp$keepPearlsOfOfflineOwners(Operation<Void> original) {
        if (this.level() instanceof ServerLevel serverLevel) {
            UUID ownerUUID = ((ProjectileOwnerAccessor) this).vbp$getOwnerUUID();
            if (ownerUUID != null && serverLevel.getServer().getPlayerList().getPlayer(ownerUUID) == null) {
                this.vbp$hiddenOwner = ownerUUID;
                ((ProjectileOwnerAccessor) this).vbp$setOwnerUUID(null);
            }
        }
        try {
            original.call();
        } finally {
            this.vbp$restoreOwner();
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/entity/projectile/ThrownEnderpearl;getOwner()Lnet/minecraft/world/entity/Entity;"))
    private Entity vbp$restoreOwnerBeforeVanillaTick(ThrownEnderpearl pearl, Operation<Entity> original) {
        this.vbp$restoreOwner();
        return original.call(pearl);
    }

    @Unique
    private void vbp$restoreOwner() {
        if (this.vbp$hiddenOwner != null) {
            ((ProjectileOwnerAccessor) this).vbp$setOwnerUUID(this.vbp$hiddenOwner);
            this.vbp$hiddenOwner = null;
        }
    }
}
