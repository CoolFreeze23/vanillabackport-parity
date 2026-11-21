package com.coolfreeze23.vbparity.flora.worldgen;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** Places the placed feature that {@link ConfigSwitchConfiguration#select()} picks, at the same origin and random. */
public class ConfigSwitchFeature extends Feature<ConfigSwitchConfiguration> {
    public ConfigSwitchFeature() {
        super(ConfigSwitchConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<ConfigSwitchConfiguration> context) {
        return context.config().select().value().place(context.level(), context.chunkGenerator(), context.random(), context.origin());
    }
}
