package com.coolfreeze23.vbparity.sulfur;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.blackgear.vanillabackport.common.registries.entities.ModAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/** Which entities get native 26.2 restitution (EntityBounceMixin) instead of the 1.21.1 slime/bed formulas. */
public final class SulfurBounce {
    private SulfurBounce() {
    }

    /**
     * Every VB sulfur cube (sticky, baby and empty ones included: native bounces them with the block's restitution) and
     * every other entity VB gives a BOUNCINESS above 0. Other entities keep the 1.21.1 engine formulas.
     */
    public static boolean nativeRestitution(Entity entity) {
        if (entity instanceof SulfurCube) {
            return true;
        }
        if (entity instanceof LivingEntity living) {
            AttributeInstance instance = living.getAttribute(ModAttributes.BOUNCINESS);
            return instance != null && instance.getValue() > 0.0;
        }
        return false;
    }
}
