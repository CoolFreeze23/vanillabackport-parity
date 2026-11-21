package com.coolfreeze23.vbparity.ai.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-VAR-04: native Frog.finalizeSpawn selects its variant from the frog_variant registry (always one candidate, one
 * nextInt(1) on the level random) right before FrogAi.initMemories; also on tadpole conversion. 1.21.1 picks by biome
 * tag without a draw; the result is the same.
 */
@Mixin(Frog.class)
public abstract class FrogSpawnDrawMixin {
    @Inject(method = "finalizeSpawn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/animal/frog/FrogAi;initMemories(Lnet/minecraft/world/entity/animal/frog/Frog;Lnet/minecraft/util/RandomSource;)V"))
    private void vbp$variantDraw(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data,
                                 CallbackInfoReturnable<SpawnGroupData> cir) {
        level.getRandom().nextInt(1);
    }
}
