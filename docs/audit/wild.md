# wild — Wilderness Bound (native = 26.3-pre-1, `src-mc263`)

**Summary.** 39 findings (numbers 20 and 40 unused): gameplay 22, visual 7, audio 3, data 4, cosmetic 3.
**Covered:** dappled forest (biome JSON, climate placement through Platform and TerraBlender, surface rule, noise, biome tags, extra Spring-to-Life injections), poplar (all 19 blocks, items, wood/set types, tree grower, trunk and foliage placers, the 3 configured and placed trees, fallen poplar, leaf particles, ambient sound player, flammability, fuel, compost, strip), shelf mushroom (block, decorator, bounce), red shrub, straw bed (block, 2 mixins, item renderer), cushion (entity, item, renderer, model, recipes; MISC-06/36/37 re-verified and folded in as WILD-30..32), wool and concrete slabs/stairs, abandoned camps (structure set, 18 structures, every template pool, all 297 `.nbt` templates decoded and compared block by block, loot tables, map decoration), plus every WB data and asset file by script: tags resolved over a 26.2 base, loot, recipes, advancements, blockstates, models, textures pixel by pixel, `sounds.json` against `assets-26.3/sounds.json`, all 111 `.ogg` files sha1-checked against index `34.json` (all identical), and lang.
**Not checked in game:** nothing was run. WILD-01 tie behaviour and the WILD-23 net transform are worked out from code. 26.3 changed sign textures and models (`entity/signs/*.png` → `block/*_sign.png`), leaf mipmap `.mcmeta`, `on_shelf` model transforms and the noise `base_amplitude` format engine-wide; these are engine differences and are not reported per block.
**Native-only content VB lacks:** the `abandoned_camp_map` item and the 7 named secret-chest map items (WILD-12/13), 3 wandering-trader trades (WILD-26), the `sleep_in_straw_bed` stat (WILD-15), straw beds in 2 tents (WILD-14), the poplar leaf-litter tree variants (WILD-04), dappled forest in trial chambers (WILD-06) and in Adventuring Time (WILD-29), and copper gear and iron spear in camp chests (WILD-08/09).

## Coverage

| VB class / data | native counterpart (26.3-pre-1) | result |
|---|---|---|
| `common/integrations/worldgen/BiomeGeneration.DAPPLED_FOREST` + Platform `OverworldBiomeBuilderMixin` / `BiomePlacement` | `OverworldBiomeBuilder.MIDDLE_BIOMES_VARIANT[1][0]` | findings 01 |
| `compat/terrablender/OverworldRegion` | same table | findings 01 (not run) |
| `worldgen/surface/ModSurfaceRuleData` (dappled part) + `NoiseBasedChunkGeneratorMixin.merge` / `VanillaBackportTerrablender` | `material_rule/overworld/biome_surface/dappled_forest.json` inside `overworld.json → above_preliminary_surface → surface.json[2] on_floor → not_underwater → biome_surface` | findings 02 |
| `ModNoises.SMALL_PATCH` / `noise/small_patch.json` | `noise/small_patch.json` | match (`base_amplitude` is the 26.3 parity format) |
| `worldgen/biome/dappled_forest.json` / `WildernessBoundBiomes` | `worldgen/biome/dappled_forest.json` | match (spawns, colours, music, carvers, feature list). Dry foliage `#8c3a04` matches via `dry_foliage_colors/red_leaf_litter.json` 9189892 |
| biome tags with dappled forest | — | findings 05, 06; `is_forest`, `is_overworld`, `spawns_cold_variant_farm_animals`, `has_structure/abandoned_camp_dappled_forest` match |
| `WildernessBoundFeatures` / `configured_feature/{red,orange,yellow}_poplar.json` | `feature/{red,orange,yellow}_poplar.json` | match apart from `below_trunk_provider` (finding 07; grass/podzol/moss/mud stay unchanged in both, so the WILD-07 fix must not use 1.21.1 `setDirtAt`, which turns grass into dirt) |
| `configured_feature/trees_dappled_forest.json` | `feature/trees_dappled_forest.json` | findings 03, 04 |
| `configured_feature/fallen_poplar_tree.json` | `feature/fallen_poplar_tree.json` | match |
| `placed_feature/fallen_poplar_tree.json` + `WildernessBoundFeatureManager` | `placed_feature/fallen_poplar_tree.json` (selector entry only) | findings 03 |
| `configured_feature/red_shrub.json`, `placed_feature/patch_red_shrub.json`, `brown_mushroom_dappled_forest.json`, `trees_dappled_forest.json`, `{color}_poplar.json`, `bamboo_in_structure.json` | same | match |
| — | `placed_feature/{red,orange,yellow}_poplar_leaf_litter.json`, `feature/*_poplar_leaf_litter.json` | missing (finding 04) |
| `tree/trunk/PoplarTrunkPlacer` | `trunkplacers/PoplarTrunkPlacer` | findings 07 (RNG order matches) |
| `tree/foliage/PoplarFoliagePlacer` | `foliageplacers/PoplarFoliagePlacer` | match (`foliageHeightOffset` is always 0 for poplar) |
| `tree/decorators/ShelfMushroomDecorator` | `treedecorators/ShelfMushroomDecorator` | match |
| `ModTreeGrowers.POPLAR` | `TreeGrower.POPLAR` | match (1/3 each, same `nextInt(3)`) |
| `ModBlocks` poplar family, `SharedBlockProperties.POPLAR`, `logProperties`, signs | `Blocks` | match (strength, map colours, sounds, instrument, lava). `PushReaction.DESTROY` = 26.x `POPPED` |
| `POPLAR_SHELF` | `Blocks.POPLAR_SHELF` | light-gray map colour and lava ignition match only through the shared-Properties leak (COPPER-14); findings 27, 28 |
| `AtmosphericLeavesBlock` + `AmbientLeavesBlockSoundPlayer` | `UntintedParticleLeavesBlock` + `sounds/AmbientLeavesBlockSoundPlayer` | match (300 / 1 log / 3 leaves; 0.01 particles). Client RNG order of drip/particle/sound differs (client-only, not reported) |
| `FallingLeavesParticle.PoplarProvider`, `particles/*_poplar_leaves.json` | same | match |
| `ModWoodTypes` / `ModBlockSetTypes` POPLAR | `WoodType` / `BlockSetType` POPLAR | match |
| `BlockIntegrations` flammability / strip / compost / fuel | `FireBlock`, `Items` `compostable`/`cookingFuel` | findings 19, 27, 31; red shrub/leaves/sapling 0.3 and shelf mushroom 0.65 compost, all poplar fuel values via tags (300/150/200/800/100/1200) and flammability match |
| `ShelfMushroomBlock` | `ShelfMushroomBlock` + `Properties.bounceRestitution/fallDistanceReduction` | findings 18, 24 |
| `RED_SHRUB` (`ActualBushBlock`) | `BushBlock` | properties match; finding 25 |
| `StrawBedBlock`, `mixin/common/straw_bed/{LivingEntity,ServerPlayer}Mixin` | `StrawBedBlock` / `AbstractBedBlock`, `LivingEntity`, `ServerPlayer.startSleepInBed`, `STRAW_BED_RULE` | findings 15, 16, 17, 18, 19, 21, 22 |
| `StrawBedItemRenderer` + `models/item/straw_bed.json` | `items/straw_bed.json` (composite) | findings 23 |
| `Cushion`, `CushionItem`, `CushionModel`, `CushionRenderer` | same names | findings 30, 31, 32, 33; item placement and model match |
| wool / concrete slabs and stairs (`ModBlocks`, tags, recipes, loot) | `Blocks.WOOL_SLAB/STAIRS`, `CONCRETE_SLAB/STAIRS` | findings 34, 38; properties, flammability, shears speed (VB `ShearsItemMixin`), loot and models match |
| `WildernessBoundStructures`, `WildernessBoundStructureSets`, `AbandonedCampStructurePools`, all `structure`/`structure_set`/`template_pool` JSON | same | match (identical JSON) |
| `structure/abandoned_camp/**.nbt` (297) | same | findings 14 (133 identical, 162 differ only in format, the rest in blocks) |
| `loot_table/chests/abandoned_camp_*`, `barrels/abandoned_camp_barrel` | same | findings 08–12; barrel matches |
| `tags/worldgen/structure/on_abandoned_camp_*` | same | finding 39 |
| `ModMapDecorationTypes.ABANDONED_CAMP` + texture | `MapDecorationTypes` | match (MISC); texture pixel-equal |
| block loot tables (poplar family, leaves, shrub, mushroom, straw bed) | same | match (format only) |
| recipes + recipe advancements (poplar, straw bed, cushions, shelf) | same | finding 34; the rest match |
| `sounds.json` WB events, `ModSoundEvents`, `ModSoundTypes`, `.ogg` | `assets-26.3/sounds.json`, `SoundEvents`, `SoundType` | finding 36; everything else identical |
| textures (poplar, shrub, mushroom, straw bed, cushion, boats, particles, map) | same | finding 41 (one cushion texture); the rest pixel-identical |
| block / item models, blockstates | same | findings 23, 28; leaves `block/leaves` vs `cube_all` and shrub `tinted_cross` vs `cross` render the same (VB registers no tint for them) |
| lang | `en_us.json` | finding 35 |
| `TradeIntegrations` | `villager_trade/wandering_trader/emerald_poplar_*`, `emerald_shelf_mushroom` | finding 26 |
| `advancement/adventure/adventuring_time` | same | finding 29 |

