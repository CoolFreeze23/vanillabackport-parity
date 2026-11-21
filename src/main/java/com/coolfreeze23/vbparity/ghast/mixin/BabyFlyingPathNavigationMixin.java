package com.coolfreeze23.vbparity.ghast.mixin;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * GHAST-11: the ghastling navigation has a required path length of 48, as in the current game: paths may be
 * max(follow range, 48) long and the path finder visits floor(that * 16) nodes.
 */
@Mixin(targets = "com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast$BabyFlyingPathNavigation")
public abstract class BabyFlyingPathNavigationMixin extends FlyingPathNavigation {
    private static final float VBP$REQUIRED_PATH_LENGTH = 48.0F;

    protected BabyFlyingPathNavigationMixin(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        return super.createPathFinder(Math.max(maxVisitedNodes, (int) (VBP$REQUIRED_PATH_LENGTH * 16.0F)));
    }

    @Nullable
    @Override
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy) {
        float maxPathLength = Math.max((float) this.mob.getAttributeValue(Attributes.FOLLOW_RANGE), VBP$REQUIRED_PATH_LENGTH);
        return this.createPath(targets, regionOffset, offsetUpward, accuracy, maxPathLength);
    }

    /**
     * Native PathNavigation keeps maxVisitedNodes at floor(max(FOLLOW_RANGE, requiredPathLength) * 16) and updates it
     * when the follow range changes (Mob.onAttributeUpdated). Setting it before each search has the same effect.
     */
    @Nullable
    @Override
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange) {
        float maxPathLength = Math.max((float) this.mob.getAttributeValue(Attributes.FOLLOW_RANGE), VBP$REQUIRED_PATH_LENGTH);
        ((PathFinderAccessor) ((PathNavigationAccessor) this).vbp$getPathFinder()).vbp$setMaxVisitedNodes(Mth.floor(maxPathLength * 16.0F));
        return super.createPath(targets, regionOffset, offsetUpward, accuracy, followRange);
    }
}
