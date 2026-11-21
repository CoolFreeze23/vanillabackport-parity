package com.coolfreeze23.vbparity.ghast;

import com.blackgear.platform.common.data.neoforge.LootPoolAccess;
import com.blackgear.platform.core.mixin.neoforge.access.LootTableAccessor;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.coolfreeze23.vbparity.ghast.mixin.LootItemAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/** Event wiring for the happy ghast, harness and dried ghast fixes. */
public final class GhastParity {
    private GhastParity() {
    }

    @SuppressWarnings("unchecked")
    static final Supplier<Item>[] HARNESSES = new Supplier[]{
        ModItems.WHITE_HARNESS, ModItems.ORANGE_HARNESS, ModItems.MAGENTA_HARNESS, ModItems.LIGHT_BLUE_HARNESS,
        ModItems.YELLOW_HARNESS, ModItems.LIME_HARNESS, ModItems.PINK_HARNESS, ModItems.GRAY_HARNESS,
        ModItems.LIGHT_GRAY_HARNESS, ModItems.CYAN_HARNESS, ModItems.PURPLE_HARNESS, ModItems.BLUE_HARNESS,
        ModItems.BROWN_HARNESS, ModItems.GREEN_HARNESS, ModItems.RED_HARNESS, ModItems.BLACK_HARNESS
    };

    @EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        /** SPAWN-07: native SpawnPlacements registers the happy ghast with Animal.checkAnimalSpawnRules. */
        @SubscribeEvent
        public static void spawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
            event.register(com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes.HAPPY_GHAST.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }

        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                for (Supplier<Item> harness : HARNESSES) {
                    DispenserBlock.registerBehavior(harness.get(), HarnessDispenseBehavior.INSTANCE);
                }
            });
        }
    }

    @EventBusSubscriber(modid = "vbparity")
    public static final class GameEvents {
        /**
         * The current game lists the dried ghast right after the ender pearl in the piglin barter pool. VanillaBackport
         * appends it at the end (same weight, so the same odds, but a given roll maps to a different item). Move it.
         */
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onLootTableLoad(LootTableLoadEvent event) {
            if (!event.getName().equals(BuiltInLootTables.PIGLIN_BARTERING.location())) {
                return;
            }

            List<LootPool> pools = ((LootTableAccessor) event.getTable()).getPools();
            if (pools.isEmpty()) {
                return;
            }

            LootPoolAccess pool = (LootPoolAccess) pools.get(0);
            List<LootPoolEntryContainer> entries = new ArrayList<>(pool.platform$getEntries());
            int driedGhast = indexOf(entries, ModBlocks.DRIED_GHAST.get().asItem());
            int enderPearl = indexOf(entries, Items.ENDER_PEARL);
            if (driedGhast < 0 || enderPearl < 0 || driedGhast == enderPearl + 1) {
                return;
            }

            LootPoolEntryContainer entry = entries.remove(driedGhast);
            enderPearl = indexOf(entries, Items.ENDER_PEARL);
            entries.add(enderPearl + 1, entry);
            pool.platform$setEntries(entries);
        }

        private static int indexOf(List<LootPoolEntryContainer> entries, Item item) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i) instanceof LootItem lootItem && ((LootItemAccessor) lootItem).vbp$getItem().value() == item) {
                    return i;
                }
            }

            return -1;
        }
    }
}
