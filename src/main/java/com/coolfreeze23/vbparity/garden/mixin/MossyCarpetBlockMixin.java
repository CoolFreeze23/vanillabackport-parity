package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.blocks.MossyCarpetBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** GARDEN-12: native pale moss carpet uses SoundType.MOSS_CARPET (VB registers SoundType.MOSS). */
@Mixin(MossyCarpetBlock.class)
public abstract class MossyCarpetBlockMixin extends Block {
    protected MossyCarpetBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public SoundType getSoundType(BlockState state) {
        return SoundType.MOSS_CARPET;
    }
}
