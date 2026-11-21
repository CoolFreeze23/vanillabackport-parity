package com.coolfreeze23.vbparity.data.mixin;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootTable.class)
public interface LootTableAccessor {
    @Accessor("randomSequence")
    Optional<ResourceLocation> vbparity$data$getRandomSequence();

    @Mutable
    @Accessor("randomSequence")
    void vbparity$data$setRandomSequence(Optional<ResourceLocation> sequence);

    @Accessor("pools")
    List<LootPool> vbparity$data$getPools();
}
