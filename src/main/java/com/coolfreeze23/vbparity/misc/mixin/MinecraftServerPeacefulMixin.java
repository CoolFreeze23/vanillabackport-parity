package com.coolfreeze23.vbparity.misc.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native MinecraftServer: monster spawning no longer depends on the difficulty (only on the spawn game rules).
 * Which monsters may exist on Peaceful is decided per type (EntityTypePeacefulMixin, MobPeacefulMixin).
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerPeacefulMixin {
    @ModifyReturnValue(method = "isSpawningMonsters", at = @At("RETURN"))
    private boolean vbp$monstersOnPeaceful(boolean original) {
        return true;
    }
}
