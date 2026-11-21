# flora — Spring to Life + Wilderness Bound flora / worldgen parity audit (VB 1.2.2 vs MC 26.2)

**Summary**
1. 20 findings: 5 gameplay, 2 audio, 4 visual, 8 data, 1 cosmetic. The largest ones: sand ambient sounds never play (FLORA-02), 3-tall cacti never grow flowers (FLORA-04), and the way VB places fallen trees and leaf litter (FLORA-07/08). Native places both from inside each biome's tree selector, while VB uses separate per-chunk features, so the per-biome rates are 2x–10x off.
2. Covered: every flora block class (bush, firefly bush, cactus flower, leaf litter, wildflowers, short/tall dry grass, particle/atmospheric leaves), the falling-leaves and leaf-litter client modules, leaf and dry-foliage colours, both particles, desert ambient sounds, the cactus mixin, every Spring to Life configured/placed feature JSON and every biome tag that injects one, fallen tree, leaf litter, cactus flower, sequence, template and weighted-selector features, tree decorators, loot tables, recipes, tags, block properties, composter/fuel/flammability values, textures (pixel-compared: all identical) and models.
3. Wilderness Bound (dappled forest, poplar trees with their foliage and trunk placers, shelf mushroom with its decorator, red shrub, straw bed and cushion, abandoned camps, TerraBlender region): none of it exists in 26.2 (no `poplar`, `dappled`, `shelf_mushroom`, `straw`, `abandoned_camp` anywhere in src-mc). It is listed as "no native counterpart" and was not diffed further.
4. Not checkable: `sounds.json` sound definitions (26.2 sounds.json and the .ogg files are not in the jar), and the volume/pitch of `SoundRegistry.soundType(...)` (it lives in the BlackGear platform library, which is not decompiled; assumed 1.0/1.0).
5. Notes for the fixes: VB's worldgen JSON under `data/minecraft/worldgen` is what runs (the registrar classes are datagen and their values match the JSON). 26.2 tree-feature JSON is not loadable on 1.21.1 (`below_trunk_provider`, `rule_based_state_provider`), so every worldgen fix below is written in 1.21.1 format and reuses VB's `minecraft:`-namespaced types (`minecraft:fallen_tree`, `minecraft:place_on_ground`, `minecraft:attached_to_logs`).

## Coverage

