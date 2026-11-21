package com.coolfreeze23.vbparity.misc.mixin;

import com.coolfreeze23.vbparity.misc.MiscContent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native VillageSiege.trySpawn builds its zombie with {@code new Zombie(level)}, without the Peaceful check (the zombie
 * then despawns on its first tick). With monster spawning on in Peaceful, the 1.21.1 {@code EntityType.ZOMBIE.create}
 * would return null there and log "Failed to create zombie for village siege" for every attempt.
 */
@Mixin(VillageSiege.class)
public abstract class VillageSiegePeacefulMixin {
    @WrapOperation(method = "trySpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"))
    private Entity vbp$createLikeNative(EntityType<?> type, Level level, Operation<Entity> original) {
        MiscContent.IGNORE_PEACEFUL_CHECK.set(true);
        try {
            return original.call(type, level);
        } finally {
            MiscContent.IGNORE_PEACEFUL_CHECK.set(false);
        }
    }
}
