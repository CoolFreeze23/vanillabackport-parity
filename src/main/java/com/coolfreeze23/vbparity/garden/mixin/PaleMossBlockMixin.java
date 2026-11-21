package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.blocks.PaleMossBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import org.spongepowered.asm.mixin.Mixin;

/** GARDEN-17: native pale moss is a BonemealableFeaturePlacerBlock, whose type is NEIGHBOR_SPREADER. */
@Mixin(PaleMossBlock.class)
public abstract class PaleMossBlockMixin implements BonemealableBlock {
    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}
