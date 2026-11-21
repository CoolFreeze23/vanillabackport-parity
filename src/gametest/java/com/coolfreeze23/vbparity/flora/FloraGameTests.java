package com.coolfreeze23.vbparity.flora;

import com.blackgear.vanillabackport.common.level.worldgen.features.FallenTreeFeature;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.flora.mixin.FallenTreeFeatureInvoker;
import com.coolfreeze23.vbparity.flora.worldgen.ConfigSwitchConfiguration;
import com.coolfreeze23.vbparity.flora.worldgen.VegetalOrderModifier;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(VBParity.MOD_ID)
@PrefixGameTestTemplate(false)
public class FloraGameTests {
    private static final String T = "flora_empty";

    // ---------------------------------------------------------------- helpers

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static <T> Registry<T> reg(GameTestHelper h, ResourceKey<? extends Registry<T>> key) {
        return h.getLevel().registryAccess().registryOrThrow(key);
    }

    private static String id(Holder<?> holder) {
        return holder.unwrapKey().map(k -> k.location().toString()).orElse("inline");
    }

    /** The random_selector behind a placed feature, as "id@chance, ... | default=id". */
    private static String describeSelector(Holder<PlacedFeature> placed) {
        ConfiguredFeature<?, ?> cf = placed.value().feature().value();
        if (!(cf.config() instanceof RandomFeatureConfiguration rf)) {
            return "not a random_selector: " + cf;
        }
        String entries = rf.features.stream().map((WeightedPlacedFeature w) -> id(w.feature) + "@" + w.chance).collect(Collectors.joining(", "));
        return entries + " | default=" + id(rf.defaultFeature);
    }

    private static Holder<PlacedFeature> switchSelection(GameTestHelper h, ConfiguredFeature<?, ?> cf, String what) {
        if (!(cf.config() instanceof ConfigSwitchConfiguration sw)) {
            throw new AssertionError(what + " is not a vbparity:config_switch: " + cf);
        }
        return sw.select();
    }