| VB class / data | Native counterpart (26.2) | Result |
|---|---|---|
| `common/level/blocks/ActualBushBlock` | `world/level/block/BushBlock` | match (shape 16x13, spread bonemeal, `getType` GROWER) |
| `common/level/blocks/FireflyBushBlock` | `FireflyBushBlock` | findings (FLORA-01) |
| `common/level/blocks/CactusFlowerBlock` | `CactusFlowerBlock` | match |
| `common/level/blocks/LeafLitterBlock`, `SegmentableBlock` | `LeafLitterBlock`, `SegmentableBlock` | match |
| wildflowers (`PinkPetalsBlock` from 1.21.1) | `FlowerBedBlock` | match (shape quadrants, bonemeal +1 / pop at 4, both checked) |
| `common/level/blocks/ShortDryGrassBlock`, `TallDryGrassBlock` (extend 1.21.1 `DeadBushBlock`) | `ShortDryGrassBlock`, `TallDryGrassBlock`, `DryVegetationBlock` | findings (FLORA-03, FLORA-16) |
| `common/level/blocks/ParticleLeavesBlock` (pale oak 0.02) | `UntintedParticleLeavesBlock` | match |
| `common/level/blocks/AtmosphericLeavesBlock` + `common/level/sounds/AmbientLeavesBlockSoundPlayer` (poplar) | — | no native counterpart |
| `common/level/blocks/ShelfMushroomBlock`, `StrawBedBlock`, `items/CushionItem`, `core/mixin/common/straw_bed/*`, `client/.../StrawBedItemRenderer`, `CushionModel/Renderer` | — | no native counterpart |
| `ModBlocks` flora properties (map colour, sound, replaceable, offset, light 2, push reaction) | `Blocks` | match |
| `BlockIntegrations` compost 0.3 / flammability 60,100 / fuel 100 | `ComposterBlock`, `FireBlock`, `FuelValues` | match |
| `core/mixin/client/falling_leaves/LeavesBlockMixin` + `client/api/modules/falling_leaves/*` | `LeavesBlock.makeFallingLeavesParticles`, `TintedParticleLeavesBlock`, `Blocks` leaf chances | findings (FLORA-17, FLORA-18) |
| `client/api/modules/leaf_litter/*` (`DryFoliageColor`, reload listeners), `assets/minecraft/dry_foliage_colors`, `leaf_colors` | `DryFoliageColor`, `ColorMapColorUtil`, `Biome.getDryFoliageColor`, biome `dry_foliage_color` effects, `BlockColors` | findings (FLORA-19); colours (#7b5334, #a0a69c, azalea -9399763) and colormap png match |
| `client/level/particle/FallingLeavesParticle` | `client/particle/FallingLeavesParticle` | match (cherry stays on 1.21.1 `CherryParticle`, which is the same math as native `CherryProvider` 0.25/2.0/false/true/1.0/0.0) |
| `client/level/particle/FireflyParticle` | `client/particle/FireflyParticle` | findings (FLORA-20) |
| `client/level/sound/AmbientDesertBlockSoundsPlayer`, `core/mixin/common/desert_ambient_sounds/*` | `block/sounds/AmbientDesertBlockSoundsPlayer`, `SandBlock`, `DryVegetationBlock` | findings (FLORA-02, FLORA-03) |
| `core/mixin/common/cactus_flowers/CactusBlockMixin` | `CactusBlock.randomTick/canSurvive` | findings (FLORA-04); `canSurvive` with `supports_cactus` matches |
| `core/mixin/common/leaf_litter/ChunkSerializerMixin` | — (VB's own old-save fixer) | no native counterpart |
| `client/level/layer/BlockDecorationLayer`, `renderer/block/BackportedBlockRenderer`, `core/neoforge/emissive/*` (firefly bush emissive) | `cross_emissive` model `light_emission:15` | match (engine-equivalent emissive quads) |
| `client/resource/SimpleJsonResourceReloadListener`, `StrictJsonParser` | — (infrastructure) | n/a |
| `worldgen/features/FallenTreeFeature` + `FallenTreeConfiguration` | `levelgen/feature/FallenTreeFeature` | findings (FLORA-06) |
| `worldgen/features/LeafLitterFeature` + `LeafLitterConfiguration` | none (native uses `PlaceOnGroundDecorator` on trees) | findings (FLORA-08) |
| `worldgen/tree/decorators/PlaceOnGroundDecorator` | `treedecorators/PlaceOnGroundDecorator` | match |
| `worldgen/tree/decorators/AttachedToLogsDecorator` | `treedecorators/AttachedToLogsDecorator` | match |
| `worldgen/tree/decorators/ShelfMushroomDecorator`, `tree/foliage/PoplarFoliagePlacer`, `tree/trunk/PoplarTrunkPlacer`, `ModFoliagePlacers`, `ModTrunkPlacers` | — | no native counterpart |
| `worldgen/features/CactusFlowerFeature` | `configured_feature/cactus.json` (block_column with flower layer) | findings (FLORA-05) |
| `worldgen/features/SequenceFeature`, `TemplateFeature(+Configuration)`, `WeightedRandomSelectorFeature(+Configuration)` | same-named native classes | match (VB only adds an optional `processors` field) |
| `ModFeatures`, `ModTreeDecorators` | `Feature`, `TreeDecoratorType` | match (ids `minecraft:fallen_tree`, `leaf_litter`, `cactus_flower`, `sequence`, `template`, `weighted_random_selector`, `place_on_ground`, `attached_to_logs`) |
| `worldgen/features/SpringToLifeFeatures`, `placements/SpringToLifePlacements`, `SpringToLifeFeatureManager`, data `worldgen/configured_feature/*`, `placed_feature/*` | 26.2 `configured_feature`, `placed_feature`, `biome/*.json` | findings (FLORA-07, -09, -10, -11, -12); wildflowers, dry grass, leaf-litter patch, firefly placement chains match |
| `tags/worldgen/biome/spawns_*` | 26.2 biome feature lists | findings (FLORA-07, -10, -11) |
| `worldgen/features/WildernessBoundFeatures`, `placements/WildernessBoundPlacements`, `WildernessBoundFeatureManager`, `biomes/WildernessBoundBiomes`, `ModBiomes` dappled forest, `structures/**`, `common/level/worldgen/structure/AbandonedCampStructurePools`, `integrations/worldgen/BiomeGeneration` (dappled part), `compat/terrablender/OverworldRegion`, `core/neoforge/VanillaBackportTerrablender` | — | no native counterpart (abandoned camps also add a structure set to 18 vanilla biomes; there is no config gate for camps, and `has_dappled_forest` gates only the biome) |
| loot tables (bush, cactus_flower, firefly_bush, leaf_litter, short/tall_dry_grass, wildflowers) | same | match (format-only diffs) |
| recipes leaf_litter (smelting 0.1 xp, 200 t), pink/yellow dye | same | match |
| block/item tags `flowers`, `inside_step_sound_blocks`, `replaceable_by_trees`, `enderman_holdable`, `support_override_cactus_flower`, `supports_cactus`, `triggers_ambient_desert_*` | same | match |
| block tags `replaceable`, `edible_for_sheep`, `dead_bush_may_place_on` vs native `supports_dry_vegetation` | native tags | findings (FLORA-14, -15, -16) |
| grass bonemeal flower pick (1.21.1 `GrassBlock` + `BiomeGenerationSettings.getFlowerFeatures`) | `GrassBlock.performBonemeal` + `can_spawn_from_bone_meal` | findings (FLORA-13) |
| textures (leaf_litter, bush, cactus_flower, firefly_bush (+emissive), dry grass, wildflowers, dry_foliage colormap, firefly particle) | same | match (pixel-identical; binary_diff is encoding only) |
| models/blockstates (firefly_bush cross vs cross_emissive, wildflowers multipart) | same | match (equivalent) |

---

### FLORA-01  Firefly particles spawn at night regardless of light level
- severity: visual
- vb: `com/blackgear/vanillabackport/common/level/blocks/FireflyBushBlock.java:42` (animateTick) — `if ((WorldUtilities.EnvironmentUtils.isNaturalNight(level) || level.getMaxLocalRawBrightness(pos) <= 13) && random.nextDouble() <= 0.7) {`
- native: `net/minecraft/world/level/block/FireflyBushBlock.java:42` (animateTick) — `if (level.getMaxLocalRawBrightness(pos) <= 13 && random.nextDouble() <= 0.7) {`
- difference: Native spawns firefly particles only when the raw brightness is ≤ 13, day or night. VB also spawns them whenever it is overworld night (day time 12600–23400), even at brightness 14–15 (next to torches or glowstone). In that case VB also draws an extra `nextDouble()` that native never draws. The idle-sound condition matches: 1/30, plus native's `FIREFLY_BUSH_SOUNDS` timeline track 12600→23401, which is the same window as VB's `isNaturalNight`.
- fix_kind: mixin-vb
- fix: `@Mixin(com.blackgear.vanillabackport.common.level.blocks.FireflyBushBlock.class)`, `@Redirect(method="animateTick", at=@At(value="INVOKE", target="Lcom/blackgear/vanillabackport/core/util/WorldUtilities$EnvironmentUtils;isNaturalNight(Lnet/minecraft/world/level/Level;)Z", ordinal=1))` returning `false`. Ordinal 1 is the second call, the particle branch; the first call belongs to the sound branch and must stay. The condition then reduces to `getMaxLocalRawBrightness(pos) <= 13 && nextDouble() <= 0.7`, which is native.
- config_gate: none

### FLORA-02  Sand / red sand ambient idle sound never plays
- severity: audio
- vb: `com/blackgear/vanillabackport/core/mixin/common/desert_ambient_sounds/ColoringFallingBlockMixin.java:15-16` — `public void vb$animateTick(...) { AmbientDesertBlockSoundsPlayer.playAmbientSandSounds(level, pos, random); }`. This only runs from `core/mixin/common/extension/BlockMixin.java:17-21`: `@Inject(method="animateTick", at=@At("HEAD")) ... BlockExtension.of(this).vb$animateTick(...)` on `Block.class`. But 1.21.1 `net/minecraft/world/level/block/FallingBlock.java:66` overrides `animateTick` (`if (random.nextInt(16) == 0) {...falling dust...}`) without calling `super`, and `ColoredFallingBlock` (sand, red sand) inherits that override, so `Block.animateTick` is never entered for sand.
- native: `net/minecraft/world/level/block/SandBlock.java:28-31` — `super.animateTick(state, level, pos, random); AmbientDesertBlockSoundsPlayer.playAmbientSandSounds(level, pos, random);`. The player's body is `if (level.getBlockState(pos.above()).is(Blocks.AIR)) { if (random.nextInt(2100) == 0 && shouldPlayAmbientSandSound(level, pos)) playLocalSound(..., SoundEvents.SAND_IDLE, AMBIENT, 1.0F, 1.0F, false) }`.
- difference: Native plays `block.sand.idle` with a 1/2100 chance per animate tick on exposed sand that has ≥3 of 4 matching sand columns 8 blocks away. VB's code for this is correct but unreachable, so the sound never plays.
- fix_kind: mixin-mc
- fix: `@Mixin(net.minecraft.world.level.block.FallingBlock.class)`, `@Inject(method="animateTick", at=@At("TAIL"))`: `if ((Object)this instanceof ColoredFallingBlock && (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND))) com.blackgear.vanillabackport.client.level.sound.AmbientDesertBlockSoundsPlayer.playAmbientSandSounds(level, pos, random);`. TAIL keeps native order: falling dust (`nextInt(16)`) first, then the sand sound. Mark it client-only or guard with `level.isClientSide`.
- config_gate: none

### FLORA-03  Dry grass ambient sound is positional instead of player-bound
- severity: audio
- vb: `com/blackgear/vanillabackport/client/level/sound/AmbientDesertBlockSoundsPlayer.java:37` (playAmbientDryGrassSounds) — `level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), ModSoundEvents.DRY_GRASS.get(), SoundSource.AMBIENT, 1.0F, 1.0F, false);`
- native: `net/minecraft/world/level/block/sounds/AmbientDesertBlockSoundsPlayer.java:34` — `level.playPlayerSound(SoundEvents.DRY_GRASS, SoundSource.AMBIENT, 1.0F, 1.0F);`. `ClientLevel.playPlayerSound` (ClientLevel.java:721-724) does `getSoundManager().play(new EntityBoundSoundInstance(sound, source, volume, pitch, this.minecraft.player, this.random.nextLong()))`.
- difference: Native plays the `block.dry_grass.ambient` wind sound bound to the local player (it follows the listener and has no distance falloff from the grass). VB plays it at the grass block's corner (x, y, z integer coords, not +0.5) with normal attenuation. The 1/200 chance and the two-blocks-of-sand/terracotta-below trigger are identical.
- fix_kind: mixin-vb
- fix: `@Mixin(AmbientDesertBlockSoundsPlayer.class /* VB */)`, `@Redirect(method="playAmbientDryGrassSounds", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))` → `Minecraft mc = Minecraft.getInstance(); if (mc.player != null) mc.getSoundManager().play(new EntityBoundSoundInstance(sound, source, vol, pitch, mc.player, level.random.nextLong()));`
- config_gate: none

### FLORA-04  Cactus taller than 2 never ages, so 3-tall cacti never grow a flower (25% branch is dead)
- severity: gameplay
- vb: `com/blackgear/vanillabackport/core/mixin/common/cactus_flowers/CactusBlockMixin.java:46-56` (HEAD of randomTick) — `while (...is(cactus)) { if (height++ == 3 && age == 15) return; } if (age == 8 && this.canSurvive(this.defaultBlockState(), level, above)) { double flowerChance = height >= 3 ? 0.25 : 0.1; if (random.nextDouble() <= flowerChance) level.setBlockAndUpdate(above, CACTUS_FLOWER) }`. Vanilla 1.21.1 `CactusBlock.randomTick` then runs unchanged (src-121 CactusBlock.java:56-63): `while (...) { i++; } if (i < 3) { ... if (j == 15) grow else setBlock(AGE j+1) }`, so ageing happens **only when the column is <3 tall**.
- native: `net/minecraft/world/level/block/CactusBlock.java:54-80` — `while (...is(this)) { if (++height == 3 && age == 15) return; } if (age == 8 && canSurvive(default, pos.above())) { chance = height >= 3 ? 0.25 : 0.1; ... } else if (age == 15 && height < 3) { grow } if (age < 15) { level.setBlock(pos, state.setValue(AGE, age + 1), 260); }`
- difference: Native ages every top cactus block from 0 to 15, including the top of a 3-tall column. That top passes age 8, where it gets one 25% flower roll, and then stops at 15. In VB/1.21.1 the top of a 3-tall column stays at age 0 forever, so it never flowers, whereas native gives it a 25% chance per growth cycle. Columns of 1–2 behave the same as native: one 10% roll at age 8. A minor extra difference: after placing a flower, native still increments the age that tick; VB does not.
- fix_kind: mixin-mc
- fix: `@Mixin(net.minecraft.world.level.block.CactusBlock.class)`, `@Inject(method="randomTick", at=@At("HEAD"), cancellable=true)` (registered after VB's mixin, or with a higher priority), then `ci.cancel()` and run the native body: `BlockPos above=pos.above(); if(!level.isEmptyBlock(above)) return; int height=1; int age=state.getValue(AGE); while(level.getBlockState(pos.below(height)).is(this)){ if(++height==3&&age==15) return; } if(age==8 && this.canSurvive(this.defaultBlockState(),level,above)){ if(VanillaBackport.COMMON_CONFIG.hasCactusFlowers.get() && random.nextDouble() <= (height>=3?0.25:0.1)) level.setBlockAndUpdate(above, ModBlocks.CACTUS_FLOWER.get().defaultBlockState()); } else if(age==15 && height<3 && CommonHooks.canCropGrow(level,above,state,true)){ level.setBlockAndUpdate(above,this.defaultBlockState()); BlockState s=state.setValue(AGE,0); level.setBlock(pos,s,4); level.neighborChanged(s,above,this,pos,false); CommonHooks.fireCropGrowPost(level,pos,state); } if(age<15) level.setBlock(pos,state.setValue(AGE,age+1),4);`. Use flag 4 (1.21.1; the native 260 adds 1.21.2+ bit 256). Also disable VB's HEAD inject, either with `@Inject` priority or by setting its body to a no-op through a mixin on `CactusBlockMixin.vb$growCactusFlower`.
- config_gate: `cactus_flowers` (COMMON `hasCactusFlowers`), for the flower placement only

### FLORA-05  Cactus flower worldgen: per-chunk post-scan instead of a flower layer inside the cactus column
- severity: data
- vb: `data/minecraft/worldgen/placed_feature/cactus_flower.json` — `{"feature":"minecraft:cactus_flower","placement":[surface_water_depth_filter 0, heightmap OCEAN_FLOOR, biome]}` (no in_square; origin = chunk corner). `CactusFlowerFeature.java:29-37`: `for x<16, z<16: if (!(random.nextFloat() >= spawnChance /*0.25*/)) { surfaceY = getHeight(MOTION_BLOCKING) - 1; if top is CACTUS && above air && canSurvive → setBlock(flower) }`. It is added only to biomes that have `patch_cactus_decorated`/`patch_cactus_desert` (`SpringToLifeFeatureManager`), at the end of vegetal decoration.
- native: `data/minecraft/worldgen/configured_feature/cactus.json` — `block_column` layers `[{height: biased_to_bottom 1..3, provider: cactus age 0}, {height: weighted_list [{data:0,weight:3},{data:1,weight:1}], provider: cactus_flower}]`, `allowed_placement: air`. Each cactus column rolls its own 1-in-4 flower at placement, in every use of `cactus` (desert, badlands patches, and desert-village `patch_cactus` decor).
- difference: Native puts a flower on 25% of cacti at the moment each one generates. VB rolls 25% per (x,z) column of the chunk, once, after all of that chunk's vegetation. Cacti that a later neighbour chunk spills into this chunk (patch spread is ±7) are never considered. Cacti outside the four cactus biomes never get flowers. The extra `nextFloat()` per column also shifts the RNG stream for every later feature in the step.
- fix_kind: data-override
- fix: (1) `data/minecraft/worldgen/configured_feature/cactus.json` (1.21.1 format): `{"type":"minecraft:block_column","config":{"allowed_placement":{"type":"minecraft:matching_blocks","blocks":"minecraft:air"},"direction":"up","layers":[{"height":{"type":"minecraft:biased_to_bottom","min_inclusive":1,"max_inclusive":3},"provider":{"type":"minecraft:simple_state_provider","state":{"Name":"minecraft:cactus","Properties":{"age":"0"}}}},{"height":{"type":"minecraft:weighted_list","distribution":[{"data":0,"weight":3},{"data":1,"weight":1}]},"provider":{"type":"minecraft:simple_state_provider","state":{"Name":"minecraft:cactus_flower"}}}],"prioritize_tip":false}}`. (2) Neutralise VB's pass: `data/minecraft/worldgen/placed_feature/cactus_flower.json` → `{"feature":{"type":"minecraft:no_op","config":{}},"placement":[]}`. Because `cactus_flowers=false` must still mean no flowers, gate (1) with a NeoForge `neoforge:conditions` on a custom condition reading `hasCactusFlowers`, or swap the configured feature from code at server start.
- config_gate: `cactus_flowers` (+ VB's `cactus_flower_spawn_chance`, which has no native equivalent; native is fixed at weight 1:3)

### FLORA-06  Fallen tree tolerates 3 consecutive unsupported log blocks instead of 2
- severity: data
- vb: `com/blackgear/vanillabackport/common/level/worldgen/features/FallenTreeFeature.java:70-72` (canPlaceEntireFallenLog) — `if (!this.isOverSolidGround(level, mutable)) { if (gaps++ > 2) { return false; } }`
- native: `net/minecraft/world/level/levelgen/feature/FallenTreeFeature.java:74-76` — `if (!this.isOverSolidGround(level, logStartPos)) { if (++gapInGround > 2) { return false; } }`
- difference: The post-increment fails only on the 4th consecutive gap. Native's pre-increment fails on the 3rd. VB therefore generates fallen logs that bridge 3-block-wide holes or ravine edges, where native rejects the log and leaves just the stump. Everything else in the class matches: stump, direction, `logLength - 2`, offset `2 + nextInt(2)`, the 6-step ground search, and the flags 3/19.
- fix_kind: mixin-vb
- fix: `@Mixin(com.blackgear.vanillabackport.common.level.worldgen.features.FallenTreeFeature.class)`, `@ModifyConstant(method="canPlaceEntireFallenLog", constant=@Constant(intValue=2))` → return `1`. The test becomes `gaps++ > 1`, which fails on the 3rd consecutive gap, identical to native `++gap > 2`.
- config_gate: `fallen_trees`

### FLORA-07  Fallen trees: separate rarity features instead of entries inside each biome's tree selector, so per-biome rates are wrong
- severity: data
- vb: `data/minecraft/worldgen/placed_feature/placed_fallen_*_tree.json` — oak/birch/jungle/spruce `rarity_filter 80`, `placed_rare_fallen_birch_tree` `rarity_filter 400`, `placed_fallen_super_birch_tree` `160`, `placed_rare_fallen_spruce_tree` `120`, each followed by `in_square, surface_water_depth_filter 0, heightmap OCEAN_FLOOR, biome, would_survive <sapling>`. They are injected by `SpringToLifeFeatureManager` (`hasFallenTrees`) through these tags: `spawns_fallen_oak_trees` = dark_forest, #is_badlands, #is_savanna, #is_hill, forest, flower_forest, c:plains; `spawns_fallen_birch_trees` = birch_forest, old_growth_birch_forest; `spawns_fallen_birch_trees_rarely` = #is_forest minus pale_garden; `spawns_fallen_super_birch_trees` = old_growth_birch_forest; `spawns_fallen_jungle_trees` = #is_jungle; `spawns_fallen_spruce_trees` = #is_taiga; `spawns_fallen_spruce_trees_rarely` = #is_hill.
- native: the fallen trees are `random_selector` entries inside the tree features (`data/minecraft/worldgen/configured_feature/trees_*.json`), so each tree attempt can become a fallen tree:
  - `trees_birch`: `[fallen_birch_tree 0.0125]`, default `birch_bees_0002`
  - `trees_birch_and_oak_leaf_litter` (forest): `[fallen_birch_tree 0.0025, birch_bees_0002_leaf_litter 0.2, fancy_oak_bees_0002_leaf_litter 0.1, fallen_oak_tree 0.0125]`
  - `dark_forest_vegetation`: `[huge_brown 0.025, huge_red 0.05, dark_oak_leaf_litter 0.6666667, fallen_birch_tree 0.0025, birch_leaf_litter 0.2, fallen_oak_tree 0.0125, fancy_oak_leaf_litter 0.1]`
  - `trees_flower_forest`: `[fallen_birch_tree 0.0025, birch_bees_002 0.2, fancy_oak_bees_002 0.1]`
  - `birch_tall`: `[fallen_super_birch_tree 0.00625, super_birch_bees_0002 0.5, fallen_birch_tree 0.0125]`
  - `trees_jungle`: `[fancy_oak_checked 0.1, jungle_bush 0.5, mega_jungle_tree_checked 0.33333334, fallen_jungle_tree 0.0125]`
  - `trees_sparse_jungle`: `[fancy_oak_checked 0.1, jungle_bush 0.5, fallen_jungle_tree 0.0125]`
  - `trees_taiga`: `[pine_checked 0.33333334, fallen_spruce_tree 0.0125]`
  - `trees_old_growth_pine_taiga`: `[mega_spruce 0.025641026, mega_pine 0.30769232, pine 0.33333334, fallen_spruce_tree 0.0125]`
  - `trees_old_growth_spruce_taiga`: `[mega_spruce 0.33333334, pine 0.33333334, fallen_spruce_tree 0.0125]`
  - `trees_snowy`: `[fallen_spruce_tree 0.0125]`
  - `trees_plains`: `[fancy_oak_bees_005 0.33333334, fallen_oak_tree 0.0125]`
  - `trees_savanna` (also used by `trees_windswept_savanna`): `[acacia_checked 0.8, fallen_oak_tree 0.0125]`
  - `trees_windswept_hills` (also used by `trees_windswept_forest`): `[fallen_spruce_tree 0.008325, spruce_checked 0.666, fancy_oak_checked 0.1, fallen_oak_tree 0.0125]`
  - `trees_badlands`: `[fallen_oak_tree 0.0125]`, default `oak_leaf_litter`
- difference: Expected fallen trees per chunk, native vs VB:

| Biome | Native | VB |
|---|---|---|
| forest | oak 0.0907 / birch 0.0252 | 0.0125 / 0.0025 |
| birch_forest | birch 0.126 | 0.015 |
| old_growth_birch_forest | super 0.0631 / birch 0.0627 | 0.00625 / 0.015 |
| dark_forest | oak 0.0493 / birch 0.0123 | 0.0125 / 0.0025 |
| flower_forest | birch 0.0152 / oak 0 | 0.0025 / 0.0125 |
| jungle | 0.188 | 0.0125 |
| sparse_jungle | 0.0118 | 0.0125 |
| bamboo_jungle | 0 | 0.0125 |
| taiga, snowy_taiga | 0.0842 | 0.0125 |
| old_growth pine/spruce taiga | 0.057 | 0.0125 |
| snowy_plains, ice_spikes | spruce 0.0013 | 0 |
| plains, sunflower_plains | 0.0004 | 0.0125 |
| savanna / savanna_plateau / windswept_savanna | 0.0027 / 0.0027 / 0.0052 | 0.0125 each |
| windswept_forest | spruce 0.0258 / oak 0.0116 | 0.00833 / 0.0125 |
| windswept_hills, windswept_gravelly_hills | 0.0008 / 0.0004 | 0.00833 / 0.0125 |
| wooded_badlands | 0.0638 | 0.0125 |
| badlands, eroded_badlands | 0 | 0.0125 |
| grove | 0 | 0.0025 |

  In native, a fallen tree also takes the place of a standing tree in that attempt; in VB it is added on top of the full tree count.
- fix_kind: data-override
- fix: (a) Turn VB's injection off by overriding the seven tags `data/minecraft/tags/worldgen/biome/spawns_fallen_{oak_trees,birch_trees,birch_trees_rarely,super_birch_trees,jungle_trees,spruce_trees,spruce_trees_rarely}.json` with `{"replace":true,"values":[]}`. (b) Override the 1.21.1 tree features so each holds the native entry list above, at the same index and with the same chance. Reference VB's placed `minecraft:fallen_oak_tree`, `fallen_birch_tree`, `fallen_super_birch_tree`, `fallen_jungle_tree` and `fallen_spruce_tree`; these placed features are 1:1 with native and carry the sapling `would_survive` filter.
  - Where 1.21.1 already has a `random_selector` configured feature (`trees_birch_and_oak`, `dark_forest_vegetation`, `trees_flower_forest`, `birch_tall`, `trees_jungle`, `trees_sparse_jungle`, `trees_taiga`, `trees_old_growth_pine_taiga`, `trees_old_growth_spruce_taiga`, `trees_plains`, `trees_savanna`, `trees_windswept_hills`), override `data/minecraft/worldgen/configured_feature/<name>.json`. Keep the 1.21.1 placed names; native `*_leaf_litter` names are only needed together with FLORA-08.
  - Where 1.21.1 `placed_feature/<name>.json` points straight at a tree (`trees_birch` → `birch_bees_0002`, `trees_snowy` → `spruce`, `trees_badlands` → `oak`), override the placed feature. Keep its 1.21.1 placement list and set `"feature": {"type":"minecraft:random_selector","config":{"features":[{"chance":0.0125,"feature":"minecraft:fallen_birch_tree"}],"default":"minecraft:birch_bees_0002"}}` (and the equivalent with `fallen_spruce_tree`/`spruce_checked` and `fallen_oak_tree`/`oak_checked`).
  - Example, `configured_feature/trees_birch_and_oak.json`: `{"type":"minecraft:random_selector","config":{"features":[{"chance":0.0025,"feature":"minecraft:fallen_birch_tree"},{"chance":0.2,"feature":"minecraft:birch_bees_0002"},{"chance":0.1,"feature":"minecraft:fancy_oak_bees_0002"},{"chance":0.0125,"feature":"minecraft:fallen_oak_tree"}],"default":"minecraft:oak_bees_0002"}}`.
  - Gate every override on `fallen_trees` with a load condition, or apply the edits in code through a `RegistryAccess` hook.
- config_gate: `fallen_trees`

### FLORA-08  Leaf litter: chunk-wide trunk scan instead of `place_on_ground` decorators on the leaf-litter tree variants
- severity: data
- vb: `placed_feature/leaf_litter_sparse.json` / `leaf_litter_thick.json` — `[surface_water_depth_filter 0, heightmap OCEAN_FLOOR, biome]` (once per chunk, at the chunk origin). The configs are `{"type":"minecraft:leaf_litter","config":{tries 96, radius 4, height 2, segments 1-3}}` and `{tries 150, radius 2, height 2, segments 1-4}`, injected into `spawns_leaf_litter` = forest, dark_forest, wooded_badlands, dappled_forest. `LeafLitterFeature.java:37-91`: for each of the 16×16 columns, `findTreeBase` (top `MOTION_BLOCKING_NO_LEAVES` block must be a vertical `#logs` stack ≥2 standing on `#dirt`, and the `MOTION_BLOCKING` top must be `#leaves` ∩ `#allows_leaf_litter` = oak, birch, dark_oak and the 3 poplars), then `placeLeafLitterAround`. `tryPlaceLeafLitter` (line 116) needs `isAir()`, a sturdy face below, the heightmap check, and `canSurvive`.
- native: the two decorators sit on the trees themselves: `configured_feature/oak_leaf_litter.json` `decorators: [{"type":"minecraft:place_on_ground","tries":96,"radius":4,"height":2, leaf_litter 1-3}, {"type":"minecraft:place_on_ground","tries":150,"height":2 /*radius default 2*/, leaf_litter 1-4}]`. The same pair, after `beehive 0.002` where present, is on `birch_leaf_litter`, `dark_oak_leaf_litter`, `fancy_oak_leaf_litter`, `oak_bees_0002_leaf_litter`, `birch_bees_0002_leaf_litter` and `fancy_oak_bees_0002_leaf_litter`. These trees are used only by `trees_birch_and_oak_leaf_litter` (forest), `dark_forest_vegetation` and `trees_badlands` (wooded_badlands); see the lists in FLORA-07. `PlaceOnGroundDecorator.attemptToPlaceBlockAbove` needs above = air **or vine**, below `isSolidRender`, and `MOTION_BLOCKING_NO_LEAVES ≤ above.y`.
- difference: Native drops litter once per generated tree, with the bounding box taken from all of the tree's lowest logs. VB repeats the full 96+150 tries for every trunk column it finds: a 2×2 dark oak gets 4× the litter, centred on each trunk column. VB also skips trees whose topmost column block is not an allowed leaf, trees not on `#dirt`, and spots occupied by vines, which native replaces. It also litters the trunks of trees that grew from saplings or came from other features, and handles the whole chunk based on the biome at the chunk corner. The VB tag set matches native's three biomes plus the VB-only dappled_forest.
- fix_kind: data-override
- fix: (a) Disable VB's pass by setting `placed_feature/leaf_litter_sparse.json` and `leaf_litter_thick.json` to `{"feature":{"type":"minecraft:no_op","config":{}},"placement":[]}`. Keep `patch_leaf_litter`, which matches native. (b) Add 1.21.1-format configured features `minecraft:oak_leaf_litter`, `birch_leaf_litter`, `dark_oak_leaf_litter`, `fancy_oak_leaf_litter`, `oak_bees_0002_leaf_litter`, `birch_bees_0002_leaf_litter` and `fancy_oak_bees_0002_leaf_litter`. Build each by copying the 1.21.1 `oak` / `birch` / `dark_oak` / `fancy_oak` / `*_bees_0002` tree JSON and appending to `decorators` the two objects `{"type":"minecraft:place_on_ground","tries":96,"radius":4,"height":2,"block_state_provider":{weighted_state_provider of minecraft:leaf_litter facing N/E/S/W × segment_amount 1..3, weight 1 each}}` and `{"type":"minecraft:place_on_ground","tries":150,"radius":2,"height":2,"block_state_provider":{same, segment_amount 1..4}}`. The type is VB's `minecraft:place_on_ground`, which matches native. Add placed features with the same names: `{"feature":"minecraft:<name>","placement":[{"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:would_survive","state":{"Name":"minecraft:<oak|birch|dark_oak>_sapling","Properties":{"stage":"0"}}}}]}`. (c) Point the forest, dark forest and wooded-badlands selectors at these, following the native lists in FLORA-07: `trees_birch_and_oak` (default `oak_bees_0002_leaf_litter`, entries `birch_bees_0002_leaf_litter 0.2`, `fancy_oak_bees_0002_leaf_litter 0.1`), `dark_forest_vegetation` (`dark_oak_leaf_litter 0.6666667`, `birch_leaf_litter 0.2`, `fancy_oak_leaf_litter 0.1`, default `oak_leaf_litter`), and `placed_feature/trees_badlands` (default `oak_leaf_litter`).
- config_gate: `leaf_litter` (plus `fallen_trees` for the fallen entries in the same files)

### FLORA-09  patch_bush makes 25 attempts per patch instead of 24
- severity: data
- vb: `data/minecraft/worldgen/configured_feature/patch_bush.json` — `{"type":"minecraft:random_patch","config":{"tries":25,"xz_spread":5,"y_spread":3, feature: simple_block bush + air filter}}`. Also `SpringToLifeFeatures.java` `new RandomPatchConfiguration(25, 5, 3, ...)`.
- native: `data/minecraft/worldgen/placed_feature/patch_bush.json` — `rarity_filter 4, in_square, heightmap MOTION_BLOCKING, biome, {"type":"minecraft:count","count":24}, random_offset trapezoid xz ±5 / y ±3 (plateau 0), block_predicate_filter #air`.
- difference: 25 vs 24 placement attempts per bush patch, about 4% more bushes. Everything else in the chain matches: rarity 4, heightmap, and the spread. 1.21.1 `nextInt(6)-nextInt(6)` has the same distribution as native trapezoid(-5..5, plateau 0).
- fix_kind: data-override
- fix: `data/minecraft/worldgen/configured_feature/patch_bush.json` → `{"type":"minecraft:random_patch","config":{"tries":24,"xz_spread":5,"y_spread":3,"feature":{"feature":{"type":"minecraft:simple_block","config":{"to_place":{"type":"minecraft:simple_state_provider","state":{"Name":"minecraft:bush"}}}},"placement":[{"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:matching_blocks","blocks":"minecraft:air"}}]}}}`
- config_gate: `bushes`

### FLORA-10  Bushes also generate in sunflower_plains
- severity: data
- vb: `data/minecraft/tags/worldgen/biome/spawns_bushes.json` — `["#minecraft:is_hill","#minecraft:is_river","minecraft:forest","minecraft:birch_forest","minecraft:old_growth_birch_forest",{"id":"#c:is_plains","required":false},{"id":"#c:plains","required":false}]`. NeoForge `c:is_plains` = plains + sunflower_plains (NeoForgeBiomeTagsProvider:188).
- native: `patch_bush` appears in exactly 9 biomes: birch_forest, forest, frozen_river, old_growth_birch_forest, plains, river, windswept_forest, windswept_gravelly_hills, windswept_hills. sunflower_plains.json does not have it.
- difference: VB adds bush patches to sunflower_plains; native has none there.
- fix_kind: data-override
- fix: `data/minecraft/tags/worldgen/biome/spawns_bushes.json` → `{"replace":true,"values":["#minecraft:is_hill","#minecraft:is_river","minecraft:forest","minecraft:birch_forest","minecraft:old_growth_birch_forest","minecraft:plains"]}`
- config_gate: `bushes`

### FLORA-11  Firefly bush (near water) biome set: extra in grove, missing in ice_spikes, snowy_plains, stony_shore
- severity: data
- vb: `data/minecraft/tags/worldgen/biome/spawns_firefly_bushes.json` — `#is_beach #is_river #is_taiga #is_forest #is_ocean #is_savanna #is_hill #is_jungle #is_badlands c:is_plains c:plains c:is_mushroom c:mushroom mangrove_swamp`. `#is_forest` includes grove and, via VB, pale_garden and dappled_forest. `#is_beach` = beach, snowy_beach.
- native: `patch_firefly_bush_near_water` is in 42 biomes, including ice_spikes, snowy_plains and stony_shore (`biome/ice_spikes.json` … `9:patch_firefly_bush_near_water`). It is **not** in grove (`biome/grove.json` vegetal list is only `trees_grove` + grass/flowers).
- difference: VB places firefly bushes beside water in grove, which native never does. It never places them in ice_spikes, snowy_plains or stony_shore, which native does with the same count 2 / near-water filter. The swamp variants are correct (swamp only).
- fix_kind: data-override
- fix: `data/minecraft/tags/worldgen/biome/spawns_firefly_bushes.json` → `{"replace":true,"values":["#minecraft:is_beach","#minecraft:is_river","#minecraft:is_taiga","#minecraft:is_ocean","#minecraft:is_savanna","#minecraft:is_hill","#minecraft:is_jungle","#minecraft:is_badlands","minecraft:forest","minecraft:flower_forest","minecraft:birch_forest","minecraft:old_growth_birch_forest","minecraft:dark_forest","minecraft:pale_garden","minecraft:dappled_forest","minecraft:plains","minecraft:sunflower_plains","minecraft:mushroom_fields","minecraft:mangrove_swamp","minecraft:ice_spikes","minecraft:snowy_plains","minecraft:stony_shore"]}`. dappled_forest is kept as VB's own choice.
- config_gate: `firefly_bushes`

### FLORA-12  Spring to Life vegetation is appended at the end of VEGETAL_DECORATION instead of at its native list position
- severity: data
- vb: `SpringToLifeFeatureManager` → `this.addVegetation(...)` (biome modifier `addFeature(VEGETAL_DECORATION, ...)`), which appends after every vanilla 1.21.1 vegetal feature. Example birch_forest order: `... forest_flowers, trees_birch, flower_default, patch_grass_forest, ..., patch_pumpkin, [VB] patch_bush, patch_firefly_bush_near_water, wildflowers_birch_forest, fallen trees`.
- native: e.g. `biome/birch_forest.json` step 9: `forest_flowers, wildflowers_birch_forest, trees_birch, patch_bush, flower_default, ..., patch_firefly_bush_near_water`. `biome/plains.json`: `patch_bush, trees_plains, flower_plains, ...`. Wildflowers and bushes come **before** trees and flowers, and trees can replace bushes (`#replaceable_by_trees`).
- difference: Native places wildflowers and bushes before trees and the default flowers. VB places them after everything, so trees and grass claim those spots first, and the per-feature random seeds (decoration index salt) differ for every vegetal feature after the insertion point. The block mix in each chunk therefore differs, even where the rates are equal.
- fix_kind: engine
- fix: NeoForge biome modifiers cannot insert at an index. Closest faithful fix: a `BiomeModifier` in our add-on, running in phase `AFTER_EVERYTHING`, that removes VB's holders from `VEGETAL_DECORATION` and re-inserts them into `builder.getGenerationSettings().getFeatures(VEGETAL_DECORATION)` (a mutable `List<Holder<PlacedFeature>>`) right after the 1.21.1 feature that precedes them in the 26.2 biome JSON (e.g. after `forest_flowers` for wildflowers, before `trees_*` for patch_bush). The 26.2 lists are in `src-mc/data/minecraft/worldgen/biome/*.json`, step index 9.
- config_gate: the individual toggles (`bushes`, `firefly_bushes`, `wildflowers`, `dry_grass`, `leaf_litter`)

### FLORA-13  Bone meal on grass never produces wildflowers (birch forests, meadow)
- severity: gameplay
- vb: VB has no `GrassBlock` change. 1.21.1 `GrassBlock.performBonemeal` (src-121) — `if (random.nextInt(8) == 0) { List<ConfiguredFeature<?,?>> list = biome.getGenerationSettings().getFlowerFeatures(); ... holder = ((RandomPatchConfiguration)list.get(0).config()).feature(); }`. `getFlowerFeatures` = every `Feature.FLOWER` in feature order. VB's `wildflowers_birch_forest` / `wildflowers_meadow` (`"type":"minecraft:flower"`) are appended last, so `get(0)` is `flower_default` (birch) or `flower_meadow` (meadow).
- native: `net/minecraft/world/level/block/GrassBlock.java` performBonemeal — `if (random.nextInt(8) == 0) { List<ConfiguredFeature<?,?>> features = biome.getGenerationSettings().getBoneMealFeatures(); if (!features.isEmpty()) { Util.getRandom(features, random).place(...) } }`. `getBoneMealFeatures` = every feature tagged `#can_spawn_from_bone_meal` = `flower_default, flower_flower_forest, flower_swamp, flower_plain, flower_meadow, flower_cherry, wildflower, flower_pale_garden`.
- difference: In native birch_forest, old_growth_birch_forest and meadow, each 1/8 flower attempt picks uniformly between the default/meadow flower and `wildflower`, so about half the bone-meal flowers are wildflowers. VB never yields wildflowers from bone meal. Native also draws one extra `random` call (`Util.getRandom`).
- fix_kind: mixin-mc
- fix: `@Mixin(net.minecraft.world.level.block.GrassBlock.class)`, `@Redirect(method="performBonemeal", at=@At(value="INVOKE", target="Ljava/util/List;get(I)Ljava/lang/Object;"))` → `return Util.getRandom(list, random)` (capture `random` with `@Local` or `@ModifyArgs`). The pool needs to match the native tag. 1.21.1's list contains only `FLOWER`-type features; in the flora biomes those are exactly `flower_default`/`flower_meadow`/… plus VB's two wildflower features, which equals the native pool. A uniform pick is therefore exact here.
- config_gate: `wildflowers`

### FLORA-14  Sheep do not eat short/tall dry grass
- severity: gameplay
- vb: VB has no change. 1.21.1 `net/minecraft/world/entity/ai/goal/EatBlockGoal.java:16` — `private static final Predicate<BlockState> IS_TALL_GRASS = BlockStatePredicate.forBlock(Blocks.SHORT_GRASS);`. VB ships no `edible_for_sheep` tag (it does not exist in 1.21.1).
- native: `net/minecraft/world/entity/ai/goal/EatBlockGoal.java:16` — `IS_EDIBLE = state -> state.is(BlockTags.EDIBLE_FOR_SHEEP);`. `data/minecraft/tags/block/edible_for_sheep.json` = `short_grass, short_dry_grass, tall_dry_grass, fern`.
- difference: Native sheep (`canUse` and `tick`) eat and break short dry grass, tall dry grass and fern to regrow wool. VB sheep only eat short grass, or grass blocks underneath them.
- fix_kind: mixin-mc
- fix: `@Mixin(EatBlockGoal.class)`. In both `canUse` and `tick`, use `@Redirect(at=@At(value="INVOKE", target="Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))` → `BlockState s=(BlockState)o; return s.is(Blocks.SHORT_GRASS)||s.is(Blocks.FERN)||s.is(ModBlocks.SHORT_DRY_GRASS.get())||s.is(ModBlocks.TALL_DRY_GRASS.get());`. Optionally define the tag `minecraft:edible_for_sheep` with those four values and test against it.
- config_gate: none

### FLORA-15  Bush, leaf litter and dry grass block bookshelf power to the enchanting table
- severity: gameplay
- vb: `ModBlocks.java:254-324` registers these blocks with `.replaceable()`, but VB has no `data/minecraft/tags/block/replaceable.json`. 1.21.1 `VanillaBlockTagsProvider:1766-1772` builds `#replaceable` at datagen from vanilla blocks with `canBeReplaced()`, and `ENCHANTMENT_POWER_TRANSMITTER = #replaceable`. 1.21.1 `EnchantingTableBlock.isValidBookShelf:49-52` requires the midpoint block to be `#enchantment_power_transmitter`.
- native: `data/minecraft/tags/block/replaceable.json` contains `minecraft:bush, minecraft:leaf_litter, minecraft:short_dry_grass, minecraft:tall_dry_grass`. `enchantment_power_transmitter.json` = `["#minecraft:replaceable"]`.
- difference: In native, these four blocks between a bookshelf and the table still transmit power, like short grass. In VB they block it, so an enchanting setup decorated with leaf litter loses levels.
- fix_kind: data-override
- fix: `data/minecraft/tags/block/replaceable.json` → `{"replace":false,"values":["minecraft:bush","minecraft:leaf_litter","minecraft:short_dry_grass","minecraft:tall_dry_grass"]}`
- config_gate: none

### FLORA-16  Short/tall dry grass cannot be placed on farmland
- severity: gameplay
- vb: `ShortDryGrassBlock` / `TallDryGrassBlock` extend 1.21.1 `DeadBushBlock` → `mayPlaceOn: state.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON)`. 1.21.1 tag (VanillaBlockTagsProvider:1720) = `#sand, #terracotta, #dirt`. VB ships no override.
- native: `DryVegetationBlock.mayPlaceOn` → `state.is(BlockTags.SUPPORTS_DRY_VEGETATION)` = `#sand, #terracotta, #supports_vegetation`, where `supports_vegetation` = `#substrate_overworld (= #dirt[3], #mud, #moss_blocks, #grass_blocks), minecraft:farmland`.
- difference: Apart from `farmland`, the two sets are the same blocks. Native lets short and tall dry grass (and dead bushes) stand on farmland; VB refuses placement there, and bone-meal spreading by tall dry grass skips farmland neighbours.
- fix_kind: data-override
- fix: `data/minecraft/tags/block/dead_bush_may_place_on.json` → `{"replace":false,"values":["minecraft:farmland"]}`. This also corrects the dead bush, which is a `DryVegetationBlock` on the same tag in native.
- config_gate: none

### FLORA-17  Mangrove leaves never drop falling-leaf particles
- severity: visual
- vb: `data/minecraft/tags/block/spawn_falling_leaves.json` — `[oak_leaves, birch_leaves, jungle_leaves, acacia_leaves, dark_oak_leaves, azalea_leaves, flowering_azalea_leaves]`. `FallingLeavesModule.java:31-35` spawns only for blocks in this tag or `spawn_falling_needles`.
- native: `Blocks.java` — `MANGROVE_LEAVES = register(..., p -> new MangroveLeavesBlock(0.01F, p), ...)`. `MangroveLeavesBlock extends TintedParticleLeavesBlock` → `ColorParticleOption.create(TINTED_LEAVES, level.getClientLeafTintColor(pos))`.
- difference: Native mangrove leaves spawn tinted falling leaves at 1% per animate tick (foliage-tinted); VB spawns none.
- fix_kind: data-override
- fix: `data/minecraft/tags/block/spawn_falling_leaves.json` → `{"replace":false,"values":["minecraft:mangrove_leaves"]}` (the mangrove foliage tint is already in 1.21.1 BlockColors).
- config_gate: client `falling_leaves` / `falling_leaves_frequency`

### FLORA-18  Spruce leaves use VB-only "needle" particle sprites
- severity: visual
- vb: `FallingLeavesModule.java:33-34` — `else if (state.is(ModBlockTags.SPAWN_FALLING_NEEDLES)) particle = ModParticles.TINTED_NEEDLES.get();` (tag = `[minecraft:spruce_leaves]`). `assets/minecraft/particles/tinted_needles.json` → `minecraft:needle_0 … needle_11`.
- native: `Blocks.java` — `SPRUCE_LEAVES = register(..., p -> new TintedParticleLeavesBlock(0.01F, p), ...)`. That uses `ParticleTypes.TINTED_LEAVES` (`particles/tinted_leaves.json` → `leaf_0 … leaf_11`). 26.2 has no needle particle type and no `needle_*` textures.
- difference: Native spruce drops the standard leaf sprites, tinted with the constant spruce colour -10380959. VB drops custom needle sprites. Physics and tint are identical.
- fix_kind: data-override
- fix: `data/minecraft/tags/block/spawn_falling_needles.json` → `{"replace":true,"values":[]}` and `data/minecraft/tags/block/spawn_falling_leaves.json` → `{"replace":false,"values":["minecraft:spruce_leaves","minecraft:mangrove_leaves"]}` (merged with FLORA-17). Alternatively, use an asset override of `assets/minecraft/particles/tinted_needles.json` with the `leaf_0..leaf_11` texture list.
- config_gate: client `falling_leaves`

### FLORA-19  Leaf litter colour is not biome-blended where a biome has a fixed dry-foliage colour
- severity: visual
- vb: `client/api/modules/falling_leaves/LeafColors.java:36-39` (getAverageDryFoliageColor) — `OptionalInt customColor = DryLeafColorReloadListener.getColorForBiome(biome); return customColor.isPresent() ? customColor.getAsInt() : new BlockTintCache(value -> level.calculateBlockTint(value, DRY_FOLIAGE_COLOR_RESOLVER)).getColor(pos);`. `DRY_FOLIAGE_COLOR_RESOLVER` uses only the climate colormap: `DryFoliageColor.get(clamp(temp), clamp(downfall))`.
- native: `client/renderer/BiomeColors.java:11,27` — `DRY_FOLIAGE_COLOR_RESOLVER = (biome, x, z) -> biome.getDryFoliageColor();` → `getAverageColor(level, pos, resolver)` (blend radius). `Biome.getDryFoliageColor()` = `dryFoliageColorOverride().orElseGet(colormap)`. dark_forest, swamp and mangrove_swamp use `#7b5334` and pale_garden uses `#a0a69c`.
- difference: Native blends leaf-litter colour smoothly across biome borders, with each biome contributing its override or colormap value. VB paints override biomes a flat colour (no blend), and the neighbouring blend reads the override biome's colormap colour instead of its override. The result is a hard colour seam at dark forest, swamp and pale garden borders. The override colours themselves are correct. A fresh `BlockTintCache` per call also means no caching (performance only).
- fix_kind: mixin-vb
- fix: `@Mixin(LeafColors.class)`, `@Inject(method="getAverageDryFoliageColor", at=@At("HEAD"), cancellable=true)`: `ClientLevel l=Minecraft.getInstance().level; if(l==null){cir.setReturnValue(-10732494);return;} cir.setReturnValue(l.getBlockTint(pos, OVERRIDE_AWARE));`. `OVERRIDE_AWARE` is a static `ColorResolver (biome,x,z) -> { Holder<Biome> h = l.registryAccess().registryOrThrow(Registries.BIOME).wrapAsHolder(biome); OptionalInt c = DryLeafColorReloadListener.getColorForBiome(h); return c.isPresent()? c.getAsInt() : DryFoliageColor.get(Mth.clamp(biome.getBaseTemperature(),0,1), Mth.clamp(downfall,0,1)); }`. Register it through NeoForge `RegisterColorHandlersEvent.ColorResolvers` so `ClientLevel.getBlockTint` caches it.
- config_gate: none

### FLORA-20  Firefly particle wander uses Math.random (double) instead of the particle's RandomSource
- severity: cosmetic
- vb: `client/level/particle/FireflyParticle.java:48-49` (tick) — `if (Math.random() > 0.95 || this.age == 1) { this.setParticleSpeed(-0.05 + 0.1F * Math.random(), -0.05 + 0.1F * Math.random(), -0.05 + 0.1F * Math.random()); }`
- native: `client/particle/FireflyParticle.java:54-55` — `if (this.random.nextFloat() > 0.95F || this.age == 1) { this.setParticleSpeed(-0.05F + 0.1F * this.random.nextFloat(), -0.05F + 0.1F * this.random.nextFloat(), -0.05F + 0.1F * this.random.nextFloat()); }`
- difference: Same 5% re-direction chance and same ±0.05 speed range. Only the RNG source (the global `Math.random` versus the particle's own `random`) and the precision (double versus float) differ. There is no observable distribution change; this is listed only for 1:1 completeness. Every other constant matches: lifetime 200–300, friction 0.96, quadSize ×0.75 ×1.5, fade 0.1/0.3 light and 0.3/0.5 alpha, translucent layer.
- fix_kind: mixin-vb
- fix: `@Mixin(com.blackgear.vanillabackport.client.level.particle.FireflyParticle.class)`, `@Redirect(method="tick", at=@At(value="INVOKE", target="Ljava/lang/Math;random()D"))` → `return ((ParticleAccessor)this).getRandom().nextFloat();`
- config_gate: none
