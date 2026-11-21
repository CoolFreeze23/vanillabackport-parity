package com.coolfreeze23.vbparity.wild;

/**
 * Native 26.x Entity.setInvulnerableTime: the entity counts as invulnerable (except to bypasses_invulnerability damage
 * and creative players) for that many ticks. 1.21.1 item entities have no such timer, so ItemEntityMixin adds one; it is
 * only used for the item a lightning-struck cushion drops (WILD-32).
 */
public interface TemporarilyInvulnerable {
    void vbparity$setInvulnerableTime(int ticks);

    int vbparity$getInvulnerableTime();
}
