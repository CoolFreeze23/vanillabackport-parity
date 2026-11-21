package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.blocks.SpeleothemBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpeleothemBlock.class)
public interface SpeleothemBlockInvoker {
    @Invoker("calculateSpeleothemThickness")
    DripstoneThickness vbp$calculateSpeleothemThickness(LevelReader level, BlockPos pos, Direction tipDirection, boolean merge);

    @Invoker("growStalagmiteBelow")
    void vbp$growStalagmiteBelow(ServerLevel level, BlockPos posAboveStalagmite);
}
