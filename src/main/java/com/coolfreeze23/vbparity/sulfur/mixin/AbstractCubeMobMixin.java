package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.AbstractCubeMob;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCubeMob.class)
public abstract class AbstractCubeMobMixin extends AgeableMob {
    protected AbstractCubeMobMixin(EntityType<? extends AgeableMob> type, Level level) {
        super(type, level);
    }

    @Shadow
    protected abstract void setSpawnSize(ServerLevelAccessor level, DifficultyInstance difficulty);

    /**
     * SULFUR-01. Native splits through convertTo(SPLIT_ON_DEATH): the passenger dismounts, the leash drops, and
     * ConversionType#convertCommon copies absorption, effects, left-handedness, name visibility, fire, no-gravity, portal
     * cooldown, silence, tags and team before setUpSplitCube runs. Age is copied there too, but setUpSplitCube's setBaby
     * overwrites it, so the end state is the same as not copying it.
     */
    @WrapOperation(
        method = "remove",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/AbstractCubeMob;setUpSplitCube(Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/AbstractCubeMob;IFF)V"
        )
    )
    private void vbp$inheritOnSplit(AbstractCubeMob self, AbstractCubeMob child, int halfSize, float x, float z, Operation<Void> original) {
        Entity passenger = self.getFirstPassenger();
        if (passenger != null) {
            passenger.stopRiding();
        }
        if (self.isLeashed()) {
            self.dropLeash(true, true);
        }

        child.setAbsorptionAmount(self.getAbsorptionAmount());
        for (MobEffectInstance effect : self.getActiveEffects()) {
            child.addEffect(new MobEffectInstance(effect));
        }
        child.setLeftHanded(self.isLeftHanded());
        child.setCustomNameVisible(self.isCustomNameVisible());
        child.setSharedFlagOnFire(self.isOnFire());
        child.setNoGravity(self.isNoGravity());
        child.setPortalCooldown(self.getPortalCooldown());
        child.setSilent(self.isSilent());
        self.getTags().forEach(child::addTag);
        PlayerTeam team = self.getTeam();
        if (team != null) {
            self.level().getScoreboard().addPlayerToTeam(child.getScoreboardName(), team);
        }

        original.call(self, child, halfSize, x, z);
    }

    /** SULFUR-02. Native uses integer division for the z row: {@code (i / 2 - 0.5F) * offset}. */
    @ModifyArg(
        method = "remove",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/AbstractCubeMob;setUpSplitCube(Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/AbstractCubeMob;IFF)V"
        ),
        index = 3
    )
    private float vbp$integerRow(float z, @Local(index = 7) int i, @Local(index = 4) float xzSpawnOffset) {
        return (i / 2 - 0.5F) * xzSpawnOffset;
    }

    /** SULFUR-03. Native picks the spawn size after AgeableMob#finalizeSpawn, so a cube made a baby there gets size 1. */
    @WrapOperation(
        method = "finalizeSpawn",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/AbstractCubeMob;setSpawnSize(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;)V"
        )
    )
    private void vbp$deferSpawnSize(AbstractCubeMob self, ServerLevelAccessor level, DifficultyInstance difficulty, Operation<Void> original) {
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void vbp$spawnSizeAfterAge(
        ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData groupData,
        CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        this.setSpawnSize(level, difficulty);
    }
}
