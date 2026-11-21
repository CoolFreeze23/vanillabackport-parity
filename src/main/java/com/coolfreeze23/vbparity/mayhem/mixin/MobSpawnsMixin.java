package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.misc.MiscContent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SPAWN-06: native Monster.checkMonsterSpawnRules has no Peaceful test; Peaceful is decided per type (SpawnPlacements:
 * {@code !type.isAllowedInPeaceful()}). VB's copy refuses every type on Peaceful, so zombie horses and spawner camel
 * husks never appeared there. Types the add-on lets spawn on Peaceful skip VB's test.
 */
@Mixin(targets = "com.blackgear.vanillabackport.common.integrations.MobSpawns", remap = false)
public abstract class MobSpawnsMixin {
    @WrapOperation(method = "checkMonsterSpawnRules",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getDifficulty()Lnet/minecraft/world/Difficulty;"))
    private static Difficulty vbp$peacefulPerType(ServerLevelAccessor level, Operation<Difficulty> original, @Local(argsOnly = true) EntityType<?> type) {
        Difficulty difficulty = original.call(level);
        return difficulty == Difficulty.PEACEFUL && type.is(MiscContent.MONSTERS_THAT_SPAWN_ON_PEACEFUL) ? Difficulty.EASY : difficulty;
    }
}
