package com.coolfreeze23.vbparity.wild.loot;

import com.coolfreeze23.vbparity.wild.WildConfig;
import com.coolfreeze23.vbparity.wild.WildRegistry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/** {@code vbparity:wild_config}: true when a VanillaBackport config toggle is on (copper gear and spears in camp chests). */
public record ConfigLootCondition(String key) implements LootItemCondition {
    public static final MapCodec<ConfigLootCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        WildConfig.KEY_CODEC.fieldOf("key").forGetter(ConfigLootCondition::key)
    ).apply(instance, ConfigLootCondition::new));

    @Override
    public LootItemConditionType getType() {
        return WildRegistry.CONFIG_CONDITION;
    }

    @Override
    public boolean test(LootContext context) {
        return WildConfig.isOn(this.key);
    }
}
