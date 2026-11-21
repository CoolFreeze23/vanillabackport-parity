package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolem;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CopperGolem.class)
public abstract class CopperGolemMixin {
    /** COPPER-07: level event 3003 already plays the wax-on sound on the client; native plays no extra server sound. */
    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
        )
    )
    private void vbparity$noDoubleWaxSound(
        Level level, Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch, Operation<Void> original
    ) {
        if (sound != SoundEvents.HONEYCOMB_WAX_ON) {
            original.call(level, player, pos, sound, source, volume, pitch);
        }
    }

    /** COPPER-32: native readyForShearing only checks the antenna item. */
    @ModifyExpressionValue(
        method = "readyForShearing",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/golem/copper_golem/CopperGolem;isAlive()Z")
    )
    private boolean vbparity$shearWhileDying(boolean alive) {
        return true;
    }
}
