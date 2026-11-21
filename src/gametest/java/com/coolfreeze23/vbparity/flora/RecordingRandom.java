package com.coolfreeze23.vbparity.flora;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** A seeded random that records which bounded ints and how many doubles were drawn, and can force nextDouble. */
final class RecordingRandom implements RandomSource {
    private final RandomSource delegate;
    final List<Integer> intBounds = new ArrayList<>();
    int doubles;
    private Double forcedDouble;

    RecordingRandom(long seed) {
        this.delegate = new LegacyRandomSource(seed);
    }

    RecordingRandom forceDouble(double value) {
        this.forcedDouble = value;
        return this;
    }

    @Override
    public RandomSource fork() {
        return this.delegate.fork();
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return this.delegate.forkPositional();
    }

    @Override
    public void setSeed(long seed) {
        this.delegate.setSeed(seed);
    }

    @Override
    public int nextInt() {
        return this.delegate.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        this.intBounds.add(bound);
        return this.delegate.nextInt(bound);
    }

    @Override
    public long nextLong() {
        return this.delegate.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return this.delegate.nextBoolean();
    }

    @Override
    public float nextFloat() {
        return this.delegate.nextFloat();
    }

    @Override
    public double nextDouble() {
        this.doubles++;
        return this.forcedDouble != null ? this.forcedDouble : this.delegate.nextDouble();
    }

    @Override
    public double nextGaussian() {
        return this.delegate.nextGaussian();
    }
}
