package com.coolfreeze23.vbparity.mayhem;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * TPHYS-C03: the current game's {@code "minecraft:flags": {"is_fall_flying": false}} (EntityFlagsPredicate checks only
 * isFallFlying). 1.21.1's {@code is_flying} also matches creative flight, so Lunge never fired while flying.
 */
public final class NotFallFlyingCondition implements LootItemCondition {
    public static final NotFallFlyingCondition INSTANCE = new NotFallFlyingCondition();
    public static final MapCodec<NotFallFlyingCondition> CODEC = MapCodec.unit(INSTANCE);
    public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

    private NotFallFlyingCondition() {
    }

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public boolean test(LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        return !(entity instanceof LivingEntity living && living.isFallFlying());
    }
}
