package com.coolfreeze23.vbparity.flora.worldgen;

import com.blackgear.vanillabackport.core.VanillaBackport;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** The VanillaBackport common-config toggles that flora worldgen data can be switched on. */
public final class VbConfigGate {
    private static final Map<String, BooleanSupplier> KEYS = Map.of(
        "fallen_trees", () -> VanillaBackport.COMMON_CONFIG.hasFallenTrees.get(),
        "leaf_litter", () -> VanillaBackport.COMMON_CONFIG.hasLeafLitter.get(),
        "cactus_flowers", () -> VanillaBackport.COMMON_CONFIG.hasCactusFlowers.get(),
        "bushes", () -> VanillaBackport.COMMON_CONFIG.hasBushes.get(),
        "wildflowers", () -> VanillaBackport.COMMON_CONFIG.hasWildflowers.get()
    );

    public static final Codec<String> KEY_CODEC = Codec.STRING.validate(
        key -> KEYS.containsKey(key) ? DataResult.success(key) : DataResult.error(() -> "Unknown VanillaBackport config key: " + key)
    );

    public static boolean isOn(String key) {
        return KEYS.get(key).getAsBoolean();
    }

    private VbConfigGate() {}
}
