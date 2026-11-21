package com.coolfreeze23.vbparity.misc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Client-only helpers called from shared code (only on the client side). */
public final class MiscClientHooks {
    private MiscClientHooks() {}

    /** Native LocatorBar partial tick: {@code deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity))}. */
    public static float entityPartialTick(Entity entity) {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(!entity.level().tickRateManager().isEntityFrozen(entity));
    }
}
