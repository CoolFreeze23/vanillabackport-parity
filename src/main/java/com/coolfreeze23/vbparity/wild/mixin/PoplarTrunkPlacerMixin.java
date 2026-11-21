package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.level.worldgen.tree.trunk.PoplarTrunkPlacer;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WILD-07. Native PoplarTrunkPlacer first calls placeBelowTrunkBlock(origin.below()) with soil_beneath_tree: dirt unless
 * the block is in #cannot_replace_below_tree_trunk (dirt, coarse dirt, rooted dirt, mud, muddy mangrove roots, moss,
 * pale moss, podzol); no random is used. 1.21.1 setDirtAt with VB's dirt_provider (dirt, force_dirt false) keeps exactly
 * those (isDirt = #dirt minus grass and mycelium; VB adds pale moss to #dirt) and turns grass, mycelium, farmland and the
 * rest into dirt, before the trunk's random calls, as native does.
 */
@Mixin(PoplarTrunkPlacer.class)
public abstract class PoplarTrunkPlacerMixin extends TrunkPlacer {
    private PoplarTrunkPlacerMixin(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Inject(method = "placeTrunk", at = @At("HEAD"))
    private void vbparity$wild$soilBeneathTrunk(
        LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, int freeTreeHeight,
        BlockPos pos, TreeConfiguration config, CallbackInfoReturnable<List<FoliagePlacer.FoliageAttachment>> cir
    ) {
        setDirtAt(level, blockSetter, random, pos.below(), config);
    }
}
