package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SulfurCube.class)
public interface SulfurCubeInvoker {
    @Invoker("equipItem")
    boolean vbp$equipItem(ItemStack stack);
}
