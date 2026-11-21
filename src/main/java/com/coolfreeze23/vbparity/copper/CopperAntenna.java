package com.coolfreeze23.vbparity.copper;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * COPPER-25: native keeps the copper golem's antenna item in EquipmentSlot.SADDLE, which 1.21.1 lacks. The BODY slot
 * (animal armour) is the 1.21.1 slot with the same rules: unused by copper golems, default drop chance 0.085, synced
 * and saved with the mob. VB kept the antenna in HEAD, so a head item could not be worn; this moves it to BODY.
 */
public final class CopperAntenna {
    public static final EquipmentSlot SLOT = EquipmentSlot.BODY;
    /** Marks a golem saved with the antenna in BODY; golems without it are VB saves with the antenna in HEAD. */
    public static final String SAVE_MARKER = "vbparity:antenna_in_body";
    /** Native #candidate_for_iron_golem_gift: villagers plus #accepts_iron_golem_gift. */
    public static final TagKey<EntityType<?>> CANDIDATE_FOR_IRON_GOLEM_GIFT =
        TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("candidate_for_iron_golem_gift"));
    public static final TagKey<EntityType<?>> ACCEPTS_IRON_GOLEM_GIFT =
        TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("accepts_iron_golem_gift"));

    private CopperAntenna() {
    }

    /** Native ServerLevel.getNearestEntity(TagKey, ...): the nearest tagged living entity that passes the conditions. */
    @Nullable
    public static LivingEntity nearestGiftCandidate(Level level, TargetingConditions conditions, @Nullable LivingEntity source, double x, double y, double z, AABB box) {
        double best = -1.0;
        LivingEntity nearest = null;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.getType().is(CANDIDATE_FOR_IRON_GOLEM_GIFT))) {
            if (conditions.test(source, entity)) {
                double distance = entity.distanceToSqr(x, y, z);
                if (best == -1.0 || distance < best) {
                    best = distance;
                    nearest = entity;
                }
            }
        }
        return nearest;
    }
}
