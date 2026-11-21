package com.coolfreeze23.vbparity.flora.mixin.client;

import com.blackgear.vanillabackport.client.level.sound.AmbientDesertBlockSoundsPlayer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * FLORA-03: native plays the dry-grass wind with {@code level.playPlayerSound}, an EntityBoundSoundInstance on the local
 * player (follows the listener, no falloff from the grass). VB plays it at the grass block's corner.
 */
@Mixin(AmbientDesertBlockSoundsPlayer.class)
public abstract class AmbientDesertBlockSoundsPlayerMixin {
    @WrapOperation(
        method = "playAmbientDryGrassSounds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V")
    )
    private static void vbparity$playOnPlayer(
        Level level, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay,
        Operation<Void> original
    ) {
        if (!level.isClientSide()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.getSoundManager().play(new EntityBoundSoundInstance(sound, source, volume, pitch, minecraft.player, level.getRandom().nextLong()));
        }
    }
}
