package com.coolfreeze23.vbparity.variants;

import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;

/** VARIANTS-13: native 26.2 {@code BABY_DIMENSIONS} of the Spring to Life baby redesigns. */
public final class BabyDimensions {
    public static final EntityDimensions CHICKEN = of(0.3F, 0.4F, 0.28125F, 0.375F);
    public static final EntityDimensions COW = of(0.45F, 0.7F, 0.69F, 0.75F);
    public static final EntityDimensions PIG = of(0.45F, 0.45F, 0.40625F, 0.5F);
    public static final EntityDimensions SHEEP = of(0.45F, 0.65F, 0.65625F, 0.5625F);
    public static final EntityDimensions WOLF = of(0.3F, 0.425F, 0.34375F, 0.4375F);
    public static final EntityDimensions CAT = of(0.3F, 0.35F, 0.34375F, 0.3125F);

    private BabyDimensions() {
    }

    private static EntityDimensions of(float width, float height, float eyeHeight, float passengerY) {
        return EntityDimensions.scalable(width, height)
            .withEyeHeight(eyeHeight)
            .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, passengerY, 0.0F));
    }
}
