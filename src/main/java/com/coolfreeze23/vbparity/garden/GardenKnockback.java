package com.coolfreeze23.vbparity.garden;

/**
 * TPHYS-C06: native Creaking overrides only the 5-argument knockback(power, x, z, source, damage), used by the default
 * hurt knockback, the sweep attack and goat rams. The extra knockback (enchantment, sprint, mob attack knockback, spear
 * stabs) goes through the 6-argument overload and still moves a frozen creaking. 1.21.1 has one knockback(DDD) for all
 * of them, so the call sites native blocks are marked while they run.
 */
public final class GardenKnockback {
    private static final ThreadLocal<int[]> DEFAULT = ThreadLocal.withInitial(() -> new int[1]);

    private GardenKnockback() {
    }

    public static void enter() {
        DEFAULT.get()[0]++;
    }

    public static void exit() {
        DEFAULT.get()[0]--;
    }

    public static boolean isDefaultKnockback() {
        return DEFAULT.get()[0] > 0;
    }
}
