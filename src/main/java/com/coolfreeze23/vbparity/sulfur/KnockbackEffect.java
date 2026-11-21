package com.coolfreeze23.vbparity.sulfur;

/**
 * Native knockback(..., comesFromEffect) flag (AI-SLF-01). 1.21.1 knockback has no such argument, so the callers that
 * native marks as an extra knockback (Player.attack, Mob.doHurtTarget and the second stab push) set it around their
 * call. Only SulfurCube.knockback reads it: power * 0.25 scales the archetype push of an extra knockback.
 */
public final class KnockbackEffect {
    private static final ThreadLocal<Boolean> FROM_EFFECT = ThreadLocal.withInitial(() -> false);

    private KnockbackEffect() {
    }

    public static boolean fromEffect() {
        return FROM_EFFECT.get();
    }

    public static void run(boolean fromEffect, Runnable call) {
        boolean previous = FROM_EFFECT.get();
        FROM_EFFECT.set(fromEffect);
        try {
            call.run();
        } finally {
            FROM_EFFECT.set(previous);
        }
    }
}
