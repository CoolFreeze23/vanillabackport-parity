package com.coolfreeze23.vbparity.variants.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.coolfreeze23.vbparity.variants.CatHutSpawn;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Cat breed selection reads the native 26.2 moon brightness. Natively the moon phase is an environment attribute driven
 * by the {@code minecraft:moon} timeline, which only the overworld dimension types run ({@code #minecraft:in_overworld});
 * elsewhere it keeps its default {@code FULL_MOON} (brightness 1.0), so every breed (all_black included) can spawn.
 * 1.21.1 derives the phase from the world's day count in every dimension. A dimension with a fixed time (the Nether,
 * the End and modded copies of them) has no moon cycle and gets native's default.
 */
@Mixin(Cat.class)
public abstract class CatNativeSpawnMixin {
    @WrapOperation(
        method = "finalizeSpawn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getMoonBrightness()F")
    )
    private float vbp$nativeMoon(ServerLevelAccessor level, Operation<Float> original) {
        return level.dimensionType().hasFixedTime() ? 1.0F : original.call(level);
    }

    /**
     * VARIANTS-21 (persistence part): native {@code Cat.finalizeSpawn} never makes a cat persistent; witch-hut cats are
     * made persistent by whoever spawns them ({@code CatSpawner.spawnInHut} and {@code SwampHutPiece}). 1.21.1 does it
     * inside {@code finalizeSpawn} whenever the cat stands in a hut, so a cat from a spawn egg or {@code /summon} there
     * never despawns. The call is kept only for {@code CatSpawner.spawnInHut} ({@link CatHutSpawn}); the hut piece sets
     * persistence itself in both versions.
     */
    @WrapOperation(
        method = "finalizeSpawn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Cat;setPersistenceRequired()V")
    )
    private void vbp$noPersistenceFromSpawn(Cat cat, Operation<Void> original) {
        if (CatHutSpawn.active()) {
            original.call(cat);
        }
    }
}
