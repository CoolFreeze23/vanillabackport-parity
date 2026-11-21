package com.coolfreeze23.vbparity.misc.mixin;

import com.coolfreeze23.vbparity.misc.MiscContent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native Mob.checkDespawn: on Peaceful, a mob despawns exactly when its type is not allowed in Peaceful. */
@Mixin(Mob.class)
public abstract class MobPeacefulMixin {
    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;shouldDespawnInPeaceful()Z"))
    private boolean vbp$despawnInPeaceful(Mob mob, Operation<Boolean> original) {
        if (mob.getType().is(MiscContent.MONSTERS_THAT_SPAWN_ON_PEACEFUL)) {
            return false;
        }
        return original.call(mob) || MiscContent.notAllowedInPeaceful(mob.getType());
    }
}
