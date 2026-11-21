package com.coolfreeze23.vbparity.ai;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** A seeded random that logs every draw: "f", "d", "b", "l", "g", "i" (unbounded) or "i<bound>" with its value. */
final class TraceRandom implements RandomSource {
    private final RandomSource delegate;
    final List<String> calls = new ArrayList<>();
    final List<Integer> intValues = new ArrayList<>();
    private Float forcedFloat;

    TraceRandom(long seed) {
        this.delegate = new LegacyRandomSource(seed);
    }

    TraceRandom forceFloat(float value) {
        this.forcedFloat = value;
        return this;
    }

    /** The value of the first nextInt(bound) call, or -1. */
    int valueOf(int bound) {
        int k = 0;
        for (String c : this.calls) {
            if (c.startsWith("i") && c.length() > 1) {
                if (c.equals("i" + bound)) {
                    return this.intValues.get(k);
                }
                k++;
            }
        }
        return -1;
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
        this.calls.add("i");
        return this.delegate.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        int v = this.delegate.nextInt(bound);
        this.calls.add("i" + bound);
        this.intValues.add(v);
        return v;
    }

    @Override
    public long nextLong() {
        this.calls.add("l");
        return this.delegate.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        this.calls.add("b");
        return this.delegate.nextBoolean();
    }

    @Override
    public float nextFloat() {
        this.calls.add("f");
        float v = this.delegate.nextFloat();
        return this.forcedFloat != null ? this.forcedFloat : v;
    }

    @Override
    public double nextDouble() {
        this.calls.add("d");
        return this.delegate.nextDouble();
    }

    @Override
    public double nextGaussian() {
        this.calls.add("g");
        return this.delegate.nextGaussian();
    }
}
