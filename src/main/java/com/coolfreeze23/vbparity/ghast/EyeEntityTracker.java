package com.coolfreeze23.vbparity.ghast;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;

/**
 * An entity tracker whose target block is the block at the tracked entity's eyes, like the current game's
 * {@code EntityTracker(entity, trackEyeHeight, targetEyeHeight = true)}. Ghastlings fly toward the eyes of the
 * player or adult they follow instead of toward its feet.
 */
public class EyeEntityTracker extends EntityTracker {
    private final Entity target;

    public EyeEntityTracker(Entity entity, boolean trackEyeHeight) {
        super(entity, trackEyeHeight);
        this.target = entity;
    }

    @Override
    public BlockPos currentBlockPosition() {
        return BlockPos.containing(this.target.getEyePosition());
    }

    @Override
    public String toString() {
        return "EyeEntityTracker for " + this.target;
    }
}
