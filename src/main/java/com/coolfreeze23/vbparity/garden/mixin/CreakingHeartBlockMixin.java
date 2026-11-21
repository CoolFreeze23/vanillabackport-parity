package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.block_entities.CreakingHeartBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GARDEN-05: native CreakingHeartBlockEntity.preRemoveSideEffects calls removeProtector(null) whenever the heart is
 * removed or replaced, so its creaking crumbles at once. After playerWillDestroy or an explosion the protector is
 * already cleared and this is a no-op.
 */
@Mixin(CreakingHeartBlock.class)
public abstract class CreakingHeartBlockMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void vbparity$garden$tearDownOnRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity heart) {
            heart.removeProtector(null);
        }
    }
}
