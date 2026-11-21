package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.worldgen.surface.ModSurfaceRuleData;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * WILD-02. Native: the dappled forest coarse-dirt rule sits in overworld, above_preliminary_surface, surface, on_floor,
 * not_underwater (water -1, x0), biome_surface. VB runs it on every block of the column. This changes the rule object VB
 * builds in makeRules (ordinal 0 = the inner noise ifTrue), so it works with any merge of it (VB's own, the sulfur area's
 * rebuilt merge, TerraBlender). All conditions are pure, so nesting them inside the noise check matches native's order.
 * The native rules before biome_surface (badlands, frozen oceans) never match the dappled forest.
 */
@Mixin(ModSurfaceRuleData.class)
public abstract class ModSurfaceRuleDataMixin {
    @ModifyArg(
        method = "makeRules",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/SurfaceRules;ifTrue(Lnet/minecraft/world/level/levelgen/SurfaceRules$ConditionSource;Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;)Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;",
            ordinal = 0
        ),
        index = 1
    )
    private static SurfaceRules.RuleSource vbparity$wild$coarseDirtOnFloorOnly(SurfaceRules.RuleSource coarseDirt) {
        return SurfaceRules.ifTrue(
            SurfaceRules.abovePreliminarySurface(),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), coarseDirt))
        );
    }
}
