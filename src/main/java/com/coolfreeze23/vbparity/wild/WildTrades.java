package com.coolfreeze23.vbparity.wild;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

/**
 * WILD-26. Native 26.3 wandering trader: {@code emerald_poplar_log} (1 emerald -> 8 logs, 4 uses) in the uncommon pool,
 * {@code emerald_poplar_sapling} (5 -> 1, 8 uses) and {@code emerald_shelf_mushroom} (1 -> 3, 12 uses) in the common pool,
 * all 1 xp and 0.05 price multiplier. VB maps native "uncommon" to the 1.21.1 rare list (its pale oak log trade).
 */
@EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.GAME)
public final class WildTrades {
    private WildTrades() {
    }

    @SubscribeEvent
    static void onWandererTrades(WandererTradesEvent event) {
        event.getRareTrades().add(new VillagerTrades.ItemsForEmeralds(ModBlocks.POPLAR_LOG.get(), 1, 8, 4, 1));
        event.getGenericTrades().add(new VillagerTrades.ItemsForEmeralds(ModBlocks.POPLAR_SAPLING.get(), 5, 1, 8, 1));
        event.getGenericTrades().add(new VillagerTrades.ItemsForEmeralds(ModBlocks.SHELF_MUSHROOM.get(), 1, 3, 12, 1));
    }
}
