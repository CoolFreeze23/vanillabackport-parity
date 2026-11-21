package com.coolfreeze23.vbparity.variants.mixin;

import com.coolfreeze23.vbparity.variants.CatHutSpawn;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.CatSpawner;
import org.spongepowered.asm.mixin.Mixin;

/** VARIANTS-21: cats {@code CatSpawner} places at witch huts stay persistent, as native {@code spawnCat(..., true)}. */
@Mixin(CatSpawner.class)
public abstract class CatSpawnerMixin {
    @WrapMethod(method = "spawnInHut")
    private int vbp$hutCatsPersist(ServerLevel level, BlockPos pos, Operation<Integer> original) {
        return CatHutSpawn.run(() -> original.call(level, pos));
    }
}
