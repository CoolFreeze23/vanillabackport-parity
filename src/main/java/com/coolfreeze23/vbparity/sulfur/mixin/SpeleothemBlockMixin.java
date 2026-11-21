package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.blocks.SpeleothemBlock;
import com.blackgear.vanillabackport.core.data.tags.ModBlockTags;
import com.coolfreeze23.vbparity.sulfur.SulfurSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** VB's SpeleothemBlock is only used by sulfur_spike, so these match native SulfurSpikeBlock + SpeleothemBlock. */
@Mixin(SpeleothemBlock.class)
public abstract class SpeleothemBlockMixin {
    /** SULFUR-30. Native sulfur spike sound type (SoundType.SULFUR_SPIKE) instead of pointed dripstone. */
    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V")
    )
    private static BlockBehaviour.Properties vbp$sulfurSpikeSounds(BlockBehaviour.Properties properties) {
        return properties.sound(SulfurSounds.SULFUR_SPIKE);
    }

    /** SULFUR-21. SulfurSpikeBlock#getMaxGrowthLength is 2 (VB searches 7 like dripstone). */
    @ModifyConstant(method = "tryGrowStalactiteOrStalagmiteIfPossible", constant = @Constant(intValue = 7))
    private int vbp$maxGrowthLength(int length) {
        return 2;
    }

    /** SULFUR-22. Native keeps scanning while blocksStalagmiteScan is false (VB's check is inverted, so it stops after one block). */
    @Redirect(
        method = "growStalagmiteBelow",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/blocks/SpeleothemBlock;blocksStalagmiteScan(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean vbp$keepScanning(SpeleothemBlock self, BlockGetter level, BlockPos pos, BlockState state) {
        return true;
    }

    /** SULFUR-23. Native also requires Projectile#mayBreak (impact_projectiles + projectilesCanBreakBlocks). */
    @Inject(method = "onProjectileHit", at = @At("HEAD"), cancellable = true)
    private void vbp$respectMayBreak(Level level, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        if (!level.isClientSide() && !projectile.mayBreak(level)) {
            ci.cancel();
        }
    }

    /** SULFUR-24. Native merges tips only with the same block; an opposing tip of another speleothem leaves this one a TIP. */
    @Inject(method = "calculateSpeleothemThickness", at = @At("HEAD"), cancellable = true)
    private void vbp$mergeOnlyWithSameBlock(
        LevelReader level, BlockPos pos, Direction tipDirection, boolean merge, CallbackInfoReturnable<DripstoneThickness> cir
    ) {
        BlockState inFront = level.getBlockState(pos.relative(tipDirection));
        if (inFront.is(ModBlockTags.SPELEOTHEMS)
            && inFront.hasProperty(SpeleothemBlock.TIP_DIRECTION)
            && inFront.getValue(SpeleothemBlock.TIP_DIRECTION) == tipDirection.getOpposite()
            && !inFront.is((SpeleothemBlock) (Object) this)) {
            cir.setReturnValue(DripstoneThickness.TIP);
        }
    }

    /**
     * SULFUR-31 / TBLK-301. Native SulfurSpikeBlock.getStalactiteLandingSound() is level event 1052 (free in 1.21.1);
     * the client plays block.sulfur_spike.land at volume 2.0, pitch 0.9-1.0 from its own random (LevelRendererMixin),
     * so the server RNG is untouched and the 64-block level-event range applies.
     */
    @Redirect(method = "onBrokenAfterFall", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void vbp$sulfurSpikeLandSound(Level level, int type, BlockPos pos, int data) {
        level.levelEvent(SulfurSounds.SULFUR_SPIKE_LAND_EVENT, pos, data);
    }
}
