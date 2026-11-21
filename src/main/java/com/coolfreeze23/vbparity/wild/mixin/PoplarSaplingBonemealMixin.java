package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TBLK-352. Native 26.3 SaplingBlock.isValidBonemealTarget for the poplar sapling (the only Wilderness Bound sapling):
 * on the server, TreeGrower.canGrow rolls the tree type (poplar: nextInt(3) from the level random; no mega trees) and
 * the bone meal is refused when pos.above(red_poplar trunk base height) is outside the build height; on the client it is
 * false. Vanilla saplings keep 1.21.1's behaviour (native's change there is engine-wide, not backported).
 */
@Mixin(SaplingBlock.class)
public abstract class PoplarSaplingBonemealMixin {
    private static final ResourceLocation SHORTEST_POPLAR = ResourceLocation.withDefaultNamespace("red_poplar");

    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$poplarBonemealTarget(LevelReader level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!state.is(ModBlocks.POPLAR_SAPLING.get())) {
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            cir.setReturnValue(false);
            return;
        }
        server.getRandom().nextInt(3);
        int base = 0;
        ConfiguredFeature<?, ?> feature = server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(SHORTEST_POPLAR);
        if (feature != null && feature.config() instanceof TreeConfiguration tree) {
            base = ((TrunkPlacerAccessor) tree.trunkPlacer).vbparity$baseHeight();
        }
        cir.setReturnValue(!level.isOutsideBuildHeight(pos.above(base)));
    }
}
