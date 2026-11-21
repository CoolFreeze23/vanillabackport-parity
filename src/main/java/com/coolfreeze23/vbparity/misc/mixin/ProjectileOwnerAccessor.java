package com.coolfreeze23.vbparity.misc.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(Projectile.class)
public interface ProjectileOwnerAccessor {
    @Nullable
    @Accessor("ownerUUID")
    UUID vbp$getOwnerUUID();

    @Accessor("ownerUUID")
    void vbp$setOwnerUUID(@Nullable UUID uuid);
}
