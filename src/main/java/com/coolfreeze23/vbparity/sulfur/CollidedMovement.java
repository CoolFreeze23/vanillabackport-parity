package com.coolfreeze23.vbparity.sulfur;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Implemented by Entity (EntityBounceMixin): the delta-movement before and the movement after this move's collide. */
public interface CollidedMovement {
    @Nullable
    Vec3 vbparity$movementBeforeCollide();

    @Nullable
    Vec3 vbparity$collidedMovement();
}