---

### WILD-01  Dappled forest climate placement: wrong area, no floor point, tie with plains
- severity: gameplay
- vb: `common/integrations/worldgen/BiomeGeneration.java:84-104` `addBiome(..., DAPPLED_FOREST, Placement.MID_SLICE_VARIANT, Temperature.COOL, Humidity.ARID, Continentalness.span(NEAR_INLAND, FAR_INLAND), Erosion.span(EROSION_2, EROSION_4))` and `(..., HIGH_SLICE_VARIANT, COOL, ARID, Continentalness.span(COAST, FAR_INLAND), Erosion.span(EROSION_2, EROSION_4))`. `addBiome` (`:142-160`) emits only `Depth.SURFACE` (point 0). Platform `OverworldBiomeBuilderMixin` appends these at the TAIL of `addBiomes`, after vanilla's own PLAINS points for the same cells. 1.21.1 `Climate.RTree.SubTree.search` (`src-121 Climate.java:449-466`) only replaces the cached `lastResult` leaf when `i > j`, so two zero-distance leaves tie and the result depends on which one the thread found last.
- native: `OverworldBiomeBuilder.java:88-94` `MIDDLE_BIOMES_VARIANT = {..., {Biomes.DAPPLED_FOREST, null, null, null, Biomes.OLD_GROWTH_PINE_TAIGA}, ...}` (26.2 and 1.21.1 have `null` there, which falls back to PLAINS). `pickMiddleBiome` (`:917-924`) returns it for temperature index 1 and humidity index 0 whenever `weirdness.max() >= 0`. Every `addSurfaceBiome` writes both depth 0 and depth 1 (`:1009-1010`). So dappled forest replaces every positive-weirdness PLAINS cell for T∈[-0.45,-0.15], H∈[-1,-0.35]: low slices, mid slices including coast E5/E6, high slices including coast E0–E1 and near-inland E1/E5, peaks (coast–near E1–E3, mid E3, all E4/E6) and valleys (mid–far E0–E1).
- difference: VB covers only the mid- and high-slice E2–E4 boxes (no low slice, valley, peak, or E0/E1/E5/E6 cells). Inside them both PLAINS and DAPPLED_FOREST match at distance 0, so the biome flips between the two depending on query order, in patches that are not reproducible across runs. Below the surface (depth near 1) the vanilla PLAINS floor point always wins, so caves under a dappled forest report plains.
- fix_kind: mixin-mc (`net.minecraft.world.level.biome.OverworldBiomeBuilder`) + mixin-vb
- fix: `@Mixin(OverworldBiomeBuilder.class) abstract class OBBMixin { @Shadow @Final private ResourceKey<Biome>[][] MIDDLE_BIOMES_VARIANT; @Inject(method="<init>", at=@At("TAIL")) private void wild$dappled(CallbackInfo ci) { if (VanillaBackport.COMMON_CONFIG.hasDappledForest.get()) this.MIDDLE_BIOMES_VARIANT[1][0] = ModBiomes.DAPPLED_FOREST; } }`. Then remove VB's own points: `@Redirect(method="bootstrap", at=@At(value="INVOKE", target="Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal=1))` in `BiomeGeneration.bootstrap` → no-op (ordinal 0 is `PALE_GARDEN.forEach`, ordinal 1 is `DAPPLED_FOREST.forEach`; sulfur uses `event.add`). Do the same for the DAPPLED_FOREST `forEach` in `OverworldRegion.addBiomes` (TerraBlender builds its vanilla region from `OverworldBiomeBuilder`, so the mixin covers it too).
- config_gate: `Wilderness Bound.has_dappled_forest`

### WILD-02  Dappled forest coarse-dirt rule has no surface conditions (coarse dirt replaces whole columns)
- severity: gameplay
- vb: `common/worldgen/surface/ModSurfaceRuleData.java:24-29` `SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.isBiome(DAPPLED_FOREST), SurfaceRules.ifTrue(SurfaceRules.noiseCondition(ModNoises.SMALL_PATCH, 1.2F), coarse_dirt)), ...)`. `merge` (`:65-70`) puts it before the whole vanilla overworld rule, bedrock and deepslate included (`NoiseBasedChunkGeneratorMixin.vb$injectSurfaceRules`; TerraBlender: `addToDefaultSurfaceRulesAtStage(OVERWORLD, BEFORE_BEDROCK, 5, ...)`). 1.21.1 `SurfaceSystem.buildSurface` (`src-121 SurfaceSystem.java:124-…`) runs the rule on every default-block (stone) position of the column.
- native: `material_rule/overworld.json` → `{"condition","if_true":{"type":"minecraft:above_preliminary_surface"},"then_run":"overworld/surface"}`; `surface.json` sequence[2] `if_true minecraft:on_floor` → `if_true minecraft:not_underwater` (`water offset -1, multiplier 0`) → `overworld/biome_surface` → `{"biome_is":"minecraft:dappled_forest"} → biome_surface/dappled_forest.json` = `noise_threshold small_patch min 1.2000000476837158 → coarse_dirt`. Otherwise it falls through to `biome_surface/default` (grass); below the floor block, `under_biome_surface/default` gives dirt.
- difference: Natively only the top floor block of a patch (small_patch ≥ 1.2, not under water) becomes coarse dirt, with dirt under it. In VB every stone, deepslate and future dirt block in that column becomes coarse dirt, all the way down while the 3D biome lookup still returns dappled forest (the upper part of the depth range, plus tie cells, see WILD-01). Ores (which need `stone_ore_replaceables`) cannot generate in those columns, and underwater floors also get coarse dirt.
- fix_kind: mixin-vb (`ModSurfaceRuleData.makeRules`)
- fix: `@ModifyArg(method="makeRules", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/levelgen/SurfaceRules;ifTrue(Lnet/minecraft/world/level/levelgen/SurfaceRules$ConditionSource;Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;)Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;", ordinal=0), index=1)` (ordinal 0 is the inner noise `ifTrue`) returning `SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), original)))`. All conditions are pure, so nesting them inside the noise check is the same as native's order.
- config_gate: `Wilderness Bound.has_dappled_forest` (the rule only matters where the biome exists)

