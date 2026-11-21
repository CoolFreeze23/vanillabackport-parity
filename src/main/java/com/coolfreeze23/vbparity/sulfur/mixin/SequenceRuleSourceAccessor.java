package com.coolfreeze23.vbparity.sulfur.mixin;

import java.util.List;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$SequenceRuleSource")
public interface SequenceRuleSourceAccessor {
    @Accessor("sequence")
    List<SurfaceRules.RuleSource> vbp$sequence();
}
