package com.coolfreeze23.vbparity.misc.mixin;

import com.coolfreeze23.vbparity.misc.MiscContent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Native EntityType.create: {@code canSpawn = isAllowedInPeaceful() || difficulty != PEACEFUL}. Server side only,
 * like native (the client builds spawner display entities with checks ignored).
 */
@Mixin(EntityType.class)
public abstract class EntityTypePeacefulMixin {
    @Inject(method = "create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), cancellable = true)
    private void vbp$notInPeaceful(Level level, CallbackInfoReturnable<Entity> cir) {
        if (!level.isClientSide() && !MiscContent.IGNORE_PEACEFUL_CHECK.get() && level.getDifficulty() == Difficulty.PEACEFUL && MiscContent.notAllowedInPeaceful((EntityType<?>) (Object) this)) {
            cir.setReturnValue(null);
        }
    }
}
