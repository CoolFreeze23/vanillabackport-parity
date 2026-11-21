package com.coolfreeze23.vbparity.sulfur.mixin;

import com.coolfreeze23.vbparity.sulfur.PropertiesCopy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;

/** Gives the block properties builder a full copy, so one shared builder can be snapshotted per registration. */
@Mixin(BlockBehaviour.Properties.class)
public abstract class BlockPropertiesMixin implements PropertiesCopy {
    @Override
    public BlockBehaviour.Properties vbp$copy() {
        BlockBehaviour.Properties copy = BlockBehaviour.Properties.of();
        try {
            // Runs inside Properties itself, so its own private fields (vanilla and NeoForge-added) are accessible.
            for (Field field : BlockBehaviour.Properties.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    field.set(copy, field.get(this));
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not copy block properties", e);
        }
        return copy;
    }
}
