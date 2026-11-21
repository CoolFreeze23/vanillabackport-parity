package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.client.registries.ModSoundEvents;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.coolfreeze23.vbparity.garden.CreakingSensorData;
import com.coolfreeze23.vbparity.garden.GardenRegistry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creaking.class)
public abstract class CreakingMixin extends Monster implements CreakingSensorData {
    @Unique
    @Nullable
    private List<Player> vbparity$garden$visibleAttackablePlayers;

    protected CreakingMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    @Nullable
    public List<Player> vbparity$garden$getVisibleAttackablePlayers() {
        return this.vbparity$garden$visibleAttackablePlayers;
    }

    @Override
    public void vbparity$garden$setVisibleAttackablePlayers(@Nullable List<Player> players) {
        this.vbparity$garden$visibleAttackablePlayers = players;
    }

    /** GARDEN-08: freeze, unfreeze, tear-down death, twitch, activate and deactivate use makeSound (voice pitch 0.8-1.2). */
    @WrapOperation(
        method = {"aiStep", "tearDown", "creakingDeathEffects", "activate", "deactivate"},
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/creaking/Creaking;playSound(Lnet/minecraft/sounds/SoundEvent;)V")
    )
    private void vbparity$garden$voicePitch(Creaking self, SoundEvent sound, Operation<Void> original) {
        self.makeSound(sound);
    }

    /**
     * GARDEN-09: native plays the attack sound with makeSound. On the client (entity event 4) that call is a no-op;
     * the audible copy is the server's, sent from Mob.doHurtTarget only when the hit lands.
     */
    @Inject(method = "playAttackSound", at = @At("HEAD"), cancellable = true)
    private void vbparity$garden$serverAttackSound(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.makeSound(ModSoundEvents.CREAKING_ATTACK.get());
        }
        ci.cancel();
    }

    /** GARDEN-07: native has no passenger restrictions on a heart-bound creaking. */
    @ModifyExpressionValue(
        method = {"canAddPassenger", "couldAcceptPassenger", "addPassenger"},
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/creaking/Creaking;isHeartBound()Z")
    )
    private boolean vbparity$garden$allowPassengers(boolean heartBound) {
        return false;
    }

    /** GARDEN-10: tear-down particles are block_crumble (zero speed, 1-10 tick life), not block. */
    @ModifyExpressionValue(
        method = "tearDown",
        at = @At(value = "FIELD", target = "Lnet/minecraft/core/particles/ParticleTypes;BLOCK:Lnet/minecraft/core/particles/ParticleType;", opcode = Opcodes.GETSTATIC)
    )
    private ParticleType<BlockParticleOption> vbparity$garden$crumbleParticles(ParticleType<BlockParticleOption> original) {
        return GardenRegistry.BLOCK_CRUMBLE;
    }
}
