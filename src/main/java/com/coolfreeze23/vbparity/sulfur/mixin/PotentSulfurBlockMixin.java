package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.block_entities.PotentSulfurBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.PotentSulfurBlock;
import com.blackgear.vanillabackport.common.level.blocks.PotentSulfurState;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.sulfur.GeyserLaunch;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** SULFUR-25, client half: native also runs the launch ticker on the client while a geyser erupts. */
@Mixin(PotentSulfurBlock.class)
public abstract class PotentSulfurBlockMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyReturnValue(method = "getTicker", at = @At("RETURN"))
    private BlockEntityTicker vbp$clientLaunch(BlockEntityTicker ticker, Level level, BlockState state, BlockEntityType type) {
        if (ticker == null || !level.isClientSide() || !VanillaBackport.COMMON_CONFIG.doGeysersErupt.get()) {
            return ticker;
        }
        PotentSulfurState geyser = state.getValue(PotentSulfurBlock.STATE);
        if (geyser == PotentSulfurState.ERUPTING || geyser == PotentSulfurState.CONTINUOUS) {
            return PotentSulfurBlockEntity.sequence((BlockEntityTicker<PotentSulfurBlockEntity>) ticker, GeyserLaunch.CLIENT_LAUNCH_TICKER);
        }
        return ticker;
    }
}
