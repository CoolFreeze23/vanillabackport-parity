package com.coolfreeze23.vbparity.sulfur.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityBodyAccessor {
    @Accessor("lastBodyItemStack")
    ItemStack vbp$lastBodyItemStack();
}
