package com.coolfreeze23.vbparity.data;

import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Native 26.2 spawn lists in the biomes VB adds to, after every other modifier. Each change only touches the exact
 * 1.21.1 entry next to an entry VB added (so the VB feature is on), which leaves lists other mods rewrote alone.
 * <ul>
 *   <li>SPAWN-01: with VB's parched in the list, skeleton 100 → 50 (native {@code monsters(builder, 19, 1, 0, 50)}).</li>
 *   <li>SPAWN-02: with VB's zombie horse, zombie 95 → 90 and the zombie horse right after the zombie villager.</li>
 *   <li>SPAWN-03: badlands farm animals (sheep 12, pig 10, chicken 10, cow 8 at the head of CREATURE), gated on
 *   {@code farm_animal_variants}.</li>
 *   <li>SPAWN-04: with VB's camel, rabbit 4 → 12.</li>
 *   <li>SPAWN-05: warm ocean nautilus first in WATER_CREATURE.</li>
 * </ul>
 */
public record DataSpawnParity() implements BiomeModifier {
    public static final MapCodec<DataSpawnParity> CODEC = MapCodec.unit(DataSpawnParity::new);

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.AFTER_EVERYTHING) {
            return;
        }
        var spawns = builder.getMobSpawnSettings();
        List<SpawnerData> monsters = spawns.getSpawner(MobCategory.MONSTER);
        if (indexOf(monsters, ModEntityTypes.PARCHED.get()) >= 0) {
            replace(monsters, EntityType.SKELETON, 100, 4, 4, 50);
        }
        int horse = indexOf(monsters, EntityType.ZOMBIE_HORSE);
        if (horse >= 0 && monsters.get(horse).getWeight().asInt() == 5) {
            replace(monsters, EntityType.ZOMBIE, 95, 4, 4, 90);
            int villager = indexOf(monsters, EntityType.ZOMBIE_VILLAGER);
            if (villager >= 0 && horse != villager + 1) {
                SpawnerData h = monsters.remove(horse);
                monsters.add(indexOf(monsters, EntityType.ZOMBIE_VILLAGER) + 1, h);
            }
        }
        List<SpawnerData> creatures = spawns.getSpawner(MobCategory.CREATURE);
        if (indexOf(creatures, EntityType.CAMEL) >= 0) {
            replace(creatures, EntityType.RABBIT, 4, 2, 3, 12);
        }
        if (biome.is(BiomeTags.IS_BADLANDS) && VanillaBackport.COMMON_CONFIG.hasFarmAnimalVariants.get()
            && indexOf(creatures, EntityType.SHEEP) < 0) {
            creatures.add(0, new SpawnerData(EntityType.COW, 8, 4, 4));
            creatures.add(0, new SpawnerData(EntityType.CHICKEN, 10, 4, 4));
            creatures.add(0, new SpawnerData(EntityType.PIG, 10, 4, 4));
            creatures.add(0, new SpawnerData(EntityType.SHEEP, 12, 4, 4));
        }
        if (biome.is(Biomes.WARM_OCEAN)) {
            List<SpawnerData> water = spawns.getSpawner(MobCategory.WATER_CREATURE);
            int n = indexOf(water, ModEntityTypes.NAUTILUS.get());
            if (n > 0) {
                water.add(0, water.remove(n));
            }
        }
    }

    static int indexOf(List<SpawnerData> list, EntityType<?> type) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).type == type) return i;
        }
        return -1;
    }

    private static void replace(List<SpawnerData> list, EntityType<?> type, int weight, int min, int max, int newWeight) {
        for (int i = 0; i < list.size(); i++) {
            SpawnerData d = list.get(i);
            if (d.type == type && d.getWeight().asInt() == weight && d.minCount == min && d.maxCount == max) {
                list.set(i, new SpawnerData(type, newWeight, min, max));
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
