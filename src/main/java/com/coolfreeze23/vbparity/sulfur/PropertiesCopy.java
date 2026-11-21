package com.coolfreeze23.vbparity.sulfur;

import net.minecraft.world.level.block.state.BlockBehaviour;

/** Added to {@link BlockBehaviour.Properties}: a field-for-field copy of the builder. */
public interface PropertiesCopy {
    BlockBehaviour.Properties vbp$copy();

    static BlockBehaviour.Properties copyOf(BlockBehaviour.Properties properties) {
        return ((PropertiesCopy) (Object) properties).vbp$copy();
    }
}
