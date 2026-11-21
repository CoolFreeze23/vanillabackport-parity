package com.coolfreeze23.vbparity.wild;

import com.blackgear.vanillabackport.core.VanillaBackport;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** The VanillaBackport common-config toggles that Wilderness Bound data (worldgen switches, loot conditions) can read. */
public final class WildConfig {
    private static final Map<String, BooleanSupplier> KEYS = Map.of(
        "dappled_forest", () -> VanillaBackport.COMMON_CONFIG.hasDappledForest.get(),
        "fallen_trees", () -> VanillaBackport.COMMON_CONFIG.hasFallenTrees.get(),
        "leaf_litter", () -> VanillaBackport.COMMON_CONFIG.hasLeafLitter.get(),
        "copper_tool_set", () -> VanillaBackport.COMMON_CONFIG.hasCopperToolSet.get(),
        "spears", () -> VanillaBackport.COMMON_CONFIG.hasSpears.get()
    );

    public static final Codec<String> KEY_CODEC = Codec.STRING.validate(
        key -> KEYS.containsKey(key) ? DataResult.success(key) : DataResult.error(() -> "Unknown VanillaBackport config key: " + key)
    );

    /**
     * Set at common setup. Vanilla builds the overworld biome table once during bootstrap, before mods and their configs
     * load (VB adds nothing then either), so the dappled forest swap only turns on from common setup.
     */
    private static volatile boolean dappledForestPlacement;

    private WildConfig() {
    }

    public static boolean isOn(String key) {
        return KEYS.get(key).getAsBoolean();
    }

    public static boolean dappledForestPlacement() {
        return dappledForestPlacement;
    }

    static void onCommonSetup() {
        dappledForestPlacement = VanillaBackport.COMMON_CONFIG.hasDappledForest.get();
    }
}