### WILD-03  Fallen poplars: weight-120 selector entry natively, 1-in-80 extra feature in VB
- severity: gameplay
- vb: `configured_feature/trees_dappled_forest.json` `{"type":"minecraft:weighted_random_selector","config":{"features":[{"data":"minecraft:red_poplar","weight":200},{"data":"minecraft:orange_poplar","weight":240},{"data":"minecraft:yellow_poplar","weight":90},{"data":"minecraft:spruce_checked","weight":27}]}}`; `WildernessBoundFeatureManager.bootstrap` adds `placed_feature/fallen_poplar_tree.json` (`rarity_filter 80, in_square, surface_water_depth_filter 0, heightmap OCEAN_FLOOR, biome, would_survive poplar_sapling`) only if `has_fallen_trees`.
- native: `feature/trees_dappled_forest.json` `{"type":"minecraft:weighted_random_selector","features":[{"data":"minecraft:red_poplar_leaf_litter","weight":200},{"data":"minecraft:orange_poplar_leaf_litter","weight":240},{"data":"minecraft:yellow_poplar_leaf_litter","weight":90},{"data":"minecraft:spruce_checked","weight":27},{"data":"minecraft:fallen_poplar_tree","weight":120}]}`; `placed_feature/fallen_poplar_tree.json` = only `block_predicate_filter would_survive poplar_sapling`. The placed `trees_dappled_forest` (count 6) is the same in both.
- difference: Natively 120/677 of the 6 attempts per chunk are fallen poplars (≈1.06 per chunk) and standing trees are ≈4.94 per chunk. VB has 6 standing trees per chunk and ≈0.0125 fallen poplars per chunk, about 85× fewer.
- fix_kind: data-override + mixin-vb
- fix: `data/minecraft/worldgen/placed_feature/fallen_poplar_tree.json` = `{"feature":"minecraft:fallen_poplar_tree","placement":[{"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:would_survive","state":{"Name":"minecraft:poplar_sapling","Properties":{"stage":"0"}}}}]}`. Add `{"data":"minecraft:fallen_poplar_tree","weight":120}` as the 5th selector entry (full JSON in WILD-04). `@Inject(method="bootstrap", at=@At("HEAD"), cancellable=true)` on `WildernessBoundFeatureManager` to drop the standalone injection.
- config_gate: VB gates fallen poplars on `Spring to Life.has_fallen_trees`; native has no gate. To keep the toggle, swap the selector file for one without the entry (via a NeoForge `neoforge:conditions` on a mod-config condition) when it is off.