    private static List<String> vegetal(GameTestHelper h, ResourceKey<Biome> biome) {
        Biome b = reg(h, Registries.BIOME).getOrThrow(biome);
        return b.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal()).stream()
            .map(FloraGameTests::id).toList();
    }

    private static boolean biomeIn(GameTestHelper h, ResourceKey<Biome> biome, String tag) {
        return reg(h, Registries.BIOME).getHolderOrThrow(biome).is(TagKey.create(Registries.BIOME, mc(tag)));
    }

    private static void floor(GameTestHelper h, Block block, int y, int size) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                h.setBlock(x, y, z, block);
            }
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    // ---------------------------------------------------------------- FLORA-01

    /** Native: no firefly particle roll (nextDouble) at night when the bush sits at raw brightness 14. VB rolled it. */
    @GameTest(template = T)
    public static void flora_firefly_particles_need_darkness(GameTestHelper h) {
        h.setBlock(2, 1, 2, Blocks.GRASS_BLOCK);
        h.setBlock(2, 2, 2, ModBlocks.FIREFLY_BUSH.get());
        h.setBlock(3, 2, 2, Blocks.GLOWSTONE);
        h.runAfterDelay(20, () -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
            BlockState bush = level.getBlockState(pos);
            long savedTime = level.getDayTime();
            RecordingRandom random = new RecordingRandom(42L);
            int brightness;
            try {
                level.setDayTime(18000L);
                level.updateSkyBrightness();
                brightness = level.getMaxLocalRawBrightness(pos);
                bush.getBlock().animateTick(bush, level, pos, random);
            } finally {
                level.setDayTime(savedTime);
                level.updateSkyBrightness();
            }
            h.assertTrue(brightness > 13, "setup: brightness at the bush is " + brightness + ", needs 14+");
            h.assertTrue(random.doubles == 0, "firefly particle rolled " + random.doubles + " doubles at brightness " + brightness + " at night");
            h.succeed();
        });
    }

    // ---------------------------------------------------------------- FLORA-02

    /** Native sand / red sand roll the 1-in-2100 idle sound after the falling-dust roll; VB's hook never ran for falling blocks. */
    @GameTest(template = T)
    public static void flora_sand_idle_sound_rolls(GameTestHelper h) {
        for (Block block : List.of(Blocks.SAND, Blocks.RED_SAND)) {
            h.setBlock(2, 1, 2, block);
            BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
            RecordingRandom random = new RecordingRandom(7L);
            block.animateTick(block.defaultBlockState(), h.getLevel(), pos, random);
            h.assertTrue(random.intBounds.size() >= 2 && random.intBounds.get(0) == 16 && random.intBounds.get(1) == 2100,
                block + " animateTick rolls " + random.intBounds + ", native rolls [16, 2100, ...]");
        }
        h.setBlock(2, 1, 2, Blocks.GRAVEL);
        RecordingRandom gravel = new RecordingRandom(7L);
        Blocks.GRAVEL.animateTick(Blocks.GRAVEL.defaultBlockState(), h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)), gravel);
        h.assertFalse(gravel.intBounds.contains(2100), "gravel must not roll the sand idle sound");
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-04

    private static BlockPos cactusColumn(GameTestHelper h, int x, int height, int topAge) {
        h.setBlock(x, 1, 2, Blocks.SAND);
        for (int y = 0; y < height; y++) {
            h.setBlock(x, 2 + y, 2, Blocks.CACTUS.defaultBlockState().setValue(CactusBlock.AGE, y == height - 1 ? topAge : 0));
        }
        return h.absolutePos(new BlockPos(x, 1 + height, 2));
    }

    /** Native ages the top of a 3-tall cactus; 1.21.1 left it at age 0 forever. */
    @GameTest(template = T)
    public static void flora_cactus_top_of_three_ages(GameTestHelper h) {
        BlockPos top = cactusColumn(h, 2, 3, 0);
        ServerLevel level = h.getLevel();
        level.getBlockState(top).randomTick(level, top, RandomSource.create(1L));
        int age = level.getBlockState(top).getValue(CactusBlock.AGE);
        h.assertTrue(age == 1, "3-tall cactus top age after a random tick: " + age + ", native 1");

        BlockPos full = cactusColumn(h, 5, 3, 15);
        level.getBlockState(full).randomTick(level, full, RandomSource.create(1L));
        h.assertTrue(level.getBlockState(full).getValue(CactusBlock.AGE) == 15, "age 15 top of a 3-tall cactus must stay 15");
        h.assertTrue(level.getBlockState(full.above()).isAir(), "a 3-tall cactus must not grow taller");
        h.succeed();
    }

    /** Native: age-8 top of a 3-tall cactus rolls its 25% flower, and the age still goes to 9 that tick. */
    @GameTest(template = T)
    public static void flora_cactus_flower_at_height_three(GameTestHelper h) {
        h.assertTrue(VanillaBackport.COMMON_CONFIG.hasCactusFlowers.get(), "needs cactus_flowers=true");
        BlockPos top = cactusColumn(h, 2, 3, 8);
        ServerLevel level = h.getLevel();
        RecordingRandom random = new RecordingRandom(3L).forceDouble(0.2); // above 0.1 (1-2 tall chance), below 0.25
        level.getBlockState(top).randomTick(level, top, random);
        h.assertTrue(level.getBlockState(top.above()).is(ModBlocks.CACTUS_FLOWER.get()), "no flower on a 3-tall cactus with roll 0.2");
        int age = level.getBlockState(top).getValue(CactusBlock.AGE);
        h.assertTrue(age == 9, "cactus age after growing a flower: " + age + ", native 9");
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-05

    @GameTest(template = T, timeoutTicks = 200)
    public static void flora_cactus_patch_grows_flowers(GameTestHelper h) {
        ConfiguredFeature<?, ?> patch = reg(h, Registries.CONFIGURED_FEATURE).get(mc("patch_cactus"));
        Holder<PlacedFeature> selected = switchSelection(h, patch, "minecraft:patch_cactus");
        if (VanillaBackport.COMMON_CONFIG.hasCactusFlowers.get()) {
            RandomPatchConfiguration rp = (RandomPatchConfiguration) selected.value().feature().value().config();
            BlockColumnConfiguration column = (BlockColumnConfiguration) rp.feature().value().feature().value().config();
            h.assertTrue(column.layers().size() == 2, "cactus column layers: " + column.layers().size() + ", native 2 (cactus + flower)");
        }
        PlacedFeature vbPass = reg(h, Registries.PLACED_FEATURE).get(mc("cactus_flower"));
        h.assertTrue(vbPass.feature().value().feature() == net.minecraft.world.level.levelgen.feature.Feature.NO_OP, "VB's cactus_flower pass is still active");
        h.assertFalse(vegetal(h, Biomes.DESERT).contains("minecraft:cactus_flower"), "desert still runs VB's cactus_flower pass");

        floor(h, Blocks.SAND, 1, 16);
        ServerLevel level = h.getLevel();
        int cacti = 0, flowers = 0;
        for (int seed = 0; seed < 40; seed++) {
            patch.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), h.absolutePos(new BlockPos(8, 2, 8)));
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 2; y < 9; y++) {
                    BlockState s = h.getBlockState(new BlockPos(x, y, z));
                    if (s.is(Blocks.CACTUS) && !h.getBlockState(new BlockPos(x, y + 1, z)).is(Blocks.CACTUS)) {
                        cacti++;
                    }
                    if (s.is(ModBlocks.CACTUS_FLOWER.get())) {
                        flowers++;
                    }
                }
            }
        }
        h.assertTrue(cacti > 4, "setup: only " + cacti + " cacti placed");
        h.assertTrue(flowers > 0, "no cactus flowers in " + cacti + " generated cacti (native 1 in 4)");
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-06

    @GameTest(template = T)
    public static void flora_fallen_log_gap_limit(GameTestHelper h) {
        FallenTreeFeature feature = (FallenTreeFeature) BuiltInRegistries.FEATURE.get(mc("fallen_tree"));
        FallenTreeFeatureInvoker invoker = (FallenTreeFeatureInvoker) (Object) feature;
        // row z=2: ground at x=1, gap under x=2,3,4, ground at x=5,6 -> native rejects (3rd gap)
        // row z=5: ground at x=1, gap under x=2,3, ground at x=4,5,6 -> native accepts
        for (int x = 1; x <= 6; x++) {
            if (x < 2 || x > 4) {
                h.setBlock(x, 1, 2, Blocks.STONE);
            }
            if (x < 2 || x > 3) {
                h.setBlock(x, 1, 5, Blocks.STONE);
            }
        }
        boolean threeGaps = invoker.vbparity$canPlaceEntireFallenLog(h.getLevel(), 6, h.absolutePos(new BlockPos(1, 2, 2)).mutable(), Direction.EAST);
        boolean twoGaps = invoker.vbparity$canPlaceEntireFallenLog(h.getLevel(), 6, h.absolutePos(new BlockPos(1, 2, 5)).mutable(), Direction.EAST);
        h.assertFalse(threeGaps, "fallen log accepted over 3 unsupported blocks (native rejects)");
        h.assertTrue(twoGaps, "fallen log rejected over 2 unsupported blocks (native accepts)");
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-07

    @GameTest(template = T)
    public static void flora_fallen_trees_inside_tree_selectors(GameTestHelper h) {
        boolean fallen = VanillaBackport.COMMON_CONFIG.hasFallenTrees.get();
        boolean litter = VanillaBackport.COMMON_CONFIG.hasLeafLitter.get();
        h.assertTrue(fallen && litter, "needs fallen_trees=true and leaf_litter=true (VB defaults)");
        Registry<ConfiguredFeature<?, ?>> configured = reg(h, Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placed = reg(h, Registries.PLACED_FEATURE);

        String[][] expectConfigured = {
            {"trees_birch_and_oak", "minecraft:fallen_birch_tree@0.0025, minecraft:birch_bees_0002_leaf_litter@0.2, minecraft:fancy_oak_bees_0002_leaf_litter@0.1, minecraft:fallen_oak_tree@0.0125 | default=minecraft:oak_bees_0002_leaf_litter"},
            {"dark_forest_vegetation", "inline@0.025, inline@0.05, minecraft:dark_oak_leaf_litter@0.6666667, minecraft:fallen_birch_tree@0.0025, minecraft:birch_leaf_litter@0.2, minecraft:fallen_oak_tree@0.0125, minecraft:fancy_oak_leaf_litter@0.1 | default=minecraft:oak_leaf_litter"},
            {"trees_flower_forest", "minecraft:fallen_birch_tree@0.0025, minecraft:birch_bees_002@0.2, minecraft:fancy_oak_bees_002@0.1 | default=minecraft:oak_bees_002"},
            {"birch_tall", "minecraft:fallen_super_birch_tree@0.00625, minecraft:super_birch_bees_0002@0.5, minecraft:fallen_birch_tree@0.0125 | default=minecraft:birch_bees_0002"},
            {"trees_jungle", "minecraft:fancy_oak_checked@0.1, minecraft:jungle_bush@0.5, minecraft:mega_jungle_tree_checked@0.33333334, minecraft:fallen_jungle_tree@0.0125 | default=minecraft:jungle_tree"},
            {"trees_sparse_jungle", "minecraft:fancy_oak_checked@0.1, minecraft:jungle_bush@0.5, minecraft:fallen_jungle_tree@0.0125 | default=minecraft:jungle_tree"},
            {"trees_taiga", "minecraft:pine_checked@0.33333334, minecraft:fallen_spruce_tree@0.0125 | default=minecraft:spruce_checked"},
            {"trees_old_growth_pine_taiga", "minecraft:mega_spruce_checked@0.025641026, minecraft:mega_pine_checked@0.30769232, minecraft:pine_checked@0.33333334, minecraft:fallen_spruce_tree@0.0125 | default=minecraft:spruce_checked"},
            {"trees_old_growth_spruce_taiga", "minecraft:mega_spruce_checked@0.33333334, minecraft:pine_checked@0.33333334, minecraft:fallen_spruce_tree@0.0125 | default=minecraft:spruce_checked"},
            {"trees_plains", "inline@0.33333334, minecraft:fallen_oak_tree@0.0125 | default=inline"},
            {"trees_savanna", "minecraft:acacia_checked@0.8, minecraft:fallen_oak_tree@0.0125 | default=minecraft:oak_checked"},
            {"trees_windswept_hills", "minecraft:fallen_spruce_tree@0.008325, minecraft:spruce_checked@0.666, minecraft:fancy_oak_checked@0.1, minecraft:fallen_oak_tree@0.0125 | default=minecraft:oak_checked"},
        };
        for (String[] e : expectConfigured) {
            String got = describeSelector(switchSelection(h, configured.get(mc(e[0])), e[0]));
            h.assertTrue(got.equals(e[1]), e[0] + ": " + got + "  expected native " + e[1]);
        }
        String[][] expectPlaced = {
            {"trees_birch", "minecraft:fallen_birch_tree@0.0125 | default=minecraft:birch_bees_0002"},
            {"trees_snowy", "minecraft:fallen_spruce_tree@0.0125 | default=minecraft:spruce_checked"},
            {"trees_badlands", "minecraft:fallen_oak_tree@0.0125 | default=minecraft:oak_leaf_litter"},
        };
        for (String[] e : expectPlaced) {
            String got = describeSelector(switchSelection(h, placed.get(mc(e[0])).feature().value(), e[0]));
            h.assertTrue(got.equals(e[1]), e[0] + ": " + got + "  expected native " + e[1]);
        }
        for (String tag : List.of("spawns_fallen_oak_trees", "spawns_fallen_birch_trees", "spawns_fallen_super_birch_trees",
            "spawns_fallen_jungle_trees", "spawns_fallen_spruce_trees", "spawns_fallen_spruce_trees_rarely")) {
            h.assertFalse(reg(h, Registries.BIOME).getTag(TagKey.create(Registries.BIOME, mc(tag))).map(t -> t.size() > 0).orElse(false),
                "#" + tag + " still lists biomes, so VB adds its separate fallen-tree pass");
        }
        h.assertFalse(biomeIn(h, Biomes.FOREST, "spawns_fallen_birch_trees_rarely"), "forest still gets VB's rare fallen birch pass");
        for (ResourceKey<Biome> b : List.of(Biomes.FOREST, Biomes.TAIGA, Biomes.JUNGLE, Biomes.PLAINS, Biomes.BIRCH_FOREST)) {
            h.assertFalse(vegetal(h, b).stream().anyMatch(s -> s.contains("placed_fallen") || s.contains("placed_rare_fallen")),
                b.location() + " still runs a separate fallen-tree feature: " + vegetal(h, b));
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-08

    @GameTest(template = T, timeoutTicks = 200, skyAccess = true) // the leaf-litter decorator reads the heightmap; no barrier roof
    public static void flora_leaf_litter_from_tree_decorators(GameTestHelper h) {
        for (ResourceKey<Biome> b : List.of(Biomes.FOREST, Biomes.DARK_FOREST, Biomes.WOODED_BADLANDS)) {
            List<String> v = vegetal(h, b);
            h.assertFalse(v.contains("minecraft:leaf_litter_sparse") || v.contains("minecraft:leaf_litter_thick"),
                b.location() + " still runs VB's chunk-wide leaf-litter scan");
        }
        h.assertTrue(vegetal(h, Biomes.DARK_FOREST).contains("minecraft:patch_leaf_litter"), "dark forest lost patch_leaf_litter");

        floor(h, Blocks.GRASS_BLOCK, 1, 16);
        ServerLevel level = h.getLevel();
        PlacedFeature oakLitter = reg(h, Registries.PLACED_FEATURE).get(mc("oak_leaf_litter"));
        BlockPos origin = h.absolutePos(new BlockPos(8, 2, 8));
        boolean placed = false;
        for (int seed = 0; seed < 20 && !placed; seed++) {
            placed = oakLitter.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
        }
        h.assertTrue(placed, "setup: oak_leaf_litter tree did not place");
        int litter = 0, logs = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 10; y++) {
                    BlockState s = h.getBlockState(new BlockPos(x, y, z));
                    if (s.is(ModBlocks.LEAF_LITTER.get())) {
                        litter++;
                    } else if (s.is(Blocks.OAK_LOG)) {
                        logs++;
                    }
                }
            }
        }
        h.assertTrue(litter > 0, "oak_leaf_litter placed a tree (" + logs + " logs) but no leaf litter around it");
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-09 / 10 / 11

    @GameTest(template = T)
    public static void flora_bush_patch_and_biomes(GameTestHelper h) {
        RandomPatchConfiguration bush = (RandomPatchConfiguration) reg(h, Registries.CONFIGURED_FEATURE).get(mc("patch_bush")).config();
        h.assertTrue(bush.tries() == 24, "patch_bush tries " + bush.tries() + ", native 24");
        for (ResourceKey<Biome> b : List.of(Biomes.PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_GRAVELLY_HILLS)) {
            h.assertTrue(biomeIn(h, b, "spawns_bushes"), b.location() + " should grow bushes");
        }
        h.assertFalse(biomeIn(h, Biomes.SUNFLOWER_PLAINS, "spawns_bushes"), "sunflower_plains grows bushes, native does not");
        h.succeed();
    }

    @GameTest(template = T)
    public static void flora_firefly_bush_biomes(GameTestHelper h) {
        for (ResourceKey<Biome> b : List.of(Biomes.ICE_SPIKES, Biomes.SNOWY_PLAINS, Biomes.STONY_SHORE, Biomes.FOREST, Biomes.WARM_OCEAN,
            Biomes.MANGROVE_SWAMP, Biomes.SUNFLOWER_PLAINS, Biomes.MUSHROOM_FIELDS, Biomes.SNOWY_BEACH, Biomes.WOODED_BADLANDS)) {
            h.assertTrue(biomeIn(h, b, "spawns_firefly_bushes"), b.location() + " should get firefly bushes near water");
        }
        h.assertFalse(biomeIn(h, Biomes.GROVE, "spawns_firefly_bushes"), "grove gets firefly bushes, native does not");
        h.assertTrue(vegetal(h, Biomes.ICE_SPIKES).contains("minecraft:patch_firefly_bush_near_water"), "ice_spikes biome lacks the firefly patch");
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-12

    @GameTest(template = T)
    public static void flora_vegetation_in_native_order(GameTestHelper h) {
        h.assertFalse(VegetalOrderModifier.fallbackUsed, "feature order fell back to VB's order");
        List<String> birch = vegetal(h, Biomes.BIRCH_FOREST);
        int wild = birch.indexOf("minecraft:wildflowers_birch_forest"), trees = birch.indexOf("minecraft:trees_birch"),
            bush = birch.indexOf("minecraft:patch_bush"), flowers = birch.indexOf("minecraft:flower_default"),
            forestFlowers = birch.indexOf("minecraft:forest_flowers");
        h.assertTrue(forestFlowers >= 0 && forestFlowers < wild && wild < trees && trees < bush && bush < flowers,
            "birch_forest order (native forest_flowers, wildflowers, trees_birch, patch_bush, flower_default): " + birch);
        List<String> plains = vegetal(h, Biomes.PLAINS);
        h.assertTrue(plains.indexOf("minecraft:patch_bush") >= 0 && plains.indexOf("minecraft:patch_bush") < plains.indexOf("minecraft:trees_plains"),
            "plains: patch_bush must come before trees_plains: " + plains);
        List<String> meadow = vegetal(h, Biomes.MEADOW);
        h.assertTrue(meadow.indexOf("minecraft:wildflowers_meadow") > meadow.indexOf("minecraft:trees_meadow"),
            "meadow: wildflowers come last, after trees_meadow: " + meadow);
        h.succeed();
    }

    /** The moved order must still sort across every overworld biome (the noise generator does this at world start). */
    @GameTest(template = T)
    public static void flora_vegetation_order_sorts_without_fallback(GameTestHelper h) {
        Registry<Biome> biomes = reg(h, Registries.BIOME);
        List<Holder<Biome>> overworld = new java.util.ArrayList<>(biomes.holders()
            .filter(b -> b.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD) || b.is(mc("pale_garden")) || b.is(mc("dappled_forest")))
            .toList());
        h.assertTrue(overworld.size() > 50, "setup: only " + overworld.size() + " overworld biomes");
        boolean before = VegetalOrderModifier.fallbackUsed;
        net.minecraft.world.level.biome.FeatureSorter.buildFeaturesPerStep(overworld, b -> b.value().getGenerationSettings().features(), true);
        h.assertTrue(before || !VegetalOrderModifier.fallbackUsed, "native flora order made the overworld feature order cyclic");
        h.assertFalse(VegetalOrderModifier.fallbackUsed, "feature sorting fell back to VB's order");
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-13

    @GameTest(template = T, timeoutTicks = 200)
    public static void flora_bonemeal_grass_gives_wildflowers(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Holder<Biome> birch = reg(h, Registries.BIOME).getHolderOrThrow(Biomes.BIRCH_FOREST);
        BlockPos min = h.absolutePos(BlockPos.ZERO), max = h.absolutePos(new BlockPos(15, 9, 15));
        for (int cx = (min.getX() >> 4) - 1; cx <= (max.getX() >> 4) + 1; cx++) {
            for (int cz = (min.getZ() >> 4) - 1; cz <= (max.getZ() >> 4) + 1; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                chunk.fillBiomesFromNoise((x, y, z, sampler) -> birch, level.getChunkSource().randomState().sampler());
                chunk.setUnsaved(true);
            }
        }
        BlockPos center = h.absolutePos(new BlockPos(8, 1, 8));
        h.assertTrue(level.getBiome(center.above()).is(Biomes.BIRCH_FOREST), "setup: biome fill failed");
        floor(h, Blocks.GRASS_BLOCK, 1, 16);
        int wildflowers = 0, others = 0;
        GrassBlock grass = (GrassBlock) Blocks.GRASS_BLOCK;
        for (int seed = 0; seed < 40; seed++) {
            grass.performBonemeal(level, RandomSource.create(seed), center, Blocks.GRASS_BLOCK.defaultBlockState());
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 2; y < 4; y++) {
                        BlockPos p = new BlockPos(x, y, z);
                        BlockState s = h.getBlockState(p);
                        if (s.is(ModBlocks.WILDFLOWERS.get())) {
                            wildflowers++;
                        } else if (s.is(Blocks.DANDELION) || s.is(Blocks.POPPY)) {
                            others++;
                        }
                        if (!s.isAir()) {
                            h.setBlock(p, Blocks.AIR);
                        }
                    }
                }
            }
        }
        h.assertTrue(wildflowers > 0 && others > 0,
            "bone meal in birch forest gave " + wildflowers + " wildflowers and " + others + " dandelions/poppies; native picks either");
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-14

    @GameTest(template = T)
    public static void flora_sheep_eat_dry_grass(GameTestHelper h) {
        for (Block food : List.of(ModBlocks.SHORT_DRY_GRASS.get(), ModBlocks.TALL_DRY_GRASS.get(), Blocks.FERN)) {
            h.setBlock(2, 1, 2, food == Blocks.FERN ? Blocks.DIRT : Blocks.SAND);
            h.setBlock(2, 2, 2, food);
            h.setBlock(2, 3, 2, Blocks.AIR);
            Sheep sheep = h.spawnWithNoFreeWill(EntityType.SHEEP, new Vec3(2.5, 2.0, 2.5));
            sheep.setSheared(true);
            EatBlockGoal goal = new EatBlockGoal(sheep);
            boolean canUse = false;
            for (int i = 0; i < 20000 && !canUse; i++) {
                canUse = goal.canUse();
            }
            h.assertTrue(canUse, "sheep never wants to eat " + food);
            goal.start();
            for (int i = 0; i < 40; i++) {
                goal.tick();
            }
            h.assertFalse(h.getBlockState(new BlockPos(2, 2, 2)).is(food), "sheep did not eat " + food);
            h.assertFalse(sheep.isSheared(), "sheep did not regrow wool after eating " + food);
            sheep.discard();
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-15 (needs the shared #replaceable entries)

    @GameTest(template = T)
    public static void flora_replaceable_flora_transmits_enchant_power(GameTestHelper h) {
        floor(h, Blocks.STONE, 1, 5);
        BlockPos table = new BlockPos(1, 2, 2);
        h.setBlock(table, Blocks.ENCHANTING_TABLE);
        h.setBlock(3, 2, 2, Blocks.BOOKSHELF);
        for (Block b : List.of(ModBlocks.BUSH.get(), ModBlocks.LEAF_LITTER.get(), ModBlocks.SHORT_DRY_GRASS.get(), ModBlocks.TALL_DRY_GRASS.get())) {
            h.setBlock(2, 1, 2, b == ModBlocks.BUSH.get() ? Blocks.GRASS_BLOCK : Blocks.SAND);
            h.setBlock(2, 2, 2, b);
            h.assertTrue(EnchantingTableBlock.isValidBookShelf(h.getLevel(), h.absolutePos(table), new BlockPos(2, 0, 0)),
                b + " between bookshelf and table blocks enchanting power (native: #replaceable transmits it)");
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-16

    @GameTest(template = T)
    public static void flora_dry_grass_on_farmland(GameTestHelper h) {
        h.setBlock(2, 1, 2, Blocks.FARMLAND);
        BlockPos above = h.absolutePos(new BlockPos(2, 2, 2));
        for (Block b : List.of(ModBlocks.SHORT_DRY_GRASS.get(), ModBlocks.TALL_DRY_GRASS.get(), Blocks.DEAD_BUSH)) {
            h.assertTrue(b.defaultBlockState().canSurvive(h.getLevel(), above), b + " cannot stand on farmland (native can)");
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- FLORA-17 / 18 (tags read by VB's client falling-leaves module)

    @GameTest(template = T)
    public static void flora_falling_leaf_tags(GameTestHelper h) {
        TagKey<Block> leaves = TagKey.create(Registries.BLOCK, mc("spawn_falling_leaves"));
        TagKey<Block> needles = TagKey.create(Registries.BLOCK, mc("spawn_falling_needles"));
        h.assertTrue(Blocks.MANGROVE_LEAVES.defaultBlockState().is(leaves), "mangrove leaves do not drop falling leaves");
        h.assertTrue(Blocks.SPRUCE_LEAVES.defaultBlockState().is(leaves), "spruce leaves do not drop the standard falling leaves");
        h.assertFalse(Blocks.SPRUCE_LEAVES.defaultBlockState().is(needles), "spruce leaves still drop VB-only needles");
        h.assertTrue(Blocks.OAK_LEAVES.defaultBlockState().is(leaves), "oak leaves lost their falling leaves");
        h.succeed();
    }
}
