package com.coolfreeze23.vbparity.ai;

/**
 * AI-VAR-01 / SPAWN-11: native cows, pigs and chickens pick their variant and sound variant before super.finalizeSpawn
 * (before the baby, follow-range and handedness rolls). VB's pick runs from Mob.finalizeSpawn RETURN; the add-on calls
 * it early instead and lets only that early call through.
 */
public final class VariantOrder {
    private static final ThreadLocal<Boolean> EARLY = ThreadLocal.withInitial(() -> false);

    private VariantOrder() {
    }

    public static boolean early() {
        return EARLY.get();
    }

    public static void runEarly(Runnable pick) {
        boolean previous = EARLY.get();
        EARLY.set(true);
        try {
            pick.run();
        } finally {
            EARLY.set(previous);
        }
    }
}
