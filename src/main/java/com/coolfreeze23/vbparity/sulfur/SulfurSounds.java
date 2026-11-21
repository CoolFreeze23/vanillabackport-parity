package com.coolfreeze23.vbparity.sulfur;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/** Native sulfur spike sound events (registered as minecraft:block.sulfur_spike.*) and its sound type. */
public final class SulfurSounds {
    public static final SoundEvent SULFUR_SPIKE_BREAK = event("block.sulfur_spike.break");
    public static final SoundEvent SULFUR_SPIKE_STEP = event("block.sulfur_spike.step");
    public static final SoundEvent SULFUR_SPIKE_PLACE = event("block.sulfur_spike.place");
    public static final SoundEvent SULFUR_SPIKE_HIT = event("block.sulfur_spike.hit");
    public static final SoundEvent SULFUR_SPIKE_FALL = event("block.sulfur_spike.fall");
    public static final SoundEvent SULFUR_SPIKE_LAND = event("block.sulfur_spike.land");
    /** Native level event id of the sulfur spike landing sound (unused in 1.21.1). */
    public static final int SULFUR_SPIKE_LAND_EVENT = 1052;
    public static final SoundEvent[] ALL = {
        SULFUR_SPIKE_BREAK, SULFUR_SPIKE_STEP, SULFUR_SPIKE_PLACE, SULFUR_SPIKE_HIT, SULFUR_SPIKE_FALL, SULFUR_SPIKE_LAND
    };
    public static final SoundType SULFUR_SPIKE = new SoundType(
        1.0F, 1.0F, SULFUR_SPIKE_BREAK, SULFUR_SPIKE_STEP, SULFUR_SPIKE_PLACE, SULFUR_SPIKE_HIT, SULFUR_SPIKE_FALL
    );

    private SulfurSounds() {
    }

    private static SoundEvent event(String path) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(path));
    }
}
