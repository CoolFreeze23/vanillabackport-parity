package com.coolfreeze23.vbparity.misc;

import com.coolfreeze23.vbparity.misc.mixin.BoatAccessor;
import net.minecraft.world.entity.vehicle.Boat;

/** Native AbstractBoat.checkFallDamage: boats track fall distance but never break or deal fall damage on landing. */
public final class BoatFallLogic {
    private BoatFallLogic() {}

    public static void checkFallDamage(Boat boat, double y, boolean onGround, boolean waterBelow) {
        ((BoatAccessor) boat).vbp$setLastYd(boat.getDeltaMovement().y);
        if (!boat.isPassenger()) {
            if (onGround) {
                boat.resetFallDistance();
            } else if (!waterBelow && y < 0.0) {
                boat.fallDistance -= (float) y;
            }
        }
    }
}