### WILD-04  Poplar trees in the dappled forest lack their leaf-litter decorators (VB uses the chunk-scan leaf-litter feature)
- severity: visual
- vb: dappled forest is in `tags/worldgen/biome/spawns_leaf_litter.json` (`["minecraft:forest","minecraft:dark_forest","minecraft:wooded_badlands","minecraft:dappled_forest"]`), so `SpringToLifeFeatureManager` adds `leaf_litter_thick` + `leaf_litter_sparse` (`LeafLitterFeature`, see FLORA-08), and the poplar leaves are in `allows_leaf_litter`. The selector uses the plain `red/orange/yellow_poplar` trees.
- native: `feature/red_poplar_leaf_litter.json` (orange/yellow the same) = the `red_poplar` tree plus `"decorators":[{"type":"minecraft:place_on_ground","tries":96,"radius":4,"height":2,"block_state_provider":<leaf_litter N/E/S/W × segment_amount 1..3, weight 1 each>},{"type":"minecraft:place_on_ground","tries":150,"height":2,"block_state_provider":<… segment_amount 1..4>},{"type":"minecraft:shelf_mushroom","probability":0.4}]`; `placed_feature/*_poplar_leaf_litter.json` = `would_survive poplar_sapling`; the dappled biome has no leaf-litter patch feature.
- difference: Native litter is placed per tree (96 tries r4 and 150 tries r2, both before the shelf mushrooms, which also sets the RNG order of the shelf-mushroom decorator). VB's litter comes from a whole-chunk scan with different placement rules (FLORA-08). If FLORA-08 is applied, dappled forest loses its litter completely unless these trees are added.
- fix_kind: data-override
- fix: add `data/minecraft/worldgen/configured_feature/{red,orange,yellow}_poplar_leaf_litter.json` = VB's `{color}_poplar.json` with `config.decorators` = `[{"type":"minecraft:place_on_ground","tries":96,"radius":4,"height":2,"block_state_provider":{"type":"minecraft:weighted_state_provider","entries":[{"data":{"Name":"minecraft:leaf_litter","Properties":{"facing":"north","segment_amount":"1"}},"weight":1}, … facing north/east/south/west × segment_amount 1,2,3 …]}},{"type":"minecraft:place_on_ground","tries":150,"radius":2,"height":2,"block_state_provider":{… segment_amount 1,2,3,4 …}},{"type":"minecraft:shelf_mushroom","probability":0.4}]`. Add `placed_feature/{color}_poplar_leaf_litter.json` = `{"feature":"minecraft:{color}_poplar_leaf_litter","placement":[{"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:would_survive","state":{"Name":"minecraft:poplar_sapling","Properties":{"stage":"0"}}}}]}`. Set `configured_feature/trees_dappled_forest.json` = `{"type":"minecraft:weighted_random_selector","config":{"features":[{"data":"minecraft:red_poplar_leaf_litter","weight":200},{"data":"minecraft:orange_poplar_leaf_litter","weight":240},{"data":"minecraft:yellow_poplar_leaf_litter","weight":90},{"data":"minecraft:spruce_checked","weight":27},{"data":"minecraft:fallen_poplar_tree","weight":120}]}}`. Remove dappled forest from `spawns_leaf_litter` (`replace:true` with the other three entries), coordinated with FLORA-08.
- config_gate: `Spring to Life.has_leaf_litter` (VB's litter toggle; native has none)

### WILD-05  VB adds firefly bushes and rare fallen birches to the dappled forest
- severity: gameplay
- vb: `SpringToLifeFeatureManager.java` adds `PATCH_FIREFLY_BUSH_NEAR_WATER` to `#spawns_firefly_bushes` and `PLACED_RARE_FALLEN_BIRCH_TREE` to `ctx.is(#spawns_fallen_birch_trees_rarely) && !ctx.is(PALE_GARDEN)`. Both tags contain `#minecraft:is_forest`, which contains `dappled_forest` (`tags/worldgen/biome/is_forest.json`).
- native: `worldgen/biome/dappled_forest.json` vegetal step = `glow_lichen, trees_dappled_forest, brown_mushroom_dappled_forest, patch_red_shrub, patch_grass_forest` (no firefly bush, no fallen birch).
- difference: VB dappled forests grow firefly bushes near water and occasional fallen birch logs; native has neither.
- fix_kind: mixin-vb (`SpringToLifeFeatureManager.bootstrap`)
- fix: wrap both predicates to also require `!ctx.is(ModBiomes.DAPPLED_FOREST)`. For example, `@ModifyArg` on the `getOrCreateBiomeBuilder(TagKey)` call for `SPAWNS_FIREFLY_BUSHES` → use the predicate overload `ctx -> ctx.is(SPAWNS_FIREFLY_BUSHES) && !ctx.is(ModBiomes.DAPPLED_FOREST)`, and append `&& !ctx.is(ModBiomes.DAPPLED_FOREST)` to the existing fallen-birch lambda (`@WrapOperation` on its `ctx.is(PALE_GARDEN)` call returning `original || ctx.is(DAPPLED_FOREST)`).
- config_gate: `has_firefly_bushes`, `has_fallen_trees`

### WILD-06  Trial chambers cannot generate in dappled forests
- severity: gameplay
- vb: `tags/worldgen/biome/has_structure/trial_chambers.json` `{"values":["minecraft:pale_garden","minecraft:sulfur_caves"]}` (appends to the 1.21.1 list, which does not include the new biome).
- native: `tags/worldgen/biome/has_structure/trial_chambers.json` includes `minecraft:dappled_forest`.
- difference: No trial chambers under dappled forests in VB.
- fix_kind: data-override (`data/minecraft/tags/worldgen/biome/has_structure/trial_chambers.json`)
- fix: `{"replace":false,"values":["minecraft:dappled_forest"]}`.
- config_gate: none

### WILD-07  Poplar trunk never replaces the block under it (native: becomes dirt unless it is dirt-family, mud, moss or podzol)
- severity: gameplay
- vb: `tree/trunk/PoplarTrunkPlacer.java:46-49` `placeTrunk(...) { int trunkHeightUpToFoliageBranches = freeTreeHeight - ...; for (y...) placeLog(...)` (no `setDirtAt` call); the JSON `dirt_provider dirt, force_dirt false` is therefore never used.
- native: `trunkplacers/PoplarTrunkPlacer.java:56` `placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), tree);` with `below_trunk_provider: "minecraft:soil_beneath_tree"` = `rule_based: if not #cannot_replace_below_tree_trunk (#dirt, #mud, #moss_blocks, podzol) → dirt` (no RNG used).
- difference: A poplar grown from a sapling on farmland (saplings may be planted there) leaves the farmland under its trunk in VB; natively it becomes dirt. Grass, podzol and the other dirt-family blocks stay unchanged in both.
- fix_kind: mixin-vb (`PoplarTrunkPlacer.placeTrunk`)
- fix: `@Inject(method="placeTrunk", at=@At("HEAD"))`: `BlockPos below = pos.below(); if (!level.isStateAtPosition(below, s -> s.is(BlockTags.DIRT) || s.is(Blocks.MUD) || s.is(Blocks.MUDDY_MANGROVE_ROOTS) || s.is(Blocks.MOSS_BLOCK) || s.is(ModBlocks.PALE_MOSS_BLOCK.get()) || s.is(Blocks.PODZOL))) blockSetter.accept(below, Blocks.DIRT.defaultBlockState());`
- config_gate: none

### WILD-08  Camp common chest lacks the six copper gear entries
- severity: gameplay
- vb: `loot_table/chests/abandoned_camp_common_chest.json` pool 1 (`rolls 2`): `bow, bucket, spyglass, shears` (weight 1, count 1 each).
- native: same pool: `bow, bucket, copper_axe, copper_boots, copper_chestplate, copper_leggings, copper_spear, copper_sword, spyglass, shears` (weight 1, count 1 each).
- difference: Each of VB's 4 items is 1/4 per roll instead of 1/10, and copper gear never appears.
- fix_kind: data-override (`data/minecraft/loot_table/chests/abandoned_camp_common_chest.json`)
- fix: replace pool 1 `entries` with the 10 native entries in 1.21.1 syntax (`{"type":"minecraft:item","name":"minecraft:copper_axe","functions":[{"function":"minecraft:set_count","count":1}]}` …), in native order. Keep pools 0 and 2 (and WILD-10/11).
- config_gate: `Copper Age` copper tool set (`has_copper_tool_set`), `has_spears` for `copper_spear`; use `neoforge:conditions` per entry or keep them always since the items exist.

### WILD-09  Camp secret chest lacks the iron spear
- severity: gameplay
- vb: `loot_table/chests/abandoned_camp_secret_chest.json` pool 2 (`rolls uniform 0..1`): `iron_axe, iron_boots, iron_leggings`.
- native: same pool plus `minecraft:iron_spear` (weight 1, count 1).
- difference: Each of VB's items is 1/3 instead of 1/4, and the iron spear never appears.
- fix_kind: data-override (`data/minecraft/loot_table/chests/abandoned_camp_secret_chest.json`)
- fix: append `{"type":"minecraft:item","name":"minecraft:iron_spear","functions":[{"function":"minecraft:set_count","count":1}]}` to pool 2.
- config_gate: `has_spears`

### WILD-10  Camp maps are zoom 1 (native zoom 2)
- severity: gameplay
- vb: both camp chests: every `exploration_map` function has `"zoom": 1` (8 camp maps in common pool 2, 7 maps in secret pool 3).
- native: same functions with no `zoom` → `ExplorationMapFunction.java:39` default `(byte)2`.
- difference: VB treasure maps cover half the width (zoom 1 = 256 blocks) of native maps (zoom 2 = 512 blocks).
- fix_kind: data-override (both chest loot tables)
- fix: remove `"zoom": 1` (or set `"zoom": 2`) on all 15 `minecraft:exploration_map` functions.
- config_gate: none

### WILD-11  Unresolved exploration maps drop as blank maps instead of being discarded
- severity: gameplay
- vb: maps are `"name":"minecraft:map"` + `exploration_map` only. 1.21.1 `ExplorationMapFunction.run` returns the unchanged stack when no structure is found, so a blank `minecraft:map` stays in the chest.
- native: each map entry ends with `{"type":"minecraft:filtered","item_filter":{"predicates":{"minecraft:map_id":{}}},"on_fail":{"type":"minecraft:discard"}}`.
- difference: A camp chest whose map target can't be found (e.g. no pale garden nearby, or the structure is disabled) holds an extra empty map in VB and nothing natively.
- fix_kind: data-override (both chest loot tables)
- fix: append to each of the 15 map entries' `functions`: `{"function":"minecraft:filtered","item_filter":{"items":"minecraft:map"},"modifier":{"function":"minecraft:set_count","count":0}}` (an un-resolved map is still `minecraft:map`; a resolved one is `minecraft:filled_map`).
- config_gate: none

### WILD-12  Secret-chest exploration maps are unnamed "Map"s
- severity: visual
- vb: secret pool 3 entries `{"name":"minecraft:map","functions":[{"function":"minecraft:exploration_map","decoration":"minecraft:ancient_city","destination":"minecraft:on_ancient_city_maps","zoom":1}]}` (and trial_chambers, mineshaft, desert_pyramid, jungle_temple, ocean_ruin_warm, mansion), with no `set_name`.
- native: the items are `buried_ancient_city_map`, `buried_trial_chambers_map`, `buried_mineshaft_map`, `desert_pyramid_map`, `jungle_pyramid_map`, `warm_ocean_ruins_map` and `woodland_mansion_map`. Each has its own name (`en_us`: "Buried Ancient City Map", "Buried Trial Chambers Map", "Buried Mineshaft Map", "Desert Pyramid Map", "Jungle Pyramid Map", "Warm Ocean Ruins Map", "Woodland Mansion Map") and its own item texture (`textures/item/*_map.png`).
- difference: VB maps show as plain "Map" with the generic filled-map icon.
- fix_kind: data-override + asset-override (engine for the separate item ids)
- fix: add `{"function":"minecraft:set_name","target":"item_name","name":{"translate":"item.minecraft.buried_ancient_city_map"}}` etc. to each entry; ship those 7 lang keys and the 26.3 strings in `assets/minecraft/lang/en_us.json`. For the icons, add `{"function":"minecraft:set_custom_model_data","value":<n>}` per entry and `assets/minecraft/models/item/filled_map.json` overrides `{"predicate":{"custom_model_data":n},"model":"minecraft:item/<name>"}` with the native textures copied (keep the `layer1` map-colour overlay of 1.21.1 `filled_map`).
- config_gate: none

### WILD-13  No `abandoned_camp_map` item: camp maps use the generic filled-map icon
- severity: visual
- vb: common pool 2 `"name":"minecraft:map"` + `set_name filled_map.<biome>_camp_map`.
- native: `"name":"minecraft:abandoned_camp_map"` (`Items.java:2082`, `MapItem`, `items/abandoned_camp_map.json`, `textures/item/abandoned_camp_map.png`, in `#clonable_maps`/`#extendable_maps`).
- difference: Same name and decoration, but VB lacks the dedicated item and its texture. The map still clones because VB uses `filled_map`.
- fix_kind: asset-override (engine for the item id)
- fix: as in WILD-12, `set_custom_model_data` on the 8 camp-map entries plus a `filled_map.json` override pointing at a model using `textures/item/abandoned_camp_map.png` (copied from 26.3).
- config_gate: none

### WILD-14  Abandoned-camp templates are from an older build (missing straw beds, wrong ground, swamp/jungle/birch/mushroom details)
- severity: gameplay
- vb: `data/minecraft/structure/abandoned_camp/**.nbt` decoded and compared with native block by block. Format-only differences were dropped (palette `Name/Properties` vs `id/properties`, jigsaw `name/target` `minecraft:bottom` vs `minecraft:down`, empty `Items`, cushion `TileX/Y/Z` vs `block_pos`). Remaining block differences (VB → native):
  - `tent/birch_forest/tent_birch_forest_6`, `tent/forest/tent_forest_10`: air → `straw_bed[facing=west,part=head]` + `[part=foot]` (2 beds missing in VB); 1 `dirt_path` → `dirt` each.
  - `tent/forest/tent_forest_7` (25), `_8` (18), `_10` (16): `dirt` → `grass_block[snowy=false]`; `_7` also air → `lily_of_the_valley`.
  - `coarse_dirt` → `dirt` (1–4 blocks each) in tents `bamboo_jungle_9`, `birch_forest_9`, `cherry_grove_6/8/9`, `dappled_forest_9`, `flower_forest_2/4/5/9`, `forest_2/9`, `meadow_9`, `old_growth_pine_taiga_2/3/5/7/8/9`, `old_growth_spruce_taiga_9`, `pale_garden_9`, `savanna_9`, `snowy_taiga_9`, `sparse_jungle_9`, `swamp_9`, `taiga_9`, `windswept_forest_9`, `wooded_badlands_8/9/10` and camps `default/campsite_default_chest_3`, `default/campsite_default_special_2`. `tent_bamboo_jungle_7` also coarse_dirt → grass_block and short_grass → air.
  - `tent/flower_forest/tent_flower_forest_4`: `bee_nest[facing=east]` → air.
  - `tent/old_growth_spruce_taiga/tent_old_growth_spruce_taiga_3`: air → `red_mushroom`.
  - `tent/sparse_jungle/tent_sparse_jungle_2`: tree jigsaw `pool` `minecraft:abandoned_camp/trees/bamboo` → `minecraft:abandoned_camp/trees/jungle`.
  - `camp/old_growth_birch_forest/campsite_old_growth_birch_forest_4`: one jigsaw `orientation=north_up` → `up_west`.
  - `camp/swamp/campsite_swamp_1`: 21 vines, `lily_pad`, `firefly_bush` → air; `grass_block` → `clay`, air → `clay`, `seagrass` → `water[level=0]`. `campsite_swamp_2`: `brown_mushroom` + `mycelium` → air.
  - `camp/cherry_grove/campsite_cherry_grove_3`: vegetation layout differs (11 grass/tall-grass/poppy cells, and a jigsaw → `grass_block`).
  - `tent/swamp/tent_swamp_6`: mushroom-block face states differ (10 cells), `white_wool_stairs`, `red_mushroom`, `brown_mushroom` → air.
  - `camp/taiga/campsite_taiga_4`: `water[level=8]` → `water[level=0]` (2).
  - vine `up=` flags differ in `tent_sparse_jungle_5/6/7/8/10` and `tent_swamp_3/5/7/10` (plus 6 vines → air in `tent_swamp_3`).
- native: `src-mc263/data/minecraft/structure/abandoned_camp/**` (same 297 paths; template pools and structures identical in both).
- difference: Some tents lack their straw beds, have dirt instead of grass/dirt, spawn the wrong tree pool (bamboo instead of jungle) or have a rotated tree jigsaw, and the swamp camp has extra vegetation.
- fix_kind: data-override (`data/minecraft/structure/abandoned_camp/...`)
- fix: re-ship the listed templates converted from 26.3: rename palette keys `id`→`Name`, `properties`→`Properties`; jigsaw `name`/`target` `minecraft:down`→`minecraft:bottom` (VB's 1.21.1 feature-element jigsaw name); cushion entity `block_pos`→`TileX/TileY/TileZ`; set `DataVersion` 3955. All block ids used by the native templates already exist in VB (checked: none missing).
- config_gate: none

### WILD-15  Straw bed: wrong stat (awarded twice) and it triggers "Sweet Dreams"
- severity: gameplay
- vb: `StrawBedBlock.java:105-109` `player.startSleepInBed(pos).ifLeft(...).ifRight(unit -> player.awardStat(Stats.SLEEP_IN_BED));`. 1.21.1 `ServerPlayer.startSleepInBed` (`src-121 ServerPlayer.java:1028-1031`) already does `awardStat(Stats.SLEEP_IN_BED); CriteriaTriggers.SLEPT_IN_BED.trigger(this);`.
- native: `ServerPlayer.java:1298-1303` `.ifRight(unit -> { this.awardStat(bedBlock.getSleptInBedStatType()); if (canSetSpawn) CriteriaTriggers.SLEPT_IN_BED.trigger(this); })`; `StrawBedBlock.getSleptInBedStatType()` = `Stats.SLEEP_IN_STRAW_BED` ("Times Slept in a Straw Bed"); `STRAW_BED_RULE` overworld = `BedRule.DESTROY_ON_LEAVE` (`can_set_spawn NEVER`).
- difference: Sleeping in a straw bed adds 2 to "Times Slept in a Bed" in VB and grants `adventure/sleep_in_bed`. Natively it adds 1 to a separate `sleep_in_straw_bed` stat and grants no advancement.
- fix_kind: mixin-vb + mixin-mc (`ServerPlayer.startSleepInBed`), engine for the new stat
- fix: remove VB's `ifRight` (`@Redirect` the `Either.ifRight` call in `StrawBedBlock.useWithoutItem` → return the receiver). Register a custom stat `minecraft:sleep_in_straw_bed` (`Stats.makeCustomStat`, lang `stat.minecraft.sleep_in_straw_bed`). In `ServerPlayer.startSleepInBed`, `@WrapOperation` the inner `ifRight` lambda's `awardStat(Stats.SLEEP_IN_BED)` and `CriteriaTriggers.SLEPT_IN_BED.trigger` so that when `level().getBlockState(at).getBlock() instanceof StrawBedBlock` it awards `sleep_in_straw_bed` and skips the trigger.
- config_gate: none

### WILD-16  Straw bed sleeping height is 11/16 (native 6/16)
- severity: visual
- vb: no straw-bed handling of the sleep position; 1.21.1 `LivingEntity.setPosToBed(BlockPos)` → `setPos(x+0.5, y+0.6875, z+0.5)`.
- native: `LivingEntity.java:3843-3845` `setPos(bed.x+0.5, bed.y + sleepHeight + 0.125, bed.z+0.5)`; `StrawBedBlock.getSleepHeight` uses the foot part's shape max Y = 4/16 (`AbstractBedBlock.getSleepHeight`), giving +0.375.
- difference: Players sleeping on a straw bed float 0.3125 block (5 px) higher in VB.
- fix_kind: mixin-mc (`net.minecraft.world.entity.LivingEntity.setPosToBed(BlockPos)`)
- fix: `@Inject(method="setPosToBed", at=@At("HEAD"), cancellable=true)`: `if (level().getBlockState(pos).getBlock() instanceof StrawBedBlock) { setPos(pos.getX()+0.5, pos.getY()+0.375, pos.getZ()+0.5); ci.cancel(); }`. Here 0.375 = 4/16 + 0.125, and the head part also uses the foot shape, so it is 0.375 for both.
- config_gate: none

### WILD-17  Straw beds are not washed away by water
- severity: gameplay
- vb: 1.21.1 `FlowingFluid.canHoldFluid` → `!state.blocksMotion()`. Straw bed collision 16×4×16 has an average dimension of 0.75 ≥ 0.729, so it is legacy-solid and `blocksMotion` is true.
- native: `FlowingFluid.java:408` `block instanceof LiquidBlockContainer ? true : state.is(BlockTags.WASHED_AWAY_BY_FLUIDS)`; `washed_away_by_fluids.json` contains `minecraft:straw_bed` (and `red_shrub`, `shelf_mushroom`, which VB already washes away as non-solid).
- difference: Flowing water/lava breaks (and drops) a straw bed natively; in VB it acts as a wall.
- fix_kind: mixin-mc (`net.minecraft.world.level.material.FlowingFluid.canHoldFluid`)
- fix: `@Inject(method="canHoldFluid", at=@At("HEAD"), cancellable=true)` → `if (state.getBlock() instanceof StrawBedBlock) cir.setReturnValue(true);`
- config_gate: none

### WILD-18  Straw bed and shelf mushroom count as pathfindable
- severity: gameplay
- vb: `StrawBedBlock` and `ShelfMushroomBlock` do not override `isPathfindable`; 1.21.1 `BlockBehaviour.isPathfindable` LAND → `!state.isCollisionShapeFullBlock(...)` = true.
- native: `AbstractBedBlock.isPathfindable` → `false`; `ShelfMushroomBlock.isPathfindable` → `false`.
- difference: Mob pathfinding treats them as passable cells in VB (as with vanilla beds, which return false).
- fix_kind: mixin-vb (`StrawBedBlock`, `ShelfMushroomBlock`)
- fix: add `@Override protected boolean isPathfindable(BlockState s, PathComputationType t) { return false; }` via `@Mixin` with a new method (or `@Inject` into `BlockBehaviour.isPathfindable` HEAD checking `instanceof`).
- config_gate: none

### WILD-19  Straw bed is missing the 0.85 compost value
- severity: gameplay
- vb: `BlockIntegrations.registerCompostables` (`:108-128`) has no `STRAW_BED` entry.
- native: `Items.java:2058-2059` `STRAW_BED ... new Item.Properties().stacksTo(16).compostable(ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH)`; `compostable/medium_high.json` 85/15.
- difference: Straw beds can't be composted in VB; natively 85 % per item.
- fix_kind: data-override (`data/neoforge/data_maps/item/compostables.json`)
- fix: `{"values":{"minecraft:straw_bed":{"chance":0.85}}}`.
- config_gate: none

### WILD-21  Straw bed broken by `destroyBlock` plays the break sound (native: particles only)
- severity: audio
- vb: `StrawBedBlock` has no `spawnDestroyParticles`; 1.21.1 `Level.destroyBlock` → `levelEvent(2001, …)` (break sound + particles), used for piston pops and the native water case (WILD-17).
- native: `StrawBedBlock.spawnDestroyParticles` → `level.levelEvent(2014, pos, getId(state))`; `LevelEventHandler.java:379-381` `case 2014: addDestroyBlockEffect(...)` (no sound). `Level.destroyBlock` (`:279`) calls `spawnDestroyParticles`.
- difference: When a straw bed is destroyed by a non-player cause (piston, water), VB plays `block.straw_bed.break` at volume (1+1)/2, pitch 0.8; native is silent and shows only the particles.
- fix_kind: mixin-mc (`Level.destroyBlock`)
- fix: `@WrapOperation` the `Level.levelEvent(int, BlockPos, int)` call (event 2001) in `Level.destroyBlock(BlockPos, boolean, Entity, int)`: if the state is a `StrawBedBlock`, call `levelEvent(WILD_STRAW_BED_PARTICLES, pos, id)` instead. 1.21.1 has no event 2014, so register a client mixin on `LevelRenderer.levelEvent` that handles the new id with `this.level.addDestroyBlockEffect(pos, Block.stateById(data))` and no sound.
- config_gate: none

### WILD-22  Straw bed stand-up position is computed before the bed is removed
- severity: gameplay
- vb: `mixin/common/straw_bed/LivingEntityMixin.vb$onStopSleeping`: `setBlock(OCCUPIED false)` → `findStandUpPosition(...)` → `setPos` → `strawBed.onStopSleeping(level, pos)` (removes the bed).
- native: `LivingEntity.java:3860-3865` `setBlockAndUpdate(OCCUPIED false); bedBlock.onStopSleeping(level, bedPosition); Vec3 standUp = AbstractBedBlock.findStandUpPosition(...)`.
- difference: Natively the dismount search runs with the bed already gone, so the `{0,0}` / `{-forward}` "above" offsets find the floor where the bed was. VB still sees the 4 px bed, so the player stands on top of it (y+0.25) before it disappears and then drops, or picks a side position.
- fix_kind: mixin-vb (`LivingEntityMixin.vb$onStopSleeping`)
- fix: move `strawBed.onStopSleeping(entity.level(), pos)` to directly after the `OCCUPIED=false` `setBlock` and before `findStandUpPosition` (`@Redirect` the `findStandUpPosition` call to first call `onStopSleeping`, then `@Redirect` the later `onStopSleeping` call to a no-op).
- config_gate: none

### WILD-23  Straw bed item display transforms and layout differ from the native composite model
- severity: visual
- vb: `models/item/straw_bed.json` display `firstperson/thirdperson_righthand rotation [30,160,0]`, `gui translation [2,5,0]`, `ground rotation [0,0,0]`, `head rotation [0,180,0] translation [0,10,-8]`, `fixed rotation [270,0,0] translation [0,6,-3]`. `StrawBedItemRenderer`: SOUTH-facing head at `translate(0,0,-0.3)`, foot at `(0,0,-1.3)`.
- native: `items/straw_bed.json` composite: `block/straw_bed_head` rotated 180° Y + `translation [1,0,1]`, `block/straw_bed_foot` rotated 180° Y + `[1,0,2]`. The display comes from the block models: `firstperson/thirdperson rotation [30,340,0]`, `gui translation [2,3,0]`, `ground rotation [0,180,0]`, `head translation [0,10,-8]` (no rotation), `fixed rotation [270,180,0] translation [0,4,-3]`, plus `on_shelf`.
- difference: VB's 180° display offsets make up for its unrotated bed. The GUI and fixed (item frame) translations are 2 px higher (5 vs 3, 6 vs 4), and the pair is shifted 0.3 block along its length (centre at z −0.3 vs native's equivalent 0.5 after the flip). The exact net offset was not confirmed in game.
- fix_kind: asset-override + mixin-vb (`StrawBedItemRenderer.renderFirstPerson`)
- fix: copy the native block-model `display` block into `models/item/straw_bed.json` except for the rotations, keep VB's +180° Y, and set `gui.translation [2,3,0]`, `fixed.translation [0,4,-3]`. In the renderer use `translate(0,0,0)` for the head and `(0,0,-1)` for the foot so the pair spans z −1..1 like the native composite after its 180° flip. Check by screenshot against 26.3 in GUI, hand and frame.
- config_gate: none

### WILD-24  Shelf mushroom bounce: sound gate and position, no BOUNCE game event
- severity: audio
- vb: `ShelfMushroomBlock.java:101-108` `if (restitution.y < 0) { setDeltaMovement(x, -y*0.75*(living?1.0:0.8), z); if (!client && restitution.y < -0.1 && !(ItemEntity) && !(PrimedTnt)) playSound(null, entity.blockPosition(), SHELF_MUSHROOM_BOUNCE, BLOCKS); }`
- native: `Entity.java:857-883` bounce whenever `-movement.y > effectiveGravity` and not suppressed (`isSuppressingBounce`, `#suppresses_bounce`), restitution `max(…, 0.75 × (living ? 1 : 0.8))` with gravity and drag compensation. On bounce: `effectState.getBlock().bounceOn(level, state, getBlockPosBelowThatAffectsMyMovement(), this, fallDistance)` → `ShelfMushroomBlock.bounceOn` plays `SHELF_MUSHROOM_BOUNCE` (1.0/1.0) at the mushroom's position for any non-item, non-TNT entity, then `gameEvent(GameEvent.BOUNCE)`.
- difference: VB plays the sound only when falling faster than 0.1 b/t and at the entity's block position (one block above the mushroom), and emits no game event. Natively it plays at the mushroom for every bounce above gravity speed (≈0.08) and sculk sensors hear the bounce. The gravity/drag compensation is a 26.x engine change shared with slime blocks (engine).
- fix_kind: mixin-vb (`ShelfMushroomBlock.bounceUp`)
- fix: `@Overwrite`-style replace: `if (v.y < 0) { entity.setDeltaMovement(v.x, -v.y*0.75*(entity instanceof LivingEntity ? 1.0 : 0.8), v.z); if (!entity.level().isClientSide && -v.y > entity.getGravity() && !(entity instanceof ItemEntity) && !(entity instanceof PrimedTnt)) { entity.level().playSound(null, entity.getBlockPosBelowThatAffectsMyMovement(), SHELF_MUSHROOM_BOUNCE.get(), SoundSource.BLOCKS, 1.0F, 1.0F); } entity.gameEvent(ModGameEvents.BOUNCE or GameEvent.HIT_GROUND); }`. 1.21.1 has no `BOUNCE` game event; use `HIT_GROUND`, or skip it (engine).
- config_gate: none

### WILD-25  Red shrub tag membership
- severity: gameplay
- vb: `red_shrub` is in `tags/block/replaceable_by_trees.json` but not in `replaceable`, `replaceable_by_mushrooms` or `enchantment_power_transmitter`. The block has `.replaceable()`, but the tags are separate.
- native: `red_shrub` is in `#replaceable`, `#replaceable_by_mushrooms` and `#enchantment_power_transmitter`, and not in `#replaceable_by_trees` (tags resolved).
- difference: In VB, trees and saplings overwrite red shrubs (natively a shrub blocks the trunk/leaf spot); huge mushrooms can't overwrite them; a shrub between a bookshelf and an enchanting table blocks the power (natively it passes); features that use `#replaceable` don't treat it as replaceable.
- fix_kind: data-override
- fix: `tags/block/replaceable.json`, `replaceable_by_mushrooms.json`, `enchantment_power_transmitter.json` each `{"replace":false,"values":["minecraft:red_shrub"]}`. Remove it from `replaceable_by_trees` by shipping VB's `replaceable_by_trees.json` with `"replace":true`, the 1.21.1 vanilla list, and VB's other entries minus `red_shrub`.
- config_gate: none

### WILD-26  Wandering trader lacks the three Wilderness Bound trades
- severity: gameplay
- vb: `TradeIntegrations.java` registers pale-garden and other drops' trades; no poplar or shelf mushroom.
- native: `villager_trade/wandering_trader/emerald_poplar_log.json` `{wants emerald ×1, gives poplar_log ×8, reputation_discount 0.05}` (max_uses default 4, xp 1) in `#wandering_trader/uncommon`; `emerald_poplar_sapling.json` `{wants emerald ×5, gives poplar_sapling ×1, max_uses 8, 0.05}` and `emerald_shelf_mushroom.json` `{wants emerald ×1, gives shelf_mushroom ×3, max_uses 12, 0.05}` in `#wandering_trader/common`.
- difference: The trader never sells poplar logs, saplings or shelf mushrooms in VB.
- fix_kind: mixin-vb (`TradeIntegrations`) or a NeoForge `WandererTradesEvent` listener
- fix: `event.registerWandererTrade(true, new ItemsForEmeralds(ModBlocks.POPLAR_LOG.get(), 1, 8, 4, 1))`, `event.registerWandererTrade(false, new ItemsForEmeralds(ModBlocks.POPLAR_SAPLING.get(), 5, 1, 8, 1), new ItemsForEmeralds(ModBlocks.SHELF_MUSHROOM.get(), 1, 3, 12, 1))` (matching VB's pale-oak pattern: `true` = rare/uncommon pool; price multiplier 0.05 is VB's `ItemsForEmeralds` default).
- config_gate: none (VB has no Wilderness Bound trade toggle; a new one would match `pale_trades`)

### WILD-27  Poplar shelf is not furnace fuel (native 300 ticks)
- severity: gameplay
- vb: no fuel entry for any shelf; COPPER-15's fix lists the ten vanilla-wood shelves but not `poplar_shelf`.
- native: `Items.java:728-731` `POPLAR_SHELF ... .cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_BLOCKS)` = 300.
- difference: Poplar shelves don't burn in VB; native 300 ticks.
- fix_kind: data-override (`data/neoforge/data_maps/item/furnace_fuels.json`)
- fix: add `"minecraft:poplar_shelf": {"burn_time": 300}` to the COPPER-15 file.
- config_gate: none

### WILD-28  Poplar shelf break particles use the stripped pale oak texture
- severity: visual
- vb: `models/block/poplar_shelf.json`, `_center`, `_inventory`, `_left`, `_right`, `_unconnected`, `_unpowered`: `"particle": "minecraft:block/stripped_pale_oak_log"`.
- native: same 7 models `"particle": "minecraft:block/stripped_poplar_log"`.
- difference: Breaking or walking on a poplar shelf shows pale-oak particles.
- fix_kind: asset-override (`assets/minecraft/models/block/poplar_shelf*.json`)
- fix: set `textures.particle` to `minecraft:block/stripped_poplar_log` in all 7 files (parent and `all` unchanged).
- config_gate: none

### WILD-29  Adventuring Time does not include the dappled forest
- severity: data
- vb: no `advancement/adventure/adventuring_time.json` override.
- native: `advancement/adventure/adventuring_time.json:88-96` `"minecraft:dappled_forest": {"trigger":"minecraft:location","conditions":{"player":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"minecraft:location":{"biomes":"minecraft:dappled_forest"}}}]}}` and requirement `["minecraft:dappled_forest"]` (`:929`).
- difference: Visiting a dappled forest doesn't count towards Adventuring Time.
- fix_kind: data-override (`data/minecraft/advancement/adventure/adventuring_time.json`)
- fix: add the criterion and requirement in the merged override from GARDEN-21 (one file shared with pale garden), behind a mod-config condition.
- config_gate: `Wilderness Bound.has_dappled_forest`

### WILD-30  Cushion fire check every tick, no lava/fluid effect (was MISC-06; re-verified)
- severity: gameplay
- vb: `Cushion.java` `tick() { super.tick(); if (level() instanceof ServerLevel s) this.destroyIfInFire(s); }`, `destroyIfInFire` → `hurt(inFire, 1.0F)`. 1.21.1 `BlockAttachedEntity.tick` only runs `survives()` every 100 ticks.
- native: `BlockAttachedEntity.java:42-53` every 100 ticks `tickAtCheckInterval()`; `Cushion.tickAtCheckInterval` (`:79-90`): `if (collidedWithFluid(...)) { fluidState.entityInside(...); insideEffectCollector.applyAndClear(this); } this.destroyIfInFire(level);`
- difference: VB breaks a cushion in fire within 1 tick (native up to 100 ticks). Lava never breaks a cushion in VB.
- fix_kind: mixin-vb (`Cushion.tick`)
- fix: `@Redirect` the `destroyIfInFire` call in `tick` behind an own counter `if (++wild$check >= 100) { wild$check = 0; if (level.getFluidState(blockPosition()).is(FluidTags.LAVA)) hurt(damageSources().lava(), 4.0F); destroyIfInFire(level); }`.
- config_gate: none

### WILD-31  Cushion and wool-slab fuel times (was MISC-36; re-verified)
- severity: gameplay
- vb: `BlockIntegrations.java:22-23` `registerFuelItem(ModItemTags.WOOL_SLABS, 100); registerFuelItem(ModItemTags.CUSHIONS, 200);`
- native: `Items.java:2024` cushion `cookingFuel(COOKING_TIME_WOOL_SLABS)`; wool slabs the same; `context_int_provider/cooking/time_wool_slabs.json` left 50 (÷1) = 50 ticks. Wool stairs `time_wool` 100 (match).
- difference: Cushions burn 200 ticks (native 50); wool slabs 100 (native 50).
- fix_kind: data-override (`data/neoforge/data_maps/item/furnace_fuels.json`)
- fix: `{"values":{"#minecraft:wool_slabs":{"burn_time":50},"#minecraft:cushions":{"burn_time":50}}}` (data maps override the event registration).
- config_gate: none

### WILD-32  Cushion break particles, lightning drop, sit-click result (was MISC-37; re-verified)
- severity: gameplay
- vb: `Cushion.showBreakingParticles` `sendParticles(BLOCK wool, x, getY(0.67F), z, 10, bbW/4, bbW/4, bbW/4, 0.05)`; `thunderHit` → `kill(); dropItem(null);`; `interact`: client `SUCCESS`, server `CONSUME` after `startRiding`, else `PASS`.
- native: `Cushion.java:115-129` y-spread `getBbHeight()/4F`, `getY(0.6666666666666666)`; `dropItem` (`:21-33`) `itemEntity.setInvulnerableTime(20)` when `causedBy instanceof LightningBolt`; `interact` (`:53-63`) `secondary || isVehicle → PASS; !client && startRiding → CUSHION_SIT, SUCCESS_SERVER; else CONSUME`.
- difference: Particles spread 0.25 block vertically instead of 0.0625. A lightning-broken cushion's item can burn up in VB. Clicking a cushion client-side returns SUCCESS (hand swing), and a failed mount passes the click to the held item.
- fix_kind: mixin-vb (`Cushion.showBreakingParticles`, `thunderHit`, `interact`)
- fix: `@ModifyArg` index 6 (yDist) of `sendParticles` → `(double)(getBbHeight()/4F)`. In `thunderHit` replace `dropItem(null)` with: play `CUSHION_BREAK`, show particles, then if `doEntityDrops` `ItemEntity e = spawnAtLocation(<stack with CUSTOM_NAME>); if (e != null) e.setInvulnerableTime(20);`. `interact` HEAD cancellable: `if (player.isSecondaryUseActive() || isVehicle()) return PASS; if (!level().isClientSide && player.startRiding(this)) { playSound(CUSHION_SIT); return SUCCESS; } return CONSUME;`
- config_gate: none

### WILD-33  Cushions facing north or south render rotated 180°
- severity: visual
- vb: `client/level/renderer/object/CushionRenderer.java:44` `pose.mulPose(Axis.YP.rotationDegrees(Direction.fromYRot(cushion.getYRot()).toYRot()));`
- native: `client/renderer/entity/CushionRenderer.java:39` `poseStack.rotateDegrees(Axis.YP, 180.0F - state.direction.toYRot());`
- difference: For east/west (toYRot 270/90) both give the same angle. For south (0) VB rotates 0° vs native 180°, and for north (180) 180° vs 0°. The cushion texture is not 180°-symmetric (top face differs by up to 35 per channel when rotated; north/south side faces differ), so N/S cushions show the pattern turned around.
- fix_kind: mixin-vb (`CushionRenderer.render`)
- fix: `@ModifyArg(method="render", at=@At(value="INVOKE", target="Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal=0))` → `return 180.0F - angle;`
- config_gate: none

### WILD-34  Dye recipes for cushions, wool slabs and wool stairs accept the target colour; cushion dye recipes are filed under building blocks
- severity: cosmetic
- vb: `recipe/dye_black_cushion.json` `"category":"building","ingredients":[{"item":"minecraft:black_dye"},{"tag":"minecraft:cushions"}]` (the same for all 16 colours, and `dye_<c>_wool_slab` / `dye_<c>_wool_stairs` with `#wool_slabs` / `#wool_stairs`); advancements in `advancement/recipes/building_blocks/dye_<c>_cushion.json`.
- native: `dye_black_cushion.json` ingredients `["minecraft:black_dye", [the 15 other cushions]]` (the same pattern for wool slabs/stairs); advancements in `advancement/recipes/decorations/dye_<c>_cushion.json`.
- difference: VB lets a black cushion + black dye make a black cushion (wasting dye), and dye-cushion recipes appear in the building recipe-book tab instead of decorations.
- fix_kind: data-override (`data/minecraft/recipe/dye_<c>_{cushion,wool_slab,wool_stairs}.json`)
- fix: for each colour write the native ingredient list as `[{"item":"minecraft:<c>_dye"}, [{"item":"minecraft:<other>_cushion"}, … 15 entries]]` and `"category":"misc"` for the cushion dye recipes. Ship the advancements under `recipes/decorations/` (the old `building_blocks` files can stay; they only unlock).
- config_gate: none

### WILD-35  Poplar chest boat item name
- severity: cosmetic
- vb: `assets/vanillabackport/lang/en_us.json` `"item.minecraft.poplar_chest_boat": "Poplar Chest Boat"`.
- native: `"item.minecraft.poplar_chest_boat": "Poplar Boat with Chest"` (and `entity.minecraft.poplar_boat` / `poplar_chest_boat` "Poplar Boat" / "Poplar Boat with Chest").
- difference: Item name differs.
- fix_kind: asset-override (`assets/minecraft/lang/en_us.json`)
- fix: `{"item.minecraft.poplar_chest_boat":"Poplar Boat with Chest"}`.
- config_gate: none

### WILD-36  No fall subtitles for poplar leaves, shelf mushroom and straw bed
- severity: audio
- vb: `assets/minecraft/sounds.json` `"block.poplar_leaves.fall": {"sounds":[leaf_litter/step1..6]}`, `"block.shelf_mushroom.fall": {"sounds":[shelf_mushroom/bounce1..5]}`, `"block.straw_bed.fall": {"sounds":[straw_bed/hit1..6]}` (no `subtitle`).
- native: `assets-26.3/sounds.json` same sound lists plus `"subtitle": "subtitles.block.generic.fall"` on all three. All other 25 WB events and all 111 `.ogg` files are identical.
- difference: Falling onto these blocks shows no subtitle in VB.
- fix_kind: asset-override (`assets/minecraft/sounds.json`)
- fix: `{"block.poplar_leaves.fall":{"replace":true,"sounds":["block/leaf_litter/step1","block/leaf_litter/step2","block/leaf_litter/step3","block/leaf_litter/step4","block/leaf_litter/step5","block/leaf_litter/step6"],"subtitle":"subtitles.block.generic.fall"},"block.shelf_mushroom.fall":{"replace":true,"sounds":["block/shelf_mushroom/bounce1","block/shelf_mushroom/bounce2","block/shelf_mushroom/bounce3","block/shelf_mushroom/bounce4","block/shelf_mushroom/bounce5"],"subtitle":"subtitles.block.generic.fall"},"block.straw_bed.fall":{"replace":true,"sounds":["block/straw_bed/hit1","block/straw_bed/hit2","block/straw_bed/hit3","block/straw_bed/hit4","block/straw_bed/hit5","block/straw_bed/hit6"],"subtitle":"subtitles.block.generic.fall"}}`
- config_gate: none

### WILD-37  `c:dyed/green` item tag lists the gray cushion
- severity: data
- vb: `data/c/tags/item/dyed/green.json` contains `minecraft:gray_cushion` and not `minecraft:green_cushion`.
- native: no `c:` tags; the intended member is the green cushion (all other colours list their own cushion).
- difference: Mods that use `#c:dyed/green` treat gray cushions as green and miss green cushions.
- fix_kind: data-override (`data/c/tags/item/dyed/green.json`)
- fix: ship VB's file with `"replace":true` and `minecraft:green_cushion` in place of `minecraft:gray_cushion`.
- config_gate: none

### WILD-38  Concrete slabs/stairs are not in `#slabs` / `#stairs`
- severity: data
- vb: `tags/block/slabs.json`, `tags/item/slabs.json` add `#minecraft:wool_slabs` but not `#minecraft:concrete_slabs`; `stairs.json` likewise lacks `#minecraft:concrete_stairs` (`concrete_slabs.json`/`concrete_stairs.json` exist).
- native: `tags/block/slabs.json`, `item/slabs.json` include `#minecraft:concrete_slabs`; `stairs.json` include `#minecraft:concrete_stairs`.
- difference: Anything keyed on `#slabs`/`#stairs` (mod compat, `c:` derivations, mob spawn and placement checks that use the tags) skips the 32 concrete slab/stair blocks.
- fix_kind: data-override
- fix: `tags/block/slabs.json` and `tags/item/slabs.json` `{"replace":false,"values":["#minecraft:concrete_slabs"]}`; `tags/block/stairs.json` and `tags/item/stairs.json` `{"replace":false,"values":["#minecraft:concrete_stairs"]}`.
- config_gate: none

### WILD-39  Windswept camp map tag has a different id
- severity: data
- vb: `tags/worldgen/structure/on_abandoned_camp_windswept_forest.json` `["minecraft:abandoned_camp_windswept_forest"]`, used by the common chest.
- native: `tags/worldgen/structure/on_abandoned_camp_windswept.json` with the same single value (the common chest uses `#minecraft:on_abandoned_camp_windswept`).
- difference: Same behaviour; the tag id differs, so commands or data packs that use the native id find nothing.
- fix_kind: data-override
- fix: add `data/minecraft/tags/worldgen/structure/on_abandoned_camp_windswept.json` `{"values":["minecraft:abandoned_camp_windswept_forest"]}`, and point the loot entry at it when overriding the chest (WILD-08/10/11).
- config_gate: none

### WILD-41  Black cushion entity texture differs slightly
- severity: cosmetic
- vb: `assets/minecraft/textures/entity/cushion/black_cushion.png`
- native: same path
- difference: 177 texels differ by at most 2 per channel (the other 15 colours and all item icons are pixel-identical).
- fix_kind: asset-override (`assets/minecraft/textures/entity/cushion/black_cushion.png`)
- fix: copy the 26.3 file.
- config_gate: none
