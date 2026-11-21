package com.coolfreeze23.vbparity.sulfur;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.blackgear.vanillabackport.common.registries.worldgen.ModNoises;
import com.blackgear.vanillabackport.common.worldgen.surface.ModSurfaceRuleData;
import com.coolfreeze23.vbparity.sulfur.mixin.SequenceRuleSourceAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Native placement of the sulfur cave bands (SurfaceRuleData.overworldLike): floor and under-floor positions near the
 * preliminary surface get bands-or-stone at the place the native main rule would reach them, and the plain bands come right
 * after the main rule, before the deepslate gradient. No extra bedrock gradient.
 */
public final class SulfurSurfaceRules {
    private SulfurSurfaceRules() {
    }

    public static boolean terraBlenderLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("terrablender");
    }

    /** The VB dappled forest rule from {@link ModSurfaceRuleData#makeRules()}, without VB's sulfur cave block. */
    @Nullable
    private static RuleSource dappledOnly(List<RuleSource> rules) {
        if (rules.size() != 1 || !(rules.get(0) instanceof SequenceRuleSourceAccessor vbSequence) || vbSequence.vbp$sequence().size() != 2) {
            return null;
        }
        return vbSequence.vbp$sequence().get(0);
    }

    /**
     * Rebuilds VB's merged rule with native sulfur placement. Returns null when the shape of the original rule is not the
     * vanilla overworld-like one (then VB's own merge is kept).
     */
    @Nullable
    public static RuleSource merge(RuleSource original, List<RuleSource> rules) {
        RuleSource dappled = dappledOnly(rules);
        if (dappled == null || !(original instanceof SequenceRuleSourceAccessor sequence)) {
            return null;
        }
        List<RuleSource> seq = sequence.vbp$sequence();
        int mainIndex;
        boolean preliminaryCheck;
        if (seq.size() == 3) {
            // overworld / large_biomes / amplified: [bedrock_floor, ifTrue(abovePreliminarySurface, main), deepslate]
            mainIndex = 1;
            preliminaryCheck = true;
        } else if (seq.size() == 4) {
            // caves: [bedrock_roof, bedrock_floor, main, deepslate]
            mainIndex = 2;
            preliminaryCheck = false;
        } else {
            return null;
        }

        RuleSource stone = SurfaceRules.state(Blocks.STONE.defaultBlockState());
        RuleSource cinnabar = SurfaceRules.state(ModBlocks.CINNABAR.get().defaultBlockState());
        RuleSource sulfur = SurfaceRules.state(ModBlocks.SULFUR.get().defaultBlockState());
        RuleSource bands = SurfaceRules.sequence(
            SurfaceRules.ifTrue(ModSurfaceRuleData.noiseCondition3d(ModNoises.SULFUR_CAVE_GRADIENT, -0.4F, -0.1F), cinnabar),
            SurfaceRules.ifTrue(ModSurfaceRuleData.noiseCondition3d(ModNoises.SULFUR_CAVE_GRADIENT, 0.0, 0.4F), sulfur),
            SurfaceRules.ifTrue(ModSurfaceRuleData.noiseCondition3d(ModNoises.SULFUR_CAVE_GRADIENT, 0.4F), cinnabar)
        );
        ConditionSource isSulfurCaves = SurfaceRules.isBiome(ModBiomes.SULFUR_CAVES);
        RuleSource bandsOrStone = SurfaceRules.sequence(bands, stone);
        RuleSource closeToSurface = SurfaceRules.ifTrue(
            isSulfurCaves,
            SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), bandsOrStone)),
                SurfaceRules.ifTrue(SurfaceRules.waterStartCheck(-6, -1), SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, bandsOrStone))
            )
        );

        List<RuleSource> out = new ArrayList<>();
        out.add(dappled);
        out.addAll(seq.subList(0, mainIndex));
        out.add(preliminaryCheck ? SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), closeToSurface) : closeToSurface);
        out.add(seq.get(mainIndex));
        out.add(SurfaceRules.ifTrue(isSulfurCaves, bands));
        out.addAll(seq.subList(mainIndex + 1, seq.size()));
        return SurfaceRules.sequence(out.toArray(RuleSource[]::new));
    }
}
