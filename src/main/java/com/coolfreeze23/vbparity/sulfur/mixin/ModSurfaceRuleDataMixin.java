package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.worldgen.surface.ModSurfaceRuleData;
import com.coolfreeze23.vbparity.sulfur.SulfurSurfaceRules;
import java.util.List;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SULFUR-45. VB prepends a whole sulfur-caves block (own "bedrock" gradient, bands, STONE everywhere above the preliminary
 * surface) to the overworld rule. Rebuild it with native placement instead; see {@link SulfurSurfaceRules#merge}.
 * The TerraBlender path (makeRules through SurfaceRuleManager) is left as VB has it.
 */
@Mixin(ModSurfaceRuleData.class)
public abstract class ModSurfaceRuleDataMixin {
    @Inject(method = "merge", at = @At("HEAD"), cancellable = true)
    private static void vbp$nativeSulfurPlacement(
        SurfaceRules.RuleSource original, List<SurfaceRules.RuleSource> rules, CallbackInfoReturnable<SurfaceRules.RuleSource> cir
    ) {
        if (SulfurSurfaceRules.terraBlenderLoaded()) {
            return;
        }
        SurfaceRules.RuleSource rebuilt = SulfurSurfaceRules.merge(original, rules);
        if (rebuilt != null) {
            cir.setReturnValue(rebuilt);
        }
    }
}
