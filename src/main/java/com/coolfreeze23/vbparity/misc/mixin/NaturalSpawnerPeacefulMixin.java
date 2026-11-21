package com.coolfreeze23.vbparity.misc.mixin;

import com.coolfreeze23.vbparity.misc.MiscContent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * With monster spawning on in Peaceful, skip spawn attempts for types that cannot exist in Peaceful before the
 * entity is created (otherwise NaturalSpawner logs "Can't spawn entity of type" for each refused attempt).
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerPeacefulMixin {
    @ModifyReturnValue(method = "isValidSpawnPostitionForType", at = @At("RETURN"))
    private static boolean vbp$skipNotInPeaceful(boolean valid, @Local(argsOnly = true) ServerLevel level,
                                                 @Local(argsOnly = true) MobSpawnSettings.SpawnerData data) {
        return valid && !(level.getDifficulty() == Difficulty.PEACEFUL && MiscContent.notAllowedInPeaceful(data.type));
    }
}
