package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.NautilusAi;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilusAi;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * AI-MNT-01: native Brain registers every memory its behaviours use, so FollowTemptation's IS_TEMPTED exists for the
 * nautilus and zombie nautilus. 1.21.1 only registers listed memories, and VB's list lacks IS_TEMPTED, so the tempt
 * behaviour could never start.
 */
@Mixin(value = {NautilusAi.class, ZombieNautilusAi.class}, remap = false)
public abstract class NautilusBrainMemoryMixin {
    @ModifyArg(
        method = "brainProvider",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;provider(Ljava/util/Collection;Ljava/util/Collection;)Lnet/minecraft/world/entity/ai/Brain$Provider;", remap = true),
        index = 0
    )
    private static Collection<? extends MemoryModuleType<?>> vbp$withIsTempted(Collection<? extends MemoryModuleType<?>> memories) {
        if (memories.contains(MemoryModuleType.IS_TEMPTED)) {
            return memories;
        }
        return ImmutableList.<MemoryModuleType<?>>builder().addAll(memories).add(MemoryModuleType.IS_TEMPTED).build();
    }
}
