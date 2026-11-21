package com.coolfreeze23.vbparity.wild.mixin;

import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockAttachedEntity.class)
public interface BlockAttachedEntityAccessor {
    @Accessor("checkInterval")
    int vbparity$wild$getCheckInterval();
}
