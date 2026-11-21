package com.coolfreeze23.vbparity.sulfur;

import net.minecraft.world.damagesource.DamageSource;

/** Implemented by VB's SulfurCube (SulfurCubeMixin): native knockback(…, source, damage, …) inputs for a stab without hurt. */
public interface StabKnockbackTarget {
    void vbparity$prepareKnockback(DamageSource source, float damage);
}
