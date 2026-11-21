package com.coolfreeze23.vbparity.copper;

/** COPPER-19: marks the armor roll in Mob.populateDefaultEquipmentSlots, where VB's 40% copper swap must not run. */
public final class CopperEquipment {
    private static final ThreadLocal<int[]> ROLL = ThreadLocal.withInitial(() -> new int[1]);

    private CopperEquipment() {
    }

    public static void enter() {
        ROLL.get()[0]++;
    }

    public static void exit() {
        int[] depth = ROLL.get();
        if (depth[0] > 0) {
            depth[0]--;
        }
    }

    public static boolean inRoll() {
        return ROLL.get()[0] > 0;
    }
}
