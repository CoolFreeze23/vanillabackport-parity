package com.coolfreeze23.vbparity.ghast.mixin;

import net.minecraft.world.level.pathfinder.PathFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PathFinder.class)
public interface PathFinderAccessor {
    @Accessor("maxVisitedNodes")
    int vbp$getMaxVisitedNodes();

    @Accessor("maxVisitedNodes")
    @Mutable
    void vbp$setMaxVisitedNodes(int maxVisitedNodes);
}
