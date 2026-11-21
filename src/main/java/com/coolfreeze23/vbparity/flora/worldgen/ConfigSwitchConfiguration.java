package com.coolfreeze23.vbparity.flora.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Picks one placed feature by VanillaBackport config: the first case whose keys are all on, else the default.
 * Lets one data file hold both the native feature and VB's original for when the VB toggle is off.
 */
public record ConfigSwitchConfiguration(List<Case> cases, Holder<PlacedFeature> defaultFeature) implements FeatureConfiguration {
    public static final Codec<ConfigSwitchConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
        Case.CODEC.listOf().fieldOf("cases").forGetter(ConfigSwitchConfiguration::cases),
        PlacedFeature.CODEC.fieldOf("default").forGetter(ConfigSwitchConfiguration::defaultFeature)
    ).apply(i, ConfigSwitchConfiguration::new));

    public Holder<PlacedFeature> select() {
        for (Case c : this.cases) {
            if (c.when().stream().allMatch(VbConfigGate::isOn)) {
                return c.feature();
            }
        }
        return this.defaultFeature;
    }

    @Override
    public Stream<ConfiguredFeature<?, ?>> getFeatures() {
        return this.select().value().getFeatures();
    }

    public record Case(List<String> when, Holder<PlacedFeature> feature) {
        public static final Codec<Case> CODEC = RecordCodecBuilder.create(i -> i.group(
            VbConfigGate.KEY_CODEC.listOf().fieldOf("when").forGetter(Case::when),
            PlacedFeature.CODEC.fieldOf("feature").forGetter(Case::feature)
        ).apply(i, Case::new));
    }
}
