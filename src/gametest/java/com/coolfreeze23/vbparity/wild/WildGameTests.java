package com.coolfreeze23.vbparity.wild;

import com.blackgear.vanillabackport.common.level.blocks.StrawBedBlock;
import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import com.blackgear.vanillabackport.common.level.worldgen.features.WeightedRandomFeatureConfiguration;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.blackgear.vanillabackport.common.worldgen.surface.ModSurfaceRuleData;
import com.coolfreeze23.vbparity.wild.worldgen.ConfigSwitchFeature;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public class WildGameTests {
    private static final String PLATFORM = "wild_platform";

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static <T> T registryValue(GameTestHelper helper, ResourceKey<? extends net.minecraft.core.Registry<T>> registry, String id) {
        return helper.getLevel().registryAccess().registryOrThrow(registry).getOrThrow(ResourceKey.create(registry, mc(id)));
    }

    private static String json(GameTestHelper helper, com.mojang.serialization.Codec<?> codec, Object value) {
        @SuppressWarnings("unchecked")
        com.mojang.serialization.Codec<Object> c = (com.mojang.serialization.Codec<Object>) codec;
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        return c.encodeStart(ops, value).getOrThrow().toString();
    }

    // ---- WILD-01 climate placement -----------------------------------------------------------------------------

    @GameTest(template = PLATFORM)
    public static void wild_dappled_forest_climate(GameTestHelper helper) {
        Climate.ParameterList<Holder<Biome>> params = helper.getLevel().registryAccess()
            .registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
            .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
            .parameters();
        Climate.Parameter cool = Climate.Parameter.span(-0.45F, -0.15F);
        Climate.Parameter arid = Climate.Parameter.span(-1.0F, -0.35F);
        boolean floorPoint = false;
        for (Pair<Climate.ParameterPoint, Holder<Biome>> entry : params.values()) {
            Climate.ParameterPoint point = entry.getFirst();
            if (entry.getSecond().is(ModBiomes.DAPPLED_FOREST)) {
                check(point.temperature().equals(cool) && point.humidity().equals(arid), "dappled forest point outside cool/arid: " + point);
                floorPoint |= point.depth().min() == 10000L;
            } else if (entry.getSecond().is(net.minecraft.world.level.biome.Biomes.PLAINS)) {
                check(!(point.temperature().equals(cool) && point.humidity().equals(arid) && point.weirdness().max() >= 0L),
                    "plains still has a positive-weirdness cool/arid point (dappled forest tie): " + point);
            }
        }
        check(floorPoint, "dappled forest has no floor-depth point");
        // Low slice (weirdness 0.15), near inland, erosion 2: native middle biome -> dappled forest; VB had no point here.
        Holder<Biome> lowSlice = params.findValue(Climate.target(-0.3F, -0.7F, -0.05F, -0.3F, 0.0F, 0.15F));
        check(lowSlice.is(ModBiomes.DAPPLED_FOREST), "low-slice cool/arid cell is " + lowSlice.getRegisteredName());
        // Mid slice (weirdness 0.3), mid inland, erosion 2: VB tied plains and dappled forest here; at floor depth plains won.
        Holder<Biome> midSurface = params.findValue(Climate.target(-0.3F, -0.7F, 0.2F, -0.3F, 0.0F, 0.3F));
        Holder<Biome> midFloor = params.findValue(Climate.target(-0.3F, -0.7F, 0.2F, -0.3F, 1.0F, 0.3F));
        check(midSurface.is(ModBiomes.DAPPLED_FOREST), "mid-slice surface cell is " + midSurface.getRegisteredName());
        check(midFloor.is(ModBiomes.DAPPLED_FOREST), "mid-slice floor cell is " + midFloor.getRegisteredName());
        helper.succeed();
    }

    // ---- WILD-02 surface rule ----------------------------------------------------------------------------------

    @GameTest(template = PLATFORM)
    public static void wild_dappled_surface_rule_on_floor(GameTestHelper helper) throws ReflectiveOperationException {
        SurfaceRules.RuleSource rules = ModSurfaceRuleData.makeRules();
        RecordComponent sequence = rules.getClass().getRecordComponents()[0];
        sequence.getAccessor().setAccessible(true);
        SurfaceRules.RuleSource dappled = ((List<SurfaceRules.RuleSource>) sequence.getAccessor().invoke(rules)).get(0);
        String rule = json(helper, SurfaceRules.RuleSource.CODEC, dappled);
        check(rule.contains("minecraft:dappled_forest") && rule.contains("minecraft:coarse_dirt"), "first VB rule is not the dappled forest rule: " + rule);
        check(rule.contains("minecraft:above_preliminary_surface"), "coarse dirt rule not limited to above the preliminary surface: " + rule);
        check(rule.contains("\"surface_type\":\"floor\"") && rule.contains("minecraft:stone_depth"), "coarse dirt rule not limited to the floor block: " + rule);
        check(rule.contains("minecraft:water"), "coarse dirt rule not limited to dry floors: " + rule);
        helper.succeed();
    }

    // ---- WILD-03 / 04 / 05 / 06 dappled forest features ----------------------------------------------------------

    @GameTest(template = PLATFORM)
    public static void wild_dappled_forest_features(GameTestHelper helper) {
        Holder<Biome> biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ModBiomes.DAPPLED_FOREST);
        List<String> vegetal = new ArrayList<>();
        biome.value().getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal())
            .forEach(h -> vegetal.add(h.getRegisteredName()));
        check(vegetal.contains("minecraft:trees_dappled_forest"), "no trees_dappled_forest: " + vegetal);
        for (String banned : List.of("minecraft:fallen_poplar_tree", "minecraft:patch_firefly_bush_near_water",
            "minecraft:placed_rare_fallen_birch_tree", "minecraft:rare_fallen_birch_tree", "minecraft:leaf_litter_thick", "minecraft:leaf_litter_sparse")) {
            check(!vegetal.contains(banned), "dappled forest still has " + banned + ": " + vegetal);
        }
        check(biome.is(BiomeTags.HAS_TRIAL_CHAMBERS), "dappled forest is not in has_structure/trial_chambers");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wild_dappled_tree_selector(GameTestHelper helper) {
        ConfiguredFeature<?, ?> trees = registryValue(helper, Registries.CONFIGURED_FEATURE, "trees_dappled_forest");
        check(trees.config() instanceof ConfigSwitchFeature.Config, "trees_dappled_forest is not the config switch: " + trees.feature());
        ConfigSwitchFeature.Config sw = (ConfigSwitchFeature.Config) trees.config();
        check(sw.key().equals("fallen_trees"), "switch key " + sw.key());
        WeightedRandomFeatureConfiguration selector = (WeightedRandomFeatureConfiguration) sw.on().value().feature().value().config();
        List<WeightedEntry.Wrapper<Holder<PlacedFeature>>> entries = selector.features().unwrap();
        List<Integer> weights = entries.stream().map(e -> e.getWeight().asInt()).toList();
        check(weights.equals(List.of(200, 240, 90, 27, 120)), "selector weights " + weights);
        check(entries.get(4).data().is(ResourceKey.create(Registries.PLACED_FEATURE, mc("fallen_poplar_tree"))), "5th entry is not fallen_poplar_tree");
        ConfigSwitchFeature.Config red = (ConfigSwitchFeature.Config) entries.get(0).data().value().feature().value().config();
        check(red.key().equals("leaf_litter") && red.on().is(ResourceKey.create(Registries.PLACED_FEATURE, mc("red_poplar_leaf_litter")))
            && red.off().is(ResourceKey.create(Registries.PLACED_FEATURE, mc("red_poplar"))), "red poplar entry is not the leaf-litter switch");
        PlacedFeature fallen = registryValue(helper, Registries.PLACED_FEATURE, "fallen_poplar_tree");
        check(fallen.placement().size() == 1, "fallen_poplar_tree placement has " + fallen.placement().size() + " modifiers");
        for (String color : List.of("red", "orange", "yellow")) {
            ConfiguredFeature<?, ?> tree = registryValue(helper, Registries.CONFIGURED_FEATURE, color + "_poplar_leaf_litter");
            String decorators = json(helper, TreeConfiguration.CODEC, tree.config());
            int first = decorators.indexOf("\"tries\":96");
            int second = decorators.indexOf("\"tries\":150");
            int shelf = decorators.indexOf("minecraft:shelf_mushroom");
            check(first >= 0 && second > first && shelf > second, color + " poplar leaf litter decorators out of order: " + decorators);
        }
        helper.succeed();
    }

    // ---- WILD-07 soil under the poplar trunk ---------------------------------------------------------------------

    private static List<Pair<BlockPos, BlockState>> growTrunk(GameTestHelper helper, BlockState soil, BlockPos relative) {
        BlockPos ground = helper.absolutePos(relative);
        helper.getLevel().setBlockAndUpdate(ground, soil);
        TreeConfiguration config = (TreeConfiguration) ((ConfiguredFeature<?, ?>) registryValue(helper, Registries.CONFIGURED_FEATURE, "red_poplar")).config();
        List<Pair<BlockPos, BlockState>> placed = new ArrayList<>();
        BiConsumer<BlockPos, BlockState> recorder = (pos, state) -> placed.add(Pair.of(pos.immutable(), state));
        RandomSource random = RandomSource.create(42L);
        config.trunkPlacer.placeTrunk(helper.getLevel(), recorder, random, config.trunkPlacer.getTreeHeight(random), ground.above(), config);
        return placed;
    }

    @GameTest(template = PLATFORM)
    public static void wild_poplar_soil_beneath_trunk(GameTestHelper helper) {
        BlockPos grassGround = helper.absolutePos(new BlockPos(2, 0, 2));
        List<Pair<BlockPos, BlockState>> onGrass = growTrunk(helper, Blocks.GRASS_BLOCK.defaultBlockState(), new BlockPos(2, 0, 2));
        check(onGrass.stream().anyMatch(p -> p.getFirst().equals(grassGround) && p.getSecond().is(Blocks.DIRT)), "grass under a poplar trunk not turned into dirt");
        BlockPos podzolGround = helper.absolutePos(new BlockPos(6, 0, 6));
        List<Pair<BlockPos, BlockState>> onPodzol = growTrunk(helper, Blocks.PODZOL.defaultBlockState(), new BlockPos(6, 0, 6));
        check(onPodzol.stream().noneMatch(p -> p.getFirst().equals(podzolGround)), "podzol under a poplar trunk was replaced");
        helper.succeed();
    }

    // ---- WILD-08..13, 39 camp chests ----------------------------------------------------------------------------

    private static LootTable lootTable(GameTestHelper helper, String id) {
        return helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, mc(id)));
    }

    private static List<ItemStack> roll(GameTestHelper helper, LootTable table, int times) {
        LootParams params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 1, 4))))
            .create(LootContextParamSets.CHEST);
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            table.getRandomItems(params).forEach(s -> {
                if (!s.isEmpty()) {
                    out.add(s);
                }
            });
        }
        return out;
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void wild_camp_common_chest(GameTestHelper helper) {
        LootTable table = lootTable(helper, "chests/abandoned_camp_common_chest");
        String text = json(helper, LootTable.DIRECT_CODEC, table);
        for (String copper : List.of("copper_axe", "copper_boots", "copper_chestplate", "copper_leggings", "copper_spear", "copper_sword")) {
            check(text.contains("minecraft:" + copper), "common chest lacks " + copper);
        }
        check(!text.contains("\"zoom\":1"), "camp maps still zoom 1");
        check(text.contains("minecraft:on_abandoned_camp_windswept\""), "windswept camp map does not use #on_abandoned_camp_windswept");
        check(text.contains("minecraft:set_custom_model_data") && text.contains("minecraft:filtered"), "camp maps lack icon or discard");
        List<ItemStack> items = roll(helper, table, 60);
        check(items.stream().anyMatch(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("copper_")), "no copper gear in 60 common chests");
        check(items.stream().noneMatch(s -> s.is(Items.MAP) && s.has(DataComponents.ITEM_NAME)), "an unresolved camp map stayed in the chest");
        TagKey<net.minecraft.world.level.levelgen.structure.Structure> windswept =
            TagKey.create(Registries.STRUCTURE, mc("on_abandoned_camp_windswept"));
        check(helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(windswept).map(t -> t.size() == 1).orElse(false),
            "#on_abandoned_camp_windswept missing or not one structure");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void wild_camp_secret_chest(GameTestHelper helper) {
        LootTable table = lootTable(helper, "chests/abandoned_camp_secret_chest");
        String text = json(helper, LootTable.DIRECT_CODEC, table);
        check(text.contains("minecraft:iron_spear"), "secret chest lacks the iron spear");
        check(!text.contains("\"zoom\":1"), "secret maps still zoom 1");
        for (String name : List.of("buried_ancient_city_map", "buried_trial_chambers_map", "buried_mineshaft_map", "desert_pyramid_map",
            "jungle_pyramid_map", "warm_ocean_ruins_map", "woodland_mansion_map")) {
            check(text.contains("item.minecraft." + name), "secret chest map not named " + name);
        }
        List<ItemStack> items = roll(helper, table, 200);
        check(items.stream().anyMatch(s -> s.is(BuiltInRegistries.ITEM.get(mc("iron_spear")))), "no iron spear in 200 secret chests");
        // Pool 1 has plain maps (count 1) too; pool 3 adds one exploration map per chest, all unresolvable here, so
        // without the discard every chest would hold an unnamed blank map from pool 3 plus pool 1's.
        long maps = items.stream().filter(s -> s.is(Items.MAP)).count();
        check(maps < 350, "unresolved secret maps were not discarded (" + maps + " blank maps in 200 chests)");
        helper.succeed();
    }

    // ---- WILD-14 camp templates ---------------------------------------------------------------------------------

    private static StructureTemplate template(GameTestHelper helper, String path) {
        Optional<StructureTemplate> t = helper.getLevel().getStructureManager().get(mc("abandoned_camp/" + path));
        check(t.isPresent(), "missing template " + path);
        return t.get();
    }

    @GameTest(template = PLATFORM)
    public static void wild_camp_templates(GameTestHelper helper) {
        StructurePlaceSettings settings = new StructurePlaceSettings();
        int beds = template(helper, "tent/forest/tent_forest_10").filterBlocks(BlockPos.ZERO, settings, ModBlocks.STRAW_BED.get()).size();
        check(beds == 2, "tent_forest_10 has " + beds + " straw bed blocks");
        for (StructureTemplate.StructureBlockInfo jigsaw : template(helper, "camp/dappled_forest/campsite_dappled_forest_1")
            .filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW)) {
            check(jigsaw.nbt() != null && !"minecraft:down".equals(jigsaw.nbt().getString("target")),
                "dappled camp tree jigsaw targets minecraft:down, which no 1.21.1 feature element matches");
        }
        boolean junglePool = template(helper, "tent/sparse_jungle/tent_sparse_jungle_2").filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW)
            .stream().anyMatch(j -> j.nbt() != null && j.nbt().getString("pool").equals("minecraft:abandoned_camp/trees/jungle"));
        check(junglePool, "tent_sparse_jungle_2 tree jigsaw does not use the jungle pool");
        helper.succeed();
    }

    // ---- WILD-15 / 16 / 22 straw bed sleeping ------------------------------------------------------------------

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void wild_straw_bed_sleep(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Bed: foot (4,1,3) -> head (4,1,4), facing south. Walls on every side cell so only the "above" stand-up spots fit.
        BlockPos foot = new BlockPos(4, 1, 3);
        BlockPos head = new BlockPos(4, 1, 4);
        for (int x = 2; x <= 6; x++) {
            for (int z = 1; z <= 6; z++) {
                BlockPos p = new BlockPos(x, 1, z);
                if (!p.equals(foot) && !p.equals(head)) {
                    helper.setBlock(p, Blocks.STONE);
                    helper.setBlock(p.above(), Blocks.STONE);
                }
            }
        }
        helper.setBlock(foot.below(), Blocks.STONE);
        helper.setBlock(head.below(), Blocks.STONE);
        BlockState bed = ModBlocks.STRAW_BED.get().defaultBlockState().setValue(StrawBedBlock.FACING, Direction.SOUTH);
        helper.setBlock(foot, bed.setValue(StrawBedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, bed.setValue(StrawBedBlock.PART, BedPart.HEAD));
        long dayTime = level.getDayTime();
        level.setDayTime(18000L);
        level.updateSkyBrightness();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 standOn = Vec3.atBottomCenterOf(helper.absolutePos(head)).add(0.0, 0.25, 0.0);
        player.teleportTo(level, standOn.x, standOn.y, standOn.z, 0.0F, 0.0F);
        helper.runAfterDelay(2, () -> {
            try {
                BlockPos headAbs = helper.absolutePos(head);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(headAbs), Direction.UP, headAbs, false);
                level.getBlockState(headAbs).useWithoutItem(level, player, hit);
                check(player.isSleeping(), "player did not fall asleep in the straw bed");
                int bedStat = player.getStats().getValue(Stats.CUSTOM.get(Stats.SLEEP_IN_BED));
                int strawStat = player.getStats().getValue(Stats.CUSTOM.get(WildRegistry.SLEEP_IN_STRAW_BED));
                check(bedStat == 0 && strawStat == 1, "stats: sleep_in_bed " + bedStat + ", sleep_in_straw_bed " + strawStat);
                AdvancementHolder sweetDreams = level.getServer().getAdvancements().get(mc("adventure/sleep_in_bed"));
                check(sweetDreams == null || !player.getAdvancements().getOrStartProgress(sweetDreams).isDone(), "straw bed granted Sweet Dreams");
                double sleepY = player.getY() - headAbs.getY();
                check(Math.abs(sleepY - 0.375) < 1.0E-6, "sleeping height " + sleepY + " (native 0.375)");
                player.stopSleepInBed(true, true);
                check(level.getBlockState(headAbs).isAir() && level.getBlockState(helper.absolutePos(foot)).isAir(), "straw bed not removed on waking");
                double standY = player.getY() - headAbs.getY();
                check(Math.abs(standY) < 1.0E-6, "stood up at " + standY + " above the bed block (native: on the floor where the bed was)");
            } finally {
                level.setDayTime(dayTime);
                level.updateSkyBrightness();
                level.getServer().getPlayerList().remove(player);
            }
            helper.succeed();
        });
    }

    // ---- WILD-17 / 18 / 19 straw bed blocks -----------------------------------------------------------------------

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void wild_straw_bed_washed_away(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            for (int y = 1; y <= 2; y++) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
                helper.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
            }
        }
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(new BlockPos(0, y, 4), Blocks.STONE);
            helper.setBlock(new BlockPos(6, y, 4), Blocks.STONE);
        }
        for (int x = 1; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE);
        }
        BlockPos foot = new BlockPos(2, 1, 4);
        BlockPos head = new BlockPos(3, 1, 4);
        BlockState bed = ModBlocks.STRAW_BED.get().defaultBlockState().setValue(StrawBedBlock.FACING, Direction.EAST);
        helper.setBlock(foot, bed.setValue(StrawBedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, bed.setValue(StrawBedBlock.PART, BedPart.HEAD));
        helper.setBlock(new BlockPos(1, 1, 4), Blocks.WATER);
        helper.succeedWhen(() -> {
            check(!helper.getBlockState(foot).is(ModBlocks.STRAW_BED.get()), "water did not wash the straw bed away");
            check(!helper.getBlockState(head).is(ModBlocks.STRAW_BED.get()), "straw bed head left behind");
        });
    }

    @GameTest(template = PLATFORM)
    public static void wild_straw_bed_and_shelf_mushroom_blocks(GameTestHelper helper) {
        check(!ModBlocks.STRAW_BED.get().defaultBlockState().isPathfindable(PathComputationType.LAND), "straw bed is pathfindable");
        check(!ModBlocks.SHELF_MUSHROOM.get().defaultBlockState().isPathfindable(PathComputationType.LAND), "shelf mushroom is pathfindable");
        Compostable compost = ModBlocks.STRAW_BED.get().asItem().builtInRegistryHolder().getData(NeoForgeDataMaps.COMPOSTABLES);
        check(compost != null && Math.abs(compost.chance() - 0.85F) < 1.0E-6F, "straw bed compost chance " + (compost == null ? "none" : compost.chance()));
        helper.succeed();
    }

    // ---- WILD-25 / 37 / 38 tags --------------------------------------------------------------------------------

    @GameTest(template = PLATFORM)
    public static void wild_tags(GameTestHelper helper) {
        BlockState shrub = ModBlocks.RED_SHRUB.get().defaultBlockState();
        check(shrub.is(BlockTags.create(mc("replaceable_by_mushrooms"))), "red shrub not in #replaceable_by_mushrooms");
        check(shrub.is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER), "red shrub not in #enchantment_power_transmitter");
        TagKey<Item> green = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "dyed/green"));
        check(new ItemStack(ModItems.GREEN_CUSHION.get()).is(green) && !new ItemStack(ModItems.GRAY_CUSHION.get()).is(green), "#c:dyed/green cushion entry");
        Block concreteSlab = BuiltInRegistries.BLOCK.get(mc("white_concrete_slab"));
        Block concreteStairs = BuiltInRegistries.BLOCK.get(mc("white_concrete_stairs"));
        check(concreteSlab.defaultBlockState().is(BlockTags.SLABS) && new ItemStack(concreteSlab).is(ItemTags.SLABS), "concrete slab not in #slabs");
        check(concreteStairs.defaultBlockState().is(BlockTags.STAIRS) && new ItemStack(concreteStairs).is(ItemTags.STAIRS), "concrete stairs not in #stairs");
        helper.succeed();
    }

    // ---- WILD-26 trades ----------------------------------------------------------------------------------------

    private static boolean sells(List<MerchantOffer> offers, Item item, int emeralds, int count, int maxUses) {
        return offers.stream().anyMatch(o -> o.getResult().is(item) && o.getResult().getCount() == count && o.getBaseCostA().is(Items.EMERALD)
            && o.getBaseCostA().getCount() == emeralds && o.getMaxUses() == maxUses && o.getXp() == 1 && Math.abs(o.getPriceMultiplier() - 0.05F) < 1.0E-6F);
    }

    private static List<MerchantOffer> offers(VillagerTrades.ItemListing[] listings, WanderingTrader trader) {
        List<MerchantOffer> out = new ArrayList<>();
        RandomSource random = RandomSource.create(7L);
        for (VillagerTrades.ItemListing listing : listings) {
            try {
                MerchantOffer offer = listing.getOffer(trader, random);
                if (offer != null) {
                    out.add(offer);
                }
            } catch (RuntimeException ignored) {
                // Listings that need a world context the test trader lacks are not ours.
            }
        }
        return out;
    }

    @GameTest(template = PLATFORM)
    public static void wild_wandering_trader_trades(GameTestHelper helper) {
        WanderingTrader trader = EntityType.WANDERING_TRADER.create(helper.getLevel());
        List<MerchantOffer> generic = offers(VillagerTrades.WANDERING_TRADER_TRADES.get(1), trader);
        List<MerchantOffer> rare = offers(VillagerTrades.WANDERING_TRADER_TRADES.get(2), trader);
        check(sells(rare, ModBlocks.POPLAR_LOG.get().asItem(), 1, 8, 4), "no poplar log trade (1 emerald, 8 logs, 4 uses) in the rare list");
        check(sells(generic, ModBlocks.POPLAR_SAPLING.get().asItem(), 5, 1, 8), "no poplar sapling trade (5 emeralds, 1, 8 uses)");
        check(sells(generic, ModBlocks.SHELF_MUSHROOM.get().asItem(), 1, 3, 12), "no shelf mushroom trade (1 emerald, 3, 12 uses)");
        helper.succeed();
    }

    // ---- WILD-30 / 32 cushion -----------------------------------------------------------------------------------

    private static Cushion cushion(GameTestHelper helper, BlockPos relative) {
        Cushion cushion = ModEntityTypes.CUSHION.get().create(helper.getLevel());
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(relative));
        cushion.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cushion);
        return cushion;
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void wild_cushion_fire_interval(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.SOUL_SOIL);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.SOUL_FIRE);
        Cushion cushion = cushion(helper, new BlockPos(4, 1, 4));
        helper.runAfterDelay(20, () -> check(!cushion.isRemoved(), "cushion in fire broke before the 100-tick check (VB: every tick)"));
        helper.runAfterDelay(21, () -> helper.succeedWhen(() -> check(cushion.isRemoved(), "cushion in fire never broke")));
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void wild_cushion_lava(GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                if (x != 4 || z != 4) {
                    helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                }
            }
        }
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.LAVA);
        Cushion cushion = cushion(helper, new BlockPos(4, 1, 4));
        helper.succeedWhen(() -> check(cushion.isRemoved(), "cushion in lava never broke"));
    }

    @GameTest(template = PLATFORM)
    public static void wild_cushion_lightning_drop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cushion cushion = cushion(helper, new BlockPos(4, 1, 4));
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        bolt.moveTo(cushion.position());
        cushion.thunderHit(level, bolt);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(2.0));
        check(drops.size() == 1, drops.size() + " items dropped");
        ItemEntity drop = drops.get(0);
        check(((TemporarilyInvulnerable) drop).vbparity$getInvulnerableTime() == 20, "lightning drop is not invulnerable for 20 ticks");
        check(!drop.hurt(level.damageSources().inFire(), 1.0F), "lightning drop took fire damage");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wild_cushion_interact(GameTestHelper helper) {
        Cushion cushion = cushion(helper, new BlockPos(4, 1, 4));
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        try {
            InteractionResult sit = cushion.interact(first, InteractionHand.MAIN_HAND);
            check(sit == InteractionResult.SUCCESS && first.getVehicle() == cushion, "sitting returned " + sit + " (native SUCCESS_SERVER)");
            InteractionResult taken = cushion.interact(second, InteractionHand.MAIN_HAND);
            check(taken == InteractionResult.PASS, "occupied cushion returned " + taken);
        } finally {
            first.stopRiding();
            helper.getLevel().getServer().getPlayerList().remove(first);
            helper.getLevel().getServer().getPlayerList().remove(second);
        }
        helper.succeed();
    }

    /** Native cooking fuel: cushions and wool slabs 50 ticks, poplar shelf 300 (shared fuel handler). */
    @GameTest(template = PLATFORM)
    public static void wild_fuel_values(GameTestHelper helper) {
        int cushion = new ItemStack(ModItems.WHITE_CUSHION.get()).getBurnTime(RecipeType.SMELTING);
        int slab = new ItemStack(ModBlocks.WHITE_WOOL_SLAB.get()).getBurnTime(RecipeType.SMELTING);
        int shelf = new ItemStack(ModBlocks.POPLAR_SHELF.get()).getBurnTime(RecipeType.SMELTING);
        check(cushion == 50 && slab == 50 && shelf == 300, "burn times: cushion " + cushion + ", wool slab " + slab + ", poplar shelf " + shelf);
        helper.succeed();
    }
}
