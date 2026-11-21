package com.coolfreeze23.vbparity.ai.mixin;

import net.minecraft.world.entity.ai.sensing.Sensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Sensor.class)
public interface SensorAccessor {
    @Accessor("scanRate")
    int vbp$scanRate();

    @Accessor("timeToTick")
    long vbp$timeToTick();

    @Accessor("timeToTick")
    void vbp$setTimeToTick(long timeToTick);
}
