package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.block_entities.CreakingHeartBlockEntity;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.coolfreeze23.vbparity.garden.GardenResin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreakingHeartBlockEntity.class)
public abstract class CreakingHeartBlockEntityMixin {
    /** GARDEN-01: one resin face per call, as native (TraversalNodeStatus.STOP). */
    @Inject(method = "spreadResin", at = @At("HEAD"), cancellable = true)
    private void vbparity$garden$spreadResinOnce(ServerLevel level, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        cir.setReturnValue(GardenResin.spread(level, ((BlockEntity) (Object) this).getBlockPos()));
    }

    /** GARDEN-08: native plays creaking.spawn with makeSound (random voice pitch). */
    @WrapOperation(
        method = "serverTick",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/creaking/Creaking;playSound(Lnet/minecraft/sounds/SoundEvent;)V")
    )
    private static void vbparity$garden$spawnVoicePitch(Creaking creaking, SoundEvent sound, Operation<Void> original) {
        creaking.makeSound(sound);
    }
}
