package com.coolfreeze23.vbparity.sulfur.mixin;

import com.coolfreeze23.vbparity.sulfur.SharedPropertiesGuard;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** See {@link SharedPropertiesGuard}: only VB's shared builders are swapped for a private copy; every other block is untouched. */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
    @ModifyVariable(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER), argsOnly = true)
    private BlockBehaviour.Properties vbp$privateSharedProperties(BlockBehaviour.Properties properties) {
        return SharedPropertiesGuard.privateCopy(properties);
    }
}
