package com.coolfreeze23.vbparity.garden;

import com.blackgear.vanillabackport.core.VanillaBackport;

/**
 * Worldgen switches read by vanilla-class mixins. Vanilla builds the overworld biome list once during bootstrap
 * validation, before mods load (VB's config and biome keys are not available yet, and VB adds nothing then either),
 * so the pale garden swap only turns on at common setup, the same point VB registers its biome placements.
 */
public final class GardenWorldgen {
    private static volatile boolean paleGardenPlateau;

    private GardenWorldgen() {
    }

    public static boolean paleGardenPlateau() {
        return paleGardenPlateau;
    }

    static void onCommonSetup() {
        paleGardenPlateau = VanillaBackport.COMMON_CONFIG.hasPaleGarden.get();
    }
}
