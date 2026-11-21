package com.coolfreeze23.vbparity.shared;

import com.blackgear.vanillabackport.core.VanillaBackport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;

/**
 * Vanilla advancements that list a backported biome or mob (Adventuring Time, Monster Hunter, Monsters Hunted,
 * Two by Two) get the native criteria. When the matching VanillaBackport feature is turned off in its config,
 * the criterion is removed again at load, so the advancement stays obtainable.
 */
public final class AdvancementGates {
    private static final Map<String, BooleanSupplier> GATES = Map.ofEntries(
        Map.entry("minecraft:pale_garden", () -> VanillaBackport.COMMON_CONFIG.hasPaleGarden.get()),
        Map.entry("minecraft:sulfur_caves", () -> VanillaBackport.COMMON_CONFIG.hasSulfurCaves.get()),
        Map.entry("minecraft:dappled_forest", () -> VanillaBackport.COMMON_CONFIG.hasDappledForest.get()),
        Map.entry("minecraft:creaking", () -> VanillaBackport.COMMON_CONFIG.hasCreaking.get()),
        Map.entry("minecraft:camel_husk", () -> VanillaBackport.COMMON_CONFIG.hasCamelHusks.get()),
        Map.entry("minecraft:parched", () -> VanillaBackport.COMMON_CONFIG.hasParchedSkeletons.get()),
        Map.entry("minecraft:zombie_horse", () -> VanillaBackport.COMMON_CONFIG.hasZombieHorses.get()),
        Map.entry("minecraft:zombie_nautilus", () -> VanillaBackport.COMMON_CONFIG.hasNautilus.get()),
        Map.entry("minecraft:nautilus", () -> VanillaBackport.COMMON_CONFIG.hasNautilus.get())
    );
    private static final java.util.Set<ResourceLocation> TARGETS = java.util.Set.of(
        ResourceLocation.withDefaultNamespace("adventure/adventuring_time"),
        ResourceLocation.withDefaultNamespace("adventure/kill_a_mob"),
        ResourceLocation.withDefaultNamespace("adventure/kill_all_mobs"),
        ResourceLocation.withDefaultNamespace("husbandry/bred_all_animals")
    );

    private AdvancementGates() {
    }

    public static void apply(Map<ResourceLocation, JsonElement> advancements) {
        JsonElement effects = advancements.get(ResourceLocation.withDefaultNamespace("nether/all_effects"));
        if (effects != null && effects.isJsonObject() && !VanillaBackport.COMMON_CONFIG.hasNautilus.get()) {
            stripEffect(effects.getAsJsonObject(), "minecraft:breath_of_the_nautilus");
        }
        for (ResourceLocation id : TARGETS) {
            JsonElement element = advancements.get(id);
            if (element != null && element.isJsonObject()) {
                strip(element.getAsJsonObject());
            }
        }
    }

    static void stripEffect(JsonObject advancement, String effect) {
        try {
            advancement.getAsJsonObject("criteria").getAsJsonObject("all_effects")
                .getAsJsonObject("conditions").getAsJsonObject("effects").remove(effect);
        } catch (RuntimeException ignored) {
            // A datapack reshaped the advancement; leave it as it is.
        }
    }

    static void strip(JsonObject advancement) {
        if (!advancement.has("criteria") || !advancement.has("requirements")) {
            return;
        }
        JsonObject criteria = advancement.getAsJsonObject("criteria");
        java.util.Set<String> removed = new java.util.HashSet<>();
        for (Map.Entry<String, BooleanSupplier> gate : GATES.entrySet()) {
            if (criteria.has(gate.getKey()) && !gate.getValue().getAsBoolean()) {
                criteria.remove(gate.getKey());
                removed.add(gate.getKey());
            }
        }
        if (removed.isEmpty()) {
            return;
        }
        JsonArray kept = new JsonArray();
        for (JsonElement group : advancement.getAsJsonArray("requirements")) {
            JsonArray newGroup = new JsonArray();
            for (JsonElement name : group.getAsJsonArray()) {
                if (!removed.contains(name.getAsString())) {
                    newGroup.add(name);
                }
            }
            if (!newGroup.isEmpty()) {
                kept.add(newGroup);
            }
        }
        advancement.add("requirements", kept);
    }
}
