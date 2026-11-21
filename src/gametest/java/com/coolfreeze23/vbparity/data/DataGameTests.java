package com.coolfreeze23.vbparity.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public class DataGameTests {
    private static final String T = "misc_platform";

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(id));
    }

    private static void inTag(GameTestHelper h, Block b, TagKey<Block> tag, boolean expected) {
        h.assertTrue(b.defaultBlockState().is(tag) == expected,
            BuiltInRegistries.BLOCK.getKey(b) + (expected ? " missing from #" : " should not be in #") + tag.location());
    }

    /** Native 26.2 / 26.3-pre-1 tag memberships for backported ids (tools/datasweep/tags.py). */
    @GameTest(template = T)
    public static void data_native_tag_memberships(GameTestHelper h) {
        inTag(h, block("resin_clump"), BlockTags.REPLACEABLE, true);
        inTag(h, block("resin_clump"), BlockTags.ENCHANTMENT_POWER_TRANSMITTER, true);
        inTag(h, block("open_eyeblossom"), BlockTags.REPLACEABLE_BY_TREES, true);
        inTag(h, block("closed_eyeblossom"), BlockTags.REPLACEABLE_BY_TREES, true);
        inTag(h, block("pale_moss_block"), BlockTags.BIG_DRIPLEAF_PLACEABLE, false);
        inTag(h, Blocks.MOSS_BLOCK, BlockTags.BIG_DRIPLEAF_PLACEABLE, true);
        var types = h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var spear = types.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.withDefaultNamespace("spear")));
        h.assertTrue(spear.is(DamageTypeTags.CAN_BREAK_ARMOR_STAND), "spear damage can't break armor stands");
        h.assertTrue(spear.is(DamageTypeTags.PANIC_CAUSES), "spear damage doesn't cause panic");
        h.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace("nautilus")).is(EntityTypeTags.CAN_BREATHE_UNDER_WATER),
            "nautilus missing from #can_breathe_under_water");
        h.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace("copper_golem")).is(EntityTypeTags.FALL_DAMAGE_IMMUNE),
            "copper golem missing from #fall_damage_immune");
        h.assertFalse(block("closed_eyeblossom").asItem().builtInRegistryHolder().is(ItemTags.BEE_FOOD), "closed eyeblossom is bee food");
        h.assertTrue(block("open_eyeblossom").asItem().builtInRegistryHolder().is(ItemTags.BEE_FOOD), "open eyeblossom is not bee food");
        h.succeed();
    }

    /** TBLK-201: a resin clump between a bookshelf and the table passes the enchanting power on, as in native. */
    @GameTest(template = T)
    public static void data_resin_clump_transmits_enchanting_power(GameTestHelper h) {
        BlockPos table = new BlockPos(2, 1, 2);
        h.setBlock(table, Blocks.ENCHANTING_TABLE);
        h.setBlock(table.offset(2, 0, 0), Blocks.BOOKSHELF);
        h.setBlock(table.offset(1, 0, 0), block("resin_clump"));
        h.assertTrue(EnchantingTableBlock.isValidBookShelf(h.getLevel(), h.absolutePos(table), new BlockPos(2, 0, 0)),
            "resin clump blocks the bookshelf");
        h.succeed();
    }

    private static com.google.gson.JsonObject loot(GameTestHelper h, String table) {
        var server = h.getLevel().getServer();
        var t = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace(table)));
        return net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC
            .encodeStart(h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), t)
            .getOrThrow().getAsJsonObject();
    }

    private static java.util.List<String> names(com.google.gson.JsonObject pool) {
        java.util.List<String> out = new java.util.ArrayList<>();
        pool.getAsJsonArray("entries").forEach(e -> {
            var o = e.getAsJsonObject();
            out.add(o.has("name") ? o.get("name").getAsString() : "<" + o.get("type").getAsString() + ">");
        });
        return out;
    }

    /** OUT-07: VB's tables draw from their native per-world random sequence. */
    @GameTest(template = T)
    public static void data_loot_native_random_sequences(GameTestHelper h) {
        var spec = LootParity.spec().getAsJsonObject("random_sequence");
        h.assertTrue(spec.size() >= 160, "random_sequence list is short: " + spec.size());
        for (var e : spec.entrySet()) {
            var j = loot(h, e.getKey().substring("minecraft:".length()));
            h.assertTrue(j.has("random_sequence") && j.get("random_sequence").getAsString().equals(e.getValue().getAsString()),
                e.getKey() + " has no native random_sequence");
        }
        h.succeed();
    }

    /** OUT-04 / OUT-06: VB's chest additions sit at their native index (all VB loot features on by default). */
    @GameTest(template = T)
    public static void data_loot_native_entry_positions(GameTestHelper h) {
        for (var e : LootParity.spec().getAsJsonArray("placements")) {
            var o = e.getAsJsonObject();
            String table = o.get("table").getAsString().substring("minecraft:".length());
            java.util.List<String> eff = names(loot(h, table).getAsJsonArray("pools").get(o.get("pool").getAsInt()).getAsJsonObject());
            java.util.List<String> nat = new java.util.ArrayList<>();
            o.getAsJsonArray("native_order").forEach(x -> nat.add(x.getAsString()));
            java.util.Set<String> items = new java.util.HashSet<>();
            o.getAsJsonArray("items").forEach(x -> items.add(x.getAsString()));
            for (int i = 0; i < nat.size(); i++) {
                if (items.contains(nat.get(i))) {
                    h.assertTrue(i < eff.size() && eff.get(i).equals(nat.get(i)), table + ": " + nat.get(i) + " not at native index " + i + " in " + eff);
                }
            }
            h.assertFalse(table.startsWith("chests/underwater_ruin") && eff.contains("minecraft:iron_spear"), table + " still has VB's iron spear");
        }
        h.succeed();
    }

    /** OUT-01 / OUT-02 / OUT-03: native rider pools for zombies and husks, native zombie-horse rotten flesh. */
    @GameTest(template = T)
    public static void data_loot_native_mob_pools(GameTestHelper h) {
        var z = loot(h, "entities/zombie").getAsJsonArray("pools");
        h.assertTrue(z.size() == 4 && names(z.get(1).getAsJsonObject()).equals(java.util.List.of("minecraft:red_mushroom")), "zombie pool 1 is not the red mushroom");
        h.assertTrue(names(z.get(2).getAsJsonObject()).contains("minecraft:iron_ingot"), "zombie rare drops are not pool 2");
        var hu = loot(h, "entities/husk").getAsJsonArray("pools");
        h.assertTrue(names(hu.get(1).getAsJsonObject()).equals(java.util.List.of("minecraft:rabbit_foot")), "husk pool 1 is not the rabbit's foot");
        String zh = loot(h, "entities/zombie_horse").toString();
        h.assertTrue(zh.contains("\"min\":2.0") && zh.contains("\"max\":3.0"), "zombie horse rotten flesh is not 2-3: " + zh);
        h.succeed();
    }

    /** OUT-05: the wooden spear burns for 200 ticks, as in native FuelValues. */
    @GameTest(template = T)
    public static void data_wooden_spear_is_fuel(GameTestHelper h) {
        var stack = new net.minecraft.world.item.ItemStack(com.blackgear.vanillabackport.common.registries.items.ModItems.WOODEN_SPEAR.get());
        h.assertTrue(stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) == 200, "wooden spear burn time " + stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING));
        h.succeed();
    }

    /** ED-2: VB's animals roll XP from their own RNG (1 + random.nextInt(3)), not the level's. */
    @GameTest(template = T)
    public static void data_vb_animal_xp_uses_mob_random(GameTestHelper h) {
        for (var type : java.util.List.of("happy_ghast", "nautilus", "zombie_nautilus", "camel_husk")) {
            var mob = (net.minecraft.world.entity.LivingEntity) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace(type)).create(h.getLevel());
            for (long seed = 0; seed < 12; seed++) {
                mob.getRandom().setSeed(seed);
                int xp = ((com.coolfreeze23.vbparity.data.mixin.LivingEntityXpInvoker) mob).vbparity$data$baseExperienceReward();
                int expected = 1 + net.minecraft.util.RandomSource.create(seed).nextInt(3);
                h.assertTrue(xp == expected, type + " xp " + xp + " not from its own random (expected " + expected + ")");
            }
        }
        h.succeed();
    }

    private static java.util.List<String> spawns(GameTestHelper h, String biome, net.minecraft.world.entity.MobCategory cat) {
        var b = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(biome)));
        java.util.List<String> out = new java.util.ArrayList<>();
        for (var d : b.getMobSettings().getMobs(cat).unwrap()) {
            out.add(BuiltInRegistries.ENTITY_TYPE.getKey(d.type).getPath() + ":" + d.getWeight().asInt() + ":" + d.minCount + "-" + d.maxCount);
        }
        return out;
    }

    /** SPAWN-01..05: native 26.2 spawn lists (weights, pack sizes, order) in the biomes VB adds to (defaults: features on). */
    @GameTest(template = T)
    public static void data_native_biome_spawn_lists(GameTestHelper h) {
        var M = net.minecraft.world.entity.MobCategory.MONSTER;
        var C = net.minecraft.world.entity.MobCategory.CREATURE;
        var plains = spawns(h, "plains", M);
        h.assertTrue(plains.subList(0, 5).equals(java.util.List.of("spider:100:4-4", "zombie:90:4-4", "zombie_villager:5:1-1", "zombie_horse:5:1-1", "skeleton:100:4-4")),
            "plains monsters " + plains);
        h.assertTrue(spawns(h, "snowy_plains", M).contains("zombie:90:4-4"), "snowy plains zombie weight");
        var desert = spawns(h, "desert", M);
        h.assertTrue(desert.contains("skeleton:50:4-4") && desert.contains("parched:50:4-4"), "desert monsters " + desert);
        h.assertTrue(spawns(h, "desert", C).equals(java.util.List.of("rabbit:12:2-3", "camel:1:1-1")), "desert creatures " + spawns(h, "desert", C));
        h.assertTrue(spawns(h, "badlands", C).subList(0, 4).equals(java.util.List.of("sheep:12:4-4", "pig:10:4-4", "chicken:10:4-4", "cow:8:4-4")),
            "badlands creatures " + spawns(h, "badlands", C));
        h.assertTrue(spawns(h, "warm_ocean", net.minecraft.world.entity.MobCategory.WATER_CREATURE).get(0).startsWith("nautilus:5"), "warm ocean water creatures");
        inTag(h, Blocks.SAND, net.minecraft.tags.TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("camels_spawnable_on")), true);
        h.succeed();
    }

    /** Native stonecutter paths from raw sulfur/cinnabar and native recipe-book groups (tools/datasweep/gen_recipes.py). */
    @GameTest(template = T)
    public static void data_native_recipes(GameTestHelper h) {
        var rm = h.getLevel().getServer().getRecipeManager();
        for (String id : java.util.List.of("cinnabar_bricks_from_cinnabar_stonecutting", "sulfur_brick_wall_from_sulfur_stonecutting",
                "cinnabar_brick_slab_from_polished_cinnabar_stonecutting")) {
            h.assertTrue(rm.byKey(ResourceLocation.withDefaultNamespace(id)).isPresent(), "missing native recipe " + id);
        }
        h.assertTrue(rm.byKey(ResourceLocation.withDefaultNamespace("black_wool_slab")).map(r -> r.value().getGroup()).orElse("").equals("woolen_slab"),
            "black_wool_slab group is not woolen_slab");
        h.assertTrue(rm.byKey(ResourceLocation.withDefaultNamespace("white_concrete_stairs")).map(r -> r.value().getGroup()).orElse("").equals("concrete_stairs"),
            "white_concrete_stairs group is not concrete_stairs");
        h.succeed();
    }

    /** Writes vbparity-dump.json for the offline sweep when vbparity-dump.request is present; otherwise a no-op. */
    @GameTest(template = T, required = false)
    public static void data_dump_effective_state(GameTestHelper h) {
        if (DataDump.requested()) {
            try {
                DataDump.write(h.getLevel());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        h.succeed();
    }
}
