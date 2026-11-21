package com.coolfreeze23.vbparity.wild.worldgen;

import com.coolfreeze23.vbparity.wild.WildConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Places {@code on} or {@code off} depending on a VanillaBackport config toggle, at the same origin with the same random.
 * Native 26.3 has no toggle for fallen poplars or poplar leaf litter; this keeps VB's {@code has_fallen_trees} /
 * {@code has_leaf_litter} switches working while the dappled forest uses the native tree selector.
 */
public class ConfigSwitchFeature extends Feature<ConfigSwitchFeature.Config> {
    public ConfigSwitchFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config config = context.config();
        Holder<PlacedFeature> chosen = WildConfig.isOn(config.key()) ? config.on() : config.off();
        return chosen.value().place(context.level(), context.chunkGenerator(), context.random(), context.origin());
    }

    public record Config(String key, Holder<PlacedFeature> on, Holder<PlacedFeature> off) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WildConfig.KEY_CODEC.fieldOf("key").forGetter(Config::key),
            PlacedFeature.CODEC.fieldOf("on").forGetter(Config::on),
            PlacedFeature.CODEC.fieldOf("off").forGetter(Config::off)
        ).apply(instance, Config::new));
    }
}
