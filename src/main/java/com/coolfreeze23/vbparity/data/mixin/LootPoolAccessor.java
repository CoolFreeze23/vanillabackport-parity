package com.coolfreeze23.vbparity.data.mixin;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootPool.class)
public interface LootPoolAccessor {
    @Accessor("entries")
    List<LootPoolEntryContainer> vbparity$data$getEntries();

    @Mutable
    @Accessor("entries")
    void vbparity$data$setEntries(List<LootPoolEntryContainer> entries);
}
