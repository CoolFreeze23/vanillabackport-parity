package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.blocks.EyeblossomBlock;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EyeblossomBlock.class)
public abstract class EyeblossomBlockMixin {
    /** GARDEN-11: native plays block.eyeblossom.idle in the AMBIENT category. */
    @ModifyArg(
        method = "animateTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"),
        index = 4
    )
    private SoundSource vbparity$garden$ambientIdle(SoundSource source) {
        return SoundSource.AMBIENT;
    }

    /**
     * GARDEN-14: native reads the EYEBLOSSOM_OPEN environment attribute, which only the overworld day timeline sets;
     * elsewhere it is DEFAULT and the flower keeps its current state.
     */
    @Inject(method = "tryChangingState", at = @At("HEAD"), cancellable = true)
    private void vbparity$garden$onlyNaturalDimensions(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (!level.dimensionType().natural()) {
            cir.setReturnValue(false);
        }
    }

    /** GARDEN-15: native only poisons bees inside a bee-attractive eyeblossom, which is the open one. */
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void vbparity$garden$poisonOnlyWhenOpen(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!state.is(ModBlocks.OPEN_EYEBLOSSOM.get())) {
            ci.cancel();
        }
    }
}
