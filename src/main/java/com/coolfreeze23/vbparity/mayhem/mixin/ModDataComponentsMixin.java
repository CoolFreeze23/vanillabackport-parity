package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.registries.items.ModDataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * MISC-33: native registers the spear's minimum charge component as minecraft:minimum_attack_charge; VB used
 * minimum_attack_range. The old id stays readable through a registry alias (MayhemParity).
 */
@Mixin(value = ModDataComponents.class, remap = false)
public abstract class ModDataComponentsMixin {
    @ModifyArg(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/registries/items/ModDataComponents;register(Ljava/lang/String;Ljava/util/function/UnaryOperator;)Ljava/util/function/Supplier;"),
        index = 0
    )
    private static String vbp$nativeId(String key) {
        return "minimum_attack_range".equals(key) ? "minimum_attack_charge" : key;
    }
}
