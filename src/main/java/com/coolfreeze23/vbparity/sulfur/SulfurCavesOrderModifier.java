package com.coolfreeze23.vbparity.sulfur;

import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Restores native ordering in the sulfur caves biome after VB's additions: rooted_sulfur_spring before sulfur_pool in the
 * LAKES step, and the sulfur cube spawner as the first MONSTER entry. Only reorders what VB already added.
 */
public record SulfurCavesOrderModifier() implements BiomeModifier {
    public static final MapCodec<SulfurCavesOrderModifier> CODEC = MapCodec.unit(SulfurCavesOrderModifier::new);
    private static final ResourceLocation SPRING = ResourceLocation.withDefaultNamespace("rooted_sulfur_spring");
    private static final ResourceLocation POOL = ResourceLocation.withDefaultNamespace("sulfur_pool");

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.AFTER_EVERYTHING || !biome.is(ModBiomes.SULFUR_CAVES)) {
            return;
        }

        List<Holder<PlacedFeature>> lakes = builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.LAKES);
        int pool = indexOf(lakes, POOL);
        int spring = indexOf(lakes, SPRING);
        if (pool >= 0 && spring > pool) {
            Holder<PlacedFeature> poolHolder = lakes.remove(pool);
            lakes.add(indexOf(lakes, SPRING) + 1, poolHolder);
        }

        List<MobSpawnSettings.SpawnerData> monsters = builder.getMobSpawnSettings().getSpawner(MobCategory.MONSTER);
        for (int i = 1; i < monsters.size(); i++) {
            if (monsters.get(i).type == ModEntityTypes.SULFUR_CUBE.get()) {
                monsters.add(0, monsters.remove(i));
                break;
            }
        }
    }

    private static int indexOf(List<Holder<PlacedFeature>> features, ResourceLocation id) {
        for (int i = 0; i < features.size(); i++) {
            if (features.get(i).is(id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
