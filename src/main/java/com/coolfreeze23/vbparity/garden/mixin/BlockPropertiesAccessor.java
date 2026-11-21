package com.coolfreeze23.vbparity.garden.mixin;

import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.Properties.class)
public interface BlockPropertiesAccessor {
    @Accessor("mapColor")
    void vbparity$garden$setMapColor(Function<BlockState, MapColor> mapColor);
}
