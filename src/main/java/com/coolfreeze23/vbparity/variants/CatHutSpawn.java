package com.coolfreeze23.vbparity.variants;

import java.util.function.IntSupplier;

/** Marks the stretch of {@code CatSpawner.spawnInHut}, the one 1.21.1 cat spawn that natively makes the cat persistent. */
public final class CatHutSpawn {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private CatHutSpawn() {
    }

    public static boolean active() {
        return DEPTH.get() > 0;
    }

    public static int run(IntSupplier spawn) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            return spawn.getAsInt();
        } finally {
            DEPTH.set(DEPTH.get() - 1);
        }
    }
}
