package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolemPathNavigation;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Copper golem navigation as in current vanilla. The golem sets a required path length of 48, so paths may be 48
 * blocks long and the path finder visits up to 48 * 16 nodes (COPPER-01). findSurfacePosition climbs out of solid
 * blocks, and createPath only snaps targets to the surface when asked to (COPPER-02).
 */
@Mixin(CopperGolemPathNavigation.class)
public abstract class CopperGolemPathNavigationMixin extends GroundPathNavigation {
    @Unique
    private static final float vbparity$REQUIRED_PATH_LENGTH = 48.0F;

    private CopperGolemPathNavigationMixin(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        return super.createPathFinder(Math.max(maxVisitedNodes, Mth.floor(vbparity$REQUIRED_PATH_LENGTH * 16.0F)));
    }

    @Nullable
    @Override
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy) {
        float maxPathLength = Math.max((float) this.mob.getAttributeValue(Attributes.FOLLOW_RANGE), vbparity$REQUIRED_PATH_LENGTH);
        return this.createPath(targets, regionOffset, offsetUpward, accuracy, maxPathLength);
    }

    @Shadow
    private boolean canPathToTargetsBelowSurface;

    @Shadow
    private BlockPos findSurfacePosition(LevelChunk chunk, BlockPos pos) {
        throw new AssertionError();
    }

    /**
     * COPPER-02: native GroundPathNavigation.createPath only moves the target to the surface when
     * canPathToTargetsBelowSurface is off, then calls PathNavigation.createPath. VB called the 1.21.1
     * GroundPathNavigation.createPath as super, which always moves a solid target (the chest the golem walks to while
     * transporting) up through every solid block above it.
     */
    @Inject(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
    private void vbparity$createPath(BlockPos pos, int reachRange, CallbackInfoReturnable<Path> cir) {
        LevelChunk chunk = this.level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) {
            cir.setReturnValue(null);
            return;
        }
        if (!this.canPathToTargetsBelowSurface) {
            pos = this.findSurfacePosition(chunk, pos);
        }
        // PathNavigation.createPath(BlockPos, int)
        cir.setReturnValue(this.createPath(ImmutableSet.of(pos), 8, false, reachRange));
    }

    /**
     * Native GroundPathNavigation.findSurfacePosition. VB's upward loop tested {@code >= maxBuildHeight} and never ran.
     * The whole private method is replaced because the bug is a loop condition.
     */
    @Inject(method = "findSurfacePosition", at = @At("HEAD"), cancellable = true)
    private void vbparity$findSurfacePosition(LevelChunk chunk, BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
        int minY = this.level.getMinBuildHeight();
        int maxY = this.level.getMaxBuildHeight() - 1;
        if (chunk.getBlockState(pos).isAir()) {
            BlockPos.MutableBlockPos columnPos = pos.mutable().move(Direction.DOWN);
            while (columnPos.getY() >= minY && chunk.getBlockState(columnPos).isAir()) {
                columnPos.move(Direction.DOWN);
            }
            if (columnPos.getY() >= minY) {
                cir.setReturnValue(columnPos.above());
                return;
            }
            columnPos.setY(pos.getY() + 1);
            while (columnPos.getY() <= maxY && chunk.getBlockState(columnPos).isAir()) {
                columnPos.move(Direction.UP);
            }
            pos = columnPos;
        }

        if (!chunk.getBlockState(pos).isSolid()) {
            cir.setReturnValue(pos);
        } else {
            BlockPos.MutableBlockPos columnPos = pos.mutable().move(Direction.UP);
            while (columnPos.getY() <= maxY && chunk.getBlockState(columnPos).isSolid()) {
                columnPos.move(Direction.UP);
            }
            cir.setReturnValue(columnPos.immutable());
        }
    }
}
