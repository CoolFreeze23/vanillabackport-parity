package com.coolfreeze23.vbparity.garden.mixin;

import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PathNavigation.class)
public interface PathNavigationAccessor {
    @Accessor("pathFinder")
    PathFinder vbparity$garden$pathFinder();
}
