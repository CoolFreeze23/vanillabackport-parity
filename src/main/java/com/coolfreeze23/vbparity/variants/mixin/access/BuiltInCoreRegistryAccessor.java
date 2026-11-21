package com.coolfreeze23.vbparity.variants.mixin.access;

import com.blackgear.platform.core.BuiltInCoreRegistry;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** VARIANTS-22: reads the datapack-loaded entries of a Platform registry, which are the ones a client lacks. */
@Mixin(value = BuiltInCoreRegistry.class, remap = false)
public interface BuiltInCoreRegistryAccessor {
    @Accessor("dataDriven")
    Map<ResourceLocation, Object> vbp$getDataDriven();
}
