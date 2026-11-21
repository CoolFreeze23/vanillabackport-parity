package com.coolfreeze23.vbparity.mayhem;

/** Marks a minecart whose known speed was already computed at the native point of this tick. */
public interface MinecartSpeedFlag {
    boolean vbp$consumeSpeedComputed();
}
