package com.coolfreeze23.vbparity.shared;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public class SharedGameTests {
    private static final String T = "misc_platform";

    private static AdvancementHolder adv(GameTestHelper h, String path) {
        ServerAdvancementManager manager = h.getLevel().getServer().getAdvancements();
        AdvancementHolder holder = manager.get(ResourceLocation.withDefaultNamespace(path));
        if (holder == null) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("missing advancement " + path);
        }
        return holder;
    }

    private static void hasCriteria(GameTestHelper h, String path, String... keys) {
        AdvancementHolder holder = adv(h, path);
        for (String key : keys) {
            h.assertTrue(holder.value().criteria().containsKey(key), path + " lacks native criterion " + key);
            h.assertTrue(holder.value().requirements().names().contains(key), path + " does not require " + key);
        }
    }

    /** Native 26.2/26.3 criteria for the backported biomes and mobs (defaults: every VB feature on). */
    @GameTest(template = T)
    public static void shared_native_advancement_criteria(GameTestHelper h) {
        hasCriteria(h, "adventure/adventuring_time", "minecraft:pale_garden", "minecraft:sulfur_caves", "minecraft:dappled_forest");
        hasCriteria(h, "adventure/kill_a_mob", "minecraft:creaking", "minecraft:camel_husk", "minecraft:parched", "minecraft:zombie_horse", "minecraft:zombie_nautilus");
        hasCriteria(h, "adventure/kill_all_mobs", "minecraft:creaking", "minecraft:camel_husk", "minecraft:parched", "minecraft:zombie_horse", "minecraft:zombie_nautilus");
        hasCriteria(h, "husbandry/bred_all_animals", "minecraft:nautilus");
        hasCriteria(h, "husbandry/place_dried_ghast_in_water", "place_dried_ghast_in_water");
        String effects = adv(h, "nether/all_effects").value().criteria().get("all_effects").triggerInstance().toString();
        h.assertTrue(effects.contains("breath_of_the_nautilus"), "How Did We Get Here lacks Breath of the Nautilus");
        h.assertTrue(new net.minecraft.world.item.ItemStack(com.blackgear.vanillabackport.common.registries.items.ModItems.GOLDEN_SPEAR.get()).is(net.minecraft.tags.ItemTags.PIGLIN_LOVED), "golden spear not piglin_loved");
        // Native unlock path for harness recipes; VB's duplicate under transportation/ is gone.
        adv(h, "recipes/combat/black_harness");
        h.assertTrue(h.getLevel().getServer().getAdvancements().get(ResourceLocation.withDefaultNamespace("recipes/transportation/black_harness")) == null,
            "VB's duplicate harness unlock advancement still loads");
        h.succeed();
    }

    /** A disabled VB feature removes its criterion and the requirement group, so the advancement stays obtainable. */
    @GameTest(template = T)
    public static void shared_advancement_gate_strips_requirement(GameTestHelper h) {
        JsonObject adv = JsonParser.parseString("{\"criteria\":{\"minecraft:plains\":{},\"minecraft:pale_garden\":{}},"
            + "\"requirements\":[[\"minecraft:plains\"],[\"minecraft:pale_garden\"]]}").getAsJsonObject();
        boolean on = com.blackgear.vanillabackport.core.VanillaBackport.COMMON_CONFIG.hasPaleGarden.get();
        AdvancementGates.strip(adv);
        JsonArray req = adv.getAsJsonArray("requirements");
        if (on) {
            h.assertTrue(req.size() == 2 && adv.getAsJsonObject("criteria").has("minecraft:pale_garden"), "gate removed an enabled feature");
        } else {
            h.assertTrue(req.size() == 1 && !adv.getAsJsonObject("criteria").has("minecraft:pale_garden"), "gate kept a disabled feature");
        }
        h.succeed();
    }
}
