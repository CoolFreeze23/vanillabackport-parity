package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.blocks.DriedGhastBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DriedGhastBlock.class)
public abstract class DriedGhastBlockMixin {
    /**
     * GHAST-21: the dried ghast is forceSolidOn. The flag is read when block state caches are built, which happens
     * after construction, so setting it on the block's own Properties here is in time.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void vbp$forceSolidOn(BlockBehaviour.Properties properties, CallbackInfo ci) {
        ((BlockBehaviourAccessor) this).vbp$getProperties().forceSolidOn();
    }

    /** GHAST-22: both ambient sounds belong to the Blocks volume slider. */
    @ModifyArg(method = "animateTick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"), index = 4)
    private SoundSource vbp$blocksSoundSource(SoundSource source) {
        return SoundSource.BLOCKS;
    }

    /** GHAST-23: a dry dried ghast puffs white smoke. */
    @Redirect(method = "animateTick", at = @At(value = "FIELD",
        target = "Lnet/minecraft/core/particles/ParticleTypes;SMOKE:Lnet/minecraft/core/particles/SimpleParticleType;"))
    private SimpleParticleType vbp$whiteSmoke() {
        return ParticleTypes.WHITE_SMOKE;
    }

    /** GHAST-24: only a water source waterlogs the placed block; falling flowing water does not. */
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void vbp$waterlogOnlyInSource(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null) {
            boolean source = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
            cir.setReturnValue(state.setValue(DriedGhastBlock.WATERLOGGED, source));
        }
    }
}
