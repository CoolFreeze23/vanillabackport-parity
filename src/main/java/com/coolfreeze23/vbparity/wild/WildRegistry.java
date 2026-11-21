package com.coolfreeze23.vbparity.wild;

import com.coolfreeze23.vbparity.wild.loot.ConfigLootCondition;
import com.coolfreeze23.vbparity.wild.worldgen.ConfigSwitchFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Wilderness Bound registrations: the native {@code minecraft:sleep_in_straw_bed} custom stat, the config switch feature
 * used by the dappled forest tree selector, and the config loot condition used by the camp chests.
 */
@EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
public final class WildRegistry {
    public static final ResourceLocation SLEEP_IN_STRAW_BED = ResourceLocation.withDefaultNamespace("sleep_in_straw_bed");
    public static final ConfigSwitchFeature CONFIG_SWITCH = new ConfigSwitchFeature();
    public static final LootItemConditionType CONFIG_CONDITION = new LootItemConditionType(ConfigLootCondition.CODEC);

    private WildRegistry() {
    }

    @SubscribeEvent
    static void onRegister(RegisterEvent event) {
        event.register(Registries.CUSTOM_STAT, helper -> helper.register(SLEEP_IN_STRAW_BED, SLEEP_IN_STRAW_BED));
        event.register(Registries.FEATURE, helper -> helper.register(ResourceLocation.fromNamespaceAndPath("vbparity", "wild_switch"), CONFIG_SWITCH));
        event.register(Registries.LOOT_CONDITION_TYPE,
            helper -> helper.register(ResourceLocation.fromNamespaceAndPath("vbparity", "wild_config"), CONFIG_CONDITION));
    }

    @SubscribeEvent
    static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Stats.CUSTOM.get(SLEEP_IN_STRAW_BED, StatFormatter.DEFAULT);
            WildConfig.onCommonSetup();
        });
    }
}
