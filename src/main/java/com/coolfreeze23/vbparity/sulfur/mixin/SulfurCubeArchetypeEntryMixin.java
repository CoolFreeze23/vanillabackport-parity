package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCubeArchetype;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** SULFUR-16. Native modifier ids are {@code <archetype>_add_<attribute path>} / {@code _mul_}, without the attribute namespace (e.g. regular_add_knockback_resistance). */
@Mixin(SulfurCubeArchetype.AttributeEntry.class)
public abstract class SulfurCubeArchetypeEntryMixin {
    @ModifyArg(
        method = {"add", "multiply"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"),
        index = 0
    )
    private static String vbp$nativeModifierId(String id) {
        // VB: <archetype>_add_minecraft_generic.knockback_resistance; 1.21.1 vanilla attribute paths carry the "generic." prefix
        // that 26.2 dropped, so it is removed as well to give the native id.
        return id.replace("_add_minecraft_", "_add_").replace("_mul_minecraft_", "_mul_").replace("_add_generic.", "_add_").replace("_mul_generic.", "_mul_");
    }
}
