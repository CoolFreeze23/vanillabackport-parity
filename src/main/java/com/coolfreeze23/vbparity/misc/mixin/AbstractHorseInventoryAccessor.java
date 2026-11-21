package com.coolfreeze23.vbparity.misc.mixin;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractHorse.class)
public interface AbstractHorseInventoryAccessor {
    @Accessor("inventory")
    SimpleContainer vbp$inventory();
}
