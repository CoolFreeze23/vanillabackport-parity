package com.coolfreeze23.vbparity.flora.worldgen;

import com.coolfreeze23.vbparity.VBParity;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.jetbrains.annotations.Nullable;

/**
 * Native biomes list the Spring to Life vegetation at fixed positions inside VEGETAL_DECORATION (wildflowers and bushes
 * before trees, and so on). VB appends it at the end. This moves VB's flora features to their 26.2 position, and drops VB's
 * separate cactus-flower pass (the cactus column now carries its own flower layer).
 *
 * <p>A moved feature only skips back over features that follow it natively, so it keeps every ordering VB's list gave it
 * against 1.21.1-only and other mods' features. The pre-move lists are kept, and {@code FeatureSorterMixin} falls back to
 * them if the new order still conflicts with another feature order.
 */
public final class VegetalOrderModifier implements BiomeModifier {
    public static final VegetalOrderModifier INSTANCE = new VegetalOrderModifier();
    public static final MapCodec<VegetalOrderModifier> CODEC = MapCodec.unit(INSTANCE);

    static final Set<String> MOVED = Set.of(
        "minecraft:patch_bush", "minecraft:patch_firefly_bush_near_water", "minecraft:patch_firefly_bush_swamp",
        "minecraft:patch_firefly_bush_near_water_swamp", "minecraft:wildflowers_birch_forest", "minecraft:wildflowers_meadow",
        "minecraft:patch_dry_grass_desert", "minecraft:patch_dry_grass_badlands", "minecraft:patch_leaf_litter"
    );
    static final Set<String> REMOVED = Set.of("minecraft:cactus_flower");

    /** Features that follow a moved feature in at least one 26.2 biome list. */
    private static final Map<String, Set<String>> FOLLOWERS = new HashMap<>();
    static {
        for (List<String> order : NativeVegetalOrder.ORDER.values()) {
            for (int i = 0; i < order.size(); i++) {
                if (MOVED.contains(order.get(i))) {
                    FOLLOWERS.computeIfAbsent(order.get(i), k -> new HashSet<>()).addAll(order.subList(i + 1, order.size()));
                }
            }
        }
    }

    /** Biome id -> its VEGETAL_DECORATION list before this modifier changed it. */
    public static final Map<ResourceKey<Biome>, List<Holder<PlacedFeature>>> ORIGINAL = new ConcurrentHashMap<>();
    public static volatile boolean fallbackUsed = false;

    private VegetalOrderModifier() {}

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.AFTER_EVERYTHING) {
            return;
        }
        ResourceKey<Biome> biomeKey = biome.unwrapKey().orElse(null);
        if (biomeKey == null) {
            return;
        }
        List<Holder<PlacedFeature>> list = builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION);
        if (list.stream().noneMatch(h -> MOVED.contains(id(h)) || REMOVED.contains(id(h)))) {
            return;
        }
        List<Holder<PlacedFeature>> before = List.copyOf(list);
        list.removeIf(h -> REMOVED.contains(id(h)));
        List<String> nativeOrder = NativeVegetalOrder.ORDER.get(biomeKey.location().toString());
        reorder(list, nativeOrder);
        if (!before.equals(list)) {
            ORIGINAL.put(biomeKey, before);
        }
    }

    /**
     * Moves each VB flora feature back over the features that follow it natively (in this biome's 26.2 list, or in any
     * 26.2 list for biomes 26.2 lacks), and no further: it stays after everything else VB's order put before it.
     */
    private static void reorder(List<Holder<PlacedFeature>> list, @Nullable List<String> nativeOrder) {
        Map<String, Integer> index = new HashMap<>();
        if (nativeOrder != null) {
            for (int i = 0; i < nativeOrder.size(); i++) {
                index.putIfAbsent(nativeOrder.get(i), i);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            Holder<PlacedFeature> h = list.get(i);
            String id = id(h);
            if (!MOVED.contains(id)) {
                continue;
            }
            Integer own = index.get(id);
            Set<String> followers = FOLLOWERS.getOrDefault(id, Set.of());
            int at = i;
            while (at > 0) {
                String prev = id(list.get(at - 1));
                boolean later = own != null ? index.getOrDefault(prev, -1) > own : followers.contains(prev);
                if (!later) {
                    break;
                }
                at--;
            }
            if (at != i) {
                list.remove(i);
                list.add(at, h);
            }
        }
    }

    static String id(Holder<PlacedFeature> holder) {
        return holder.unwrapKey().map(k -> k.location().toString()).orElse("");
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }

    public static void logFallback(Exception e) {
        fallbackUsed = true;
        VBParity.LOG.warn("Native flora feature order conflicts with another feature order ({}); keeping VanillaBackport's order", e.getMessage());
    }
}
