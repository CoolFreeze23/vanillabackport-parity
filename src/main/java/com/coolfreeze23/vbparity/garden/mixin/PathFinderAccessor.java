package com.coolfreeze23.vbparity.garden.mixin;

import net.minecraft.world.level.pathfinder.PathFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PathFinder.class)
public interface PathFinderAccessor {
    @Accessor("maxVisitedNodes")
    int vbparity$garden$maxVisitedNodes();

    @Mutable
    @Accessor("maxVisitedNodes")
    void vbparity$garden$setMaxVisitedNodes(int nodes);
}
