package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.blocks.HangingMossBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HangingMossBlock.class)
public abstract class HangingMossBlockMixin {
    /** GARDEN-11: native plays block.pale_hanging_moss.idle in the AMBIENT category. */
    @ModifyArg(
        method = "animateTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"),
        index = 4
    )
    private SoundSource vbparity$garden$ambientIdle(SoundSource source) {
        return SoundSource.AMBIENT;
    }

    /** GARDEN-18: native also requires the growth position to be inside the build height. */
    @Inject(method = "isValidBonemealTarget", at = @At("RETURN"), cancellable = true)
    private void vbparity$garden$insideBuildHeight(LevelReader level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && level.isOutsideBuildHeight(((HangingMossBlock) (Object) this).getTip(level, pos).below())) {
            cir.setReturnValue(false);
        }
    }
}
